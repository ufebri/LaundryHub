package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class SyncDriftAuditJob(
    private val previewService: SyncPreviewService,
    private val syncStateManager: SyncStateManager,
    private val intervalMinutes: Int = DEFAULT_INTERVAL_MINUTES,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val logger = LoggerFactory.getLogger(SyncDriftAuditJob::class.java)

    fun start() {
        scope.launch {
            logger.info("Sync drift audit job started with ${intervalMinutes}m interval.")
            delay(INITIAL_DELAY_MILLIS)
            while (isActive) {
                auditOnce()
                delay(intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES) * 60 * 1000L)
            }
        }
    }

    suspend fun auditOnce() {
        if (syncStateManager.isSyncing) {
            logger.info("Sync drift audit skipped because another sync is already running.")
            return
        }

        runCatching {
            previewService.createPreview(MasterSourceOfTruth.SUPABASE)
        }.onSuccess { preview ->
            if (preview.totalDifferences == 0) {
                logger.info("Sync drift audit clean: app-owned Sheets mirror matches Supabase.")
            } else {
                logger.warn(
                    "Sync drift audit found ${preview.totalDifferences} app-owned differences: ${preview.auditSummary()}"
                )
            }
        }.onFailure { error ->
            logger.error("Sync drift audit failed: ${error.message}")
        }
    }

    private fun com.raylabs.laundryhub.core.domain.model.sheets.SyncPreviewResponse.auditSummary(): String {
        return entities
            .filterNot { it.entity == "Gross" || it.entity == "Summary" }
            .filter { it.totalDifferences > 0 }
            .joinToString("; ") { entity ->
                val keys = listOfNotNull(
                    entity.onlyInDatabaseKeys.takeIf { it.isNotEmpty() }?.let { "dbOnly=${it.auditKeys()}" },
                    entity.onlyInSheetKeys.takeIf { it.isNotEmpty() }?.let { "sheetOnly=${it.auditKeys()}" },
                    entity.changedRowKeys.takeIf { it.isNotEmpty() }?.let { "changed=${it.auditKeys()}" }
                ).joinToString(", ")
                "${entity.entity}[$keys]"
            }
    }

    private fun List<String>.auditKeys(): String {
        val visibleKeys = take(MAX_KEYS_IN_LOG).joinToString(",")
        val remaining = size - MAX_KEYS_IN_LOG
        return if (remaining > 0) "$visibleKeys,+$remaining" else visibleKeys
    }

    companion object {
        const val DEFAULT_INTERVAL_MINUTES = 360
        private const val MIN_INTERVAL_MINUTES = 15
        private const val INITIAL_DELAY_MILLIS = 30_000L
        private const val MAX_KEYS_IN_LOG = 8
    }
}
