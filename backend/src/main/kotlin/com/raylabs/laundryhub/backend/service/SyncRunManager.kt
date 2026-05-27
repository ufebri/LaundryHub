package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import com.raylabs.laundryhub.core.domain.model.sheets.SyncPreviewResponse
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStage
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStatus
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStatusResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SyncRunManager(
    private val previewService: SyncPreviewService,
    private val batchSyncJob: SheetsBatchSyncJob,
    private val reverseSyncJob: SheetsReverseSyncJob,
    private val syncStateManager: SyncStateManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val logger = LoggerFactory.getLogger(SyncRunManager::class.java)
    private val previews = ConcurrentHashMap<String, SyncPreviewResponse>()
    private val runs = ConcurrentHashMap<String, MutableSyncRun>()

    suspend fun createPreview(sourceOfTruth: MasterSourceOfTruth): SyncPreviewResponse {
        val preview = previewService.createPreview(sourceOfTruth)
        previews[preview.previewId] = preview
        return preview
    }

    fun startRun(previewId: String, requestedSource: MasterSourceOfTruth?): SyncRunStatusResponse {
        val preview = previews[previewId] ?: error("Preview expired. Check differences again.")
        val sourceOfTruth = requestedSource ?: preview.sourceOfTruth
        if (preview.hasBlockingConflicts) {
            error("Resolve duplicate keys or two-way conflicts before syncing.")
        }
        if (sourceOfTruth == MasterSourceOfTruth.BOTH) {
            error("Two-way sync is disabled until conflict resolution is available.")
        }
        if (syncStateManager.isSyncing) {
            error("Another sync is already running.")
        }

        val run = MutableSyncRun(
            runId = "sync-${UUID.randomUUID()}",
            previewId = preview.previewId,
            totalItems = preview.totalDifferences.coerceAtLeast(1)
        )
        runs[run.runId] = run

        scope.launch {
            executeRun(run, sourceOfTruth)
        }

        return run.toResponse()
    }

    fun getRun(runId: String): SyncRunStatusResponse? = runs[runId]?.toResponse()

    private suspend fun executeRun(run: MutableSyncRun, sourceOfTruth: MasterSourceOfTruth) {
        syncStateManager.setSyncing(true)
        run.update(
            status = SyncRunStatus.RUNNING,
            stage = SyncRunStage.PLANNING,
            message = "Preparing sync plan"
        )

        try {
            val appliedCount = when (sourceOfTruth) {
                MasterSourceOfTruth.SHEETS -> applySheetsToDatabase(run)
                MasterSourceOfTruth.SUPABASE -> applyDatabaseToSheets(run)
                MasterSourceOfTruth.BOTH -> error("Two-way sync is disabled until conflict resolution is available.")
            }

            run.update(
                stage = SyncRunStage.VERIFYING,
                message = "Verifying sync result",
                processedItems = run.processedItems.coerceAtLeast(appliedCount)
            )
            val finalPreview = previewService.createPreview(sourceOfTruth)
            val status = if (finalPreview.totalDifferences == 0) {
                SyncRunStatus.SUCCEEDED
            } else {
                SyncRunStatus.PARTIAL
            }

            run.update(
                status = status,
                stage = SyncRunStage.COMPLETED,
                message = if (status == SyncRunStatus.SUCCEEDED) {
                    "Sync completed"
                } else {
                    "Sync completed with remaining differences"
                },
                processedItems = run.totalItems,
                finalDifferenceCount = finalPreview.totalDifferences
            )
            syncStateManager.recordSync(appliedCount, status.name)
        } catch (e: Exception) {
            logger.error("Manual sync run ${run.runId} failed: ${e.message}")
            run.update(
                status = SyncRunStatus.FAILED,
                message = "Sync failed",
                lastError = e.message
            )
            syncStateManager.recordSyncFailure(e.message)
        } finally {
            syncStateManager.setSyncing(false)
        }
    }

    private suspend fun applySheetsToDatabase(run: MutableSyncRun): Int {
        var processed = 0
        processed += runStage(run, SyncRunStage.APPLYING_ORDERS, "Applying orders from Google Sheets", "Orders") {
            reverseSyncJob.pullOrdersFromSheets()
        }
        processed += runStage(run, SyncRunStage.APPLYING_OUTCOMES, "Applying outcomes from Google Sheets", "Outcomes") {
            reverseSyncJob.pullOutcomesFromSheets()
        }
        processed += runStage(run, SyncRunStage.APPLYING_PACKAGES, "Applying packages from Google Sheets", "Packages") {
            reverseSyncJob.pullPackagesFromSheets()
        }
        processed += runStage(run, SyncRunStage.APPLYING_GROSS, "Applying gross rows from Google Sheets", "Gross") {
            reverseSyncJob.pullGrossFromSheets()
        }
        processed += runStage(run, SyncRunStage.APPLYING_SUMMARY, "Applying summary rows from Google Sheets", "Summary") {
            reverseSyncJob.pullSummaryFromSheets()
        }
        return processed
    }

    private suspend fun applyDatabaseToSheets(run: MutableSyncRun): Int {
        var processed = 0
        processed += runStage(run, SyncRunStage.APPLYING_ORDERS, "Applying orders to Google Sheets", "Orders") {
            batchSyncJob.processUnsyncedOrders()
        }
        processed += runStage(run, SyncRunStage.APPLYING_OUTCOMES, "Applying outcomes to Google Sheets", "Outcomes") {
            batchSyncJob.processUnsyncedOutcomes()
        }
        processed += runStage(run, SyncRunStage.APPLYING_PACKAGES, "Applying packages to Google Sheets", "Packages") {
            batchSyncJob.processUnsyncedPackages()
        }
        processed += runStage(run, SyncRunStage.APPLYING_GROSS, "Applying gross rows to Google Sheets", "Gross") {
            batchSyncJob.processUnsyncedGross()
        }
        processed += runStage(run, SyncRunStage.APPLYING_SUMMARY, "Applying summary rows to Google Sheets", "Summary") {
            batchSyncJob.processUnsyncedSummaries()
        }
        processed += runStage(run, SyncRunStage.CLEANING_DELETES, "Cleaning deleted rows from Google Sheets", "Deletes") {
            batchSyncJob.processPendingDeletes()
        }
        return processed
    }

    private suspend fun runStage(
        run: MutableSyncRun,
        stage: SyncRunStage,
        message: String,
        entity: String,
        block: suspend () -> Int
    ): Int {
        run.update(
            stage = stage,
            message = message,
            currentEntity = entity
        )
        val count = block()
        run.update(
            processedItems = (run.processedItems + count).coerceAtMost(run.totalItems)
        )
        return count
    }
}

