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
import com.raylabs.laundryhub.core.domain.model.sheets.SyncFieldDifference
import com.raylabs.laundryhub.core.domain.model.sheets.SyncPreviewResponse
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRowChangeType
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRowDifference
import com.raylabs.laundryhub.backend.util.syncVerificationSignature
import com.raylabs.laundryhub.backend.util.normalizedSyncNumberText
import com.raylabs.laundryhub.backend.util.normalizedSyncPhoneText
import com.raylabs.laundryhub.backend.util.normalizedSyncStatusText
import com.raylabs.laundryhub.backend.util.normalizedSyncText
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
                fieldDifferenceSelector = ::orderFieldDifferences,
                suspiciousKeySelector = ::isOrderHeaderKey
            ),
            buildEntityPreview(
                entity = "Outcomes",
                sheetRows = syncService.fetchOutcomesFromSheet(spreadsheetId),
                databaseRows = outcomeRepository.getAll(page = 1, size = SYNC_READ_LIMIT),
                pendingDeletes = pendingDeletesByType[SyncEntityType.OUTCOME] ?: 0,
                keySelector = OutcomeData::id,
                signatureSelector = OutcomeData::syncVerificationSignature,
                fieldDifferenceSelector = ::outcomeFieldDifferences,
                suspiciousKeySelector = { false }
            ),
            buildEntityPreview(
                entity = "Packages",
                sheetRows = syncService.fetchPackagesFromSheet(spreadsheetId),
                databaseRows = packageRepository.getAll(),
                pendingDeletes = pendingDeletesByType[SyncEntityType.PACKAGE] ?: 0,
                keySelector = PackageData::name,
                signatureSelector = PackageData::syncVerificationSignature,
                fieldDifferenceSelector = ::packageFieldDifferences,
                suspiciousKeySelector = { false }
            ),
            buildEntityPreview(
                entity = "Gross",
                sheetRows = syncService.fetchGrossFromSheet(spreadsheetId),
                databaseRows = grossRepository.getAll(page = 1, size = SYNC_READ_LIMIT),
                pendingDeletes = pendingDeletesByType[SyncEntityType.GROSS] ?: 0,
                keySelector = GrossData::month,
                signatureSelector = GrossData::syncVerificationSignature,
                fieldDifferenceSelector = ::grossFieldDifferences,
                suspiciousKeySelector = { false }
            ),
            buildEntityPreview(
                entity = "Summary",
                sheetRows = syncService.fetchSummaryFromSheet(spreadsheetId),
                databaseRows = summaryRepository.getAll(),
                pendingDeletes = pendingDeletesByType[SyncEntityType.SUMMARY] ?: 0,
                keySelector = SpreadsheetData::key,
                signatureSelector = SpreadsheetData::syncVerificationSignature,
                fieldDifferenceSelector = ::summaryFieldDifferences,
                suspiciousKeySelector = { false }
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
    fieldDifferenceSelector = { _, _ -> emptyList() },
    suspiciousKeySelector = { false }
)

