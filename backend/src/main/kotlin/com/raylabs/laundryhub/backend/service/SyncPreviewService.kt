package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.backend.db.repository.GrossRepository
import com.raylabs.laundryhub.backend.db.repository.OrderRepository
import com.raylabs.laundryhub.backend.db.repository.OutcomeRepository
import com.raylabs.laundryhub.backend.db.repository.PackageRepository
import com.raylabs.laundryhub.backend.db.repository.SummaryRepository
import com.raylabs.laundryhub.backend.db.repository.SyncDeleteEventRepository
import com.raylabs.laundryhub.backend.db.repository.SyncEntityType
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.SyncEntityPreview
import com.raylabs.laundryhub.core.domain.model.sheets.SyncPreviewResponse
import com.raylabs.laundryhub.backend.util.syncVerificationSignature
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class SyncPreviewService(
    private val orderRepository: OrderRepository,
    private val outcomeRepository: OutcomeRepository,
    private val packageRepository: PackageRepository,
    private val grossRepository: GrossRepository,
    private val summaryRepository: SummaryRepository,
    private val syncDeleteEventRepository: SyncDeleteEventRepository,
    private val syncService: SheetsSyncService,
    private val spreadsheetId: String
) {

    suspend fun createPreview(sourceOfTruth: MasterSourceOfTruth): SyncPreviewResponse {
        val pendingDeletesByType = syncDeleteEventRepository
            .getPending()
            .groupingBy { it.entityType }
            .eachCount()

        val entities = listOf(
            buildEntityPreview(
                entity = "Orders",
                sheetRows = syncService.fetchOrdersFromSheet(spreadsheetId),
                databaseRows = orderRepository.getAll(page = 1, size = SYNC_READ_LIMIT),
                pendingDeletes = pendingDeletesByType[SyncEntityType.ORDER] ?: 0,
                keySelector = OrderData::orderId,
                signatureSelector = OrderData::syncVerificationSignature,
                suspiciousKeySelector = ::isOrderHeaderKey
            ),
            buildEntityPreview(
                entity = "Outcomes",
                sheetRows = syncService.fetchOutcomesFromSheet(spreadsheetId),
                databaseRows = outcomeRepository.getAll(page = 1, size = SYNC_READ_LIMIT),
                pendingDeletes = pendingDeletesByType[SyncEntityType.OUTCOME] ?: 0,
                keySelector = OutcomeData::id,
                signatureSelector = OutcomeData::syncVerificationSignature
            ),
            buildEntityPreview(
                entity = "Packages",
                sheetRows = syncService.fetchPackagesFromSheet(spreadsheetId),
                databaseRows = packageRepository.getAll(),
                pendingDeletes = pendingDeletesByType[SyncEntityType.PACKAGE] ?: 0,
                keySelector = PackageData::name,
                signatureSelector = PackageData::syncVerificationSignature
            ),
            buildEntityPreview(
                entity = "Gross",
                sheetRows = syncService.fetchGrossFromSheet(spreadsheetId),
                databaseRows = grossRepository.getAll(page = 1, size = SYNC_READ_LIMIT),
                pendingDeletes = pendingDeletesByType[SyncEntityType.GROSS] ?: 0,
                keySelector = GrossData::month,
                signatureSelector = GrossData::syncVerificationSignature
            ),
            buildEntityPreview(
                entity = "Summary",
                sheetRows = syncService.fetchSummaryFromSheet(spreadsheetId),
                databaseRows = summaryRepository.getAll(),
                pendingDeletes = pendingDeletesByType[SyncEntityType.SUMMARY] ?: 0,
                keySelector = SpreadsheetData::key,
                signatureSelector = SpreadsheetData::syncVerificationSignature
            )
        )

        val totalDifferences = if (sourceOfTruth == MasterSourceOfTruth.SUPABASE) {
            entities.filter { it.entity != "Gross" && it.entity != "Summary" }.sumOf { it.totalDifferences }
        } else {
            entities.sumOf { it.totalDifferences }
        }
        val hasDuplicateKeys = entities.any { it.duplicateKeys > 0 }
        val hasTwoWayConflicts = sourceOfTruth == MasterSourceOfTruth.BOTH && entities.any { it.changedRows > 0 }

        return SyncPreviewResponse(
            previewId = "preview-${UUID.randomUUID()}",
            sourceOfTruth = sourceOfTruth,
            generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            entities = entities,
            totalDifferences = totalDifferences,
            hasBlockingConflicts = hasDuplicateKeys || hasTwoWayConflicts,
            recommendedAction = sourceOfTruth.recommendedAction()
        )
    }

    private fun MasterSourceOfTruth.recommendedAction(): String {
        return when (this) {
            MasterSourceOfTruth.SHEETS -> "Use Google Sheets for this sync"
            MasterSourceOfTruth.SUPABASE -> "Use App Database for this sync"
            MasterSourceOfTruth.BOTH -> "Review conflicts before enabling two-way sync"
        }
    }
}