private class MutableSyncRun(
    val runId: String,
    val previewId: String,
    val totalItems: Int
) {
    @Volatile
    var status: SyncRunStatus = SyncRunStatus.PENDING
        private set

    @Volatile
    var stage: SyncRunStage = SyncRunStage.IDLE
        private set

    @Volatile
    var message: String = "Queued"
        private set

    @Volatile
    var processedItems: Int = 0
        private set

    @Volatile
    var currentEntity: String? = null
        private set

    @Volatile
    var lastError: String? = null
        private set

    @Volatile
    var finalDifferenceCount: Int? = null
        private set

    fun update(
        status: SyncRunStatus = this.status,
        stage: SyncRunStage = this.stage,
        message: String = this.message,
        processedItems: Int = this.processedItems,
        currentEntity: String? = this.currentEntity,
        lastError: String? = this.lastError,
        finalDifferenceCount: Int? = this.finalDifferenceCount
    ) {
        this.status = status
        this.stage = stage
        this.message = message
        this.processedItems = processedItems
        this.currentEntity = currentEntity
        this.lastError = lastError
        this.finalDifferenceCount = finalDifferenceCount
    }

    fun toResponse(): SyncRunStatusResponse {
        return SyncRunStatusResponse(
            runId = runId,
            previewId = previewId,
            status = status,
            stage = stage,
            message = message,
            processedItems = processedItems,
            totalItems = totalItems,
            currentEntity = currentEntity,
            lastError = lastError,
            finalDifferenceCount = finalDifferenceCount
        )
    }
}