internal fun <T> buildEntityPreview(
    entity: String,
    sheetRows: List<T>,
    databaseRows: List<T>,
    pendingDeletes: Int,
    keySelector: (T) -> String,
    signatureSelector: (T) -> String,
    fieldDifferenceSelector: (sheetRow: T, databaseRow: T) -> List<SyncFieldDifference> = { _, _ -> emptyList() },
    suspiciousKeySelector: (String) -> Boolean
): SyncEntityPreview {
    val sheetKeys = sheetRows.map { keySelector(it).trim() }.filter { it.isNotBlank() }
    val databaseKeys = databaseRows.map { keySelector(it).trim() }.filter { it.isNotBlank() }
    val sheetRowsByKey = sheetRows.firstRowByKey(keySelector)
    val databaseRowsByKey = databaseRows.firstRowByKey(keySelector)
    val commonKeys = sheetRowsByKey.keys.intersect(databaseRowsByKey.keys)
    val onlyInSheetKeys = (sheetRowsByKey.keys - databaseRowsByKey.keys).sortedWith(syncKeyComparator())
    val onlyInDatabaseKeys = (databaseRowsByKey.keys - sheetRowsByKey.keys).sortedWith(syncKeyComparator())
    val duplicateKeyValues = (sheetKeys.duplicateValues() + databaseKeys.duplicateValues())
        .distinct()
        .sortedWith(syncKeyComparator())
    val suspiciousKeyValues = (databaseKeys.filter(suspiciousKeySelector) + sheetKeys.filter(suspiciousKeySelector))
        .distinct()
        .sortedWith(syncKeyComparator())

    val changedRowKeys = commonKeys.filter { key ->
        signatureSelector(sheetRowsByKey.getValue(key)) != signatureSelector(databaseRowsByKey.getValue(key))
    }.sortedWith(syncKeyComparator())
    val changedRowDifferences = changedRowKeys.map { key ->
        SyncRowDifference(
            key = key,
            changeType = SyncRowChangeType.CHANGED,
            fieldDifferences = fieldDifferenceSelector(
                sheetRowsByKey.getValue(key),
                databaseRowsByKey.getValue(key)
            )
        )
    }

    return SyncEntityPreview(
        entity = entity,
        onlyInSheets = onlyInSheetKeys.size,
        onlyInDatabase = onlyInDatabaseKeys.size,
        changedRows = changedRowKeys.size,
        duplicateKeys = sheetKeys.duplicateExtraCount() + databaseKeys.duplicateExtraCount(),
        pendingDeletes = pendingDeletes,
        suspiciousRows = databaseKeys.count(suspiciousKeySelector) + sheetKeys.count(suspiciousKeySelector),
        onlyInSheetKeys = onlyInSheetKeys,
        onlyInDatabaseKeys = onlyInDatabaseKeys,
        changedRowKeys = changedRowKeys,
        duplicateKeyValues = duplicateKeyValues,
        suspiciousKeyValues = suspiciousKeyValues,
        rowDifferences = onlyInSheetKeys.map { key ->
            SyncRowDifference(key = key, changeType = SyncRowChangeType.ONLY_IN_SHEETS)
        } + onlyInDatabaseKeys.map { key ->
            SyncRowDifference(key = key, changeType = SyncRowChangeType.ONLY_IN_DATABASE)
        } + changedRowDifferences
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

private fun List<String>.duplicateValues(): List<String> {
    return groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys
        .toList()
}

private fun syncKeyComparator(): Comparator<String> {
    return compareBy<String> { it.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it }
}

private const val SYNC_READ_LIMIT = 100_000

internal fun isOrderHeaderKey(key: String): Boolean {
    return key.trim().lowercase().filter { it.isLetterOrDigit() } == "orderid"
}

internal fun orderFieldDifferences(sheetRow: OrderData, databaseRow: OrderData): List<SyncFieldDifference> = listOfNotNull(
    fieldDifference("orderId", sheetRow.orderId, databaseRow.orderId),
    fieldDifference("orderDate", sheetRow.orderDate, databaseRow.orderDate, String::normalizedSyncText),
    fieldDifference("name", sheetRow.name, databaseRow.name, String::normalizedSyncText),
    fieldDifference("weight", sheetRow.weight, databaseRow.weight, String::normalizedSyncNumberText),
    fieldDifference("priceKg", sheetRow.priceKg, databaseRow.priceKg, String::normalizedSyncNumberText),
    fieldDifference("totalPrice", sheetRow.totalPrice, databaseRow.totalPrice, String::normalizedSyncNumberText),
    fieldDifference("paidStatus", sheetRow.paidStatus, databaseRow.paidStatus, String::normalizedSyncStatusText),
    fieldDifference("packageName", sheetRow.packageName, databaseRow.packageName, String::normalizedSyncText),
    fieldDifference("remark", sheetRow.remark, databaseRow.remark, String::normalizedSyncText),
    fieldDifference("paymentMethod", sheetRow.paymentMethod, databaseRow.paymentMethod, String::normalizedSyncText),
    fieldDifference("phoneNumber", sheetRow.phoneNumber, databaseRow.phoneNumber, String::normalizedSyncPhoneText),
    fieldDifference("dueDate", sheetRow.dueDate, databaseRow.dueDate, String::normalizedSyncText)
)

private fun outcomeFieldDifferences(sheetRow: OutcomeData, databaseRow: OutcomeData): List<SyncFieldDifference> = listOfNotNull(
    fieldDifference("id", sheetRow.id, databaseRow.id),
    fieldDifference("date", sheetRow.date, databaseRow.date, String::normalizedSyncText),
    fieldDifference("purpose", sheetRow.purpose, databaseRow.purpose, String::normalizedSyncText),
    fieldDifference("price", sheetRow.price, databaseRow.price, String::normalizedSyncNumberText),
    fieldDifference("remark", sheetRow.remark, databaseRow.remark, String::normalizedSyncText),
    fieldDifference("payment", sheetRow.payment, databaseRow.payment, String::normalizedSyncText)
)

private fun packageFieldDifferences(sheetRow: PackageData, databaseRow: PackageData): List<SyncFieldDifference> = listOfNotNull(
    fieldDifference("name", sheetRow.name, databaseRow.name, String::normalizedSyncText),
    fieldDifference("price", sheetRow.price, databaseRow.price, String::normalizedSyncNumberText),
    fieldDifference("duration", sheetRow.duration, databaseRow.duration, String::normalizedSyncNumberText),
    fieldDifference("unit", sheetRow.unit, databaseRow.unit, String::normalizedSyncText)
)

private fun grossFieldDifferences(sheetRow: GrossData, databaseRow: GrossData): List<SyncFieldDifference> = listOfNotNull(
    fieldDifference("month", sheetRow.month, databaseRow.month, String::normalizedSyncText),
    fieldDifference("totalNominal", sheetRow.totalNominal, databaseRow.totalNominal, String::normalizedSyncNumberText),
    fieldDifference("orderCount", sheetRow.orderCount, databaseRow.orderCount, String::normalizedSyncNumberText),
    fieldDifference("tax", sheetRow.tax, databaseRow.tax, String::normalizedSyncNumberText)
)

private fun summaryFieldDifferences(
    sheetRow: SpreadsheetData,
    databaseRow: SpreadsheetData
): List<SyncFieldDifference> = listOfNotNull(
    fieldDifference("key", sheetRow.key, databaseRow.key, String::normalizedSyncText),
    fieldDifference("value", sheetRow.value, databaseRow.value, String::normalizedSyncNumberText)
)

private fun fieldDifference(
    fieldName: String,
    sheetValue: String,
    databaseValue: String,
    normalize: (String) -> String = { it.trim() }
): SyncFieldDifference? {
    return if (normalize(sheetValue) == normalize(databaseValue)) {
        null
    } else {
        SyncFieldDifference(
            fieldName = fieldName,
            sheetValue = sheetValue,
            databaseValue = databaseValue
        )
    }
}