internal fun <T> buildEntityPreview(
    entity: String,
    sheetRows: List<T>,
    databaseRows: List<T>,
    pendingDeletes: Int,
    keySelector: (T) -> String,
    signatureSelector: (T) -> String
): SyncEntityPreview = buildEntityPreview(
    entity = entity,
    sheetRows = sheetRows,
    databaseRows = databaseRows,
    pendingDeletes = pendingDeletes,
    keySelector = keySelector,
    signatureSelector = signatureSelector,
    suspiciousKeySelector = { false }
)

internal fun <T> buildEntityPreview(
    entity: String,
    sheetRows: List<T>,
    databaseRows: List<T>,
    pendingDeletes: Int,
    keySelector: (T) -> String,
    signatureSelector: (T) -> String,
    suspiciousKeySelector: (String) -> Boolean
): SyncEntityPreview {
    val sheetKeys = sheetRows.map { keySelector(it).trim() }.filter { it.isNotBlank() }
    val databaseKeys = databaseRows.map { keySelector(it).trim() }.filter { it.isNotBlank() }
    val sheetRowsByKey = sheetRows.firstRowByKey(keySelector)
    val databaseRowsByKey = databaseRows.firstRowByKey(keySelector)
    val commonKeys = sheetRowsByKey.keys.intersect(databaseRowsByKey.keys)

    val changedRows = commonKeys.count { key ->
        signatureSelector(sheetRowsByKey.getValue(key)) != signatureSelector(databaseRowsByKey.getValue(key))
    }

    return SyncEntityPreview(
        entity = entity,
        onlyInSheets = (sheetRowsByKey.keys - databaseRowsByKey.keys).size,
        onlyInDatabase = (databaseRowsByKey.keys - sheetRowsByKey.keys).size,
        changedRows = changedRows,
        duplicateKeys = sheetKeys.duplicateExtraCount() + databaseKeys.duplicateExtraCount(),
        pendingDeletes = pendingDeletes,
        suspiciousRows = databaseKeys.count(suspiciousKeySelector) + sheetKeys.count(suspiciousKeySelector)
    )
}

private fun <T> List<T>.firstRowByKey(keySelector: (T) -> String): Map<String, T> {
    return mapNotNull { row ->
        keySelector(row).trim().takeIf { it.isNotBlank() }?.let { it to row }
    }.distinctBy { it.first }.toMap()
}

private fun List<String>.duplicateExtraCount(): Int {
    return groupingBy { it }
        .eachCount()
        .values
        .sumOf { count -> (count - 1).coerceAtLeast(0) }
}

private const val SYNC_READ_LIMIT = 100_000

internal fun isOrderHeaderKey(key: String): Boolean {
    return key.trim().lowercase().filter { it.isLetterOrDigit() } == "orderid"
}
