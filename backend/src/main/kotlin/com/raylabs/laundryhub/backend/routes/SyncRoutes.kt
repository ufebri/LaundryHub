package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.service.SheetsBatchSyncJob
import com.raylabs.laundryhub.backend.service.SheetsPushScheduler
import com.raylabs.laundryhub.backend.service.SyncRunManager
import com.raylabs.laundryhub.backend.service.SyncStateManager
import com.raylabs.laundryhub.core.domain.model.sheets.SyncConfigUpdateRequest
import com.raylabs.laundryhub.core.domain.model.sheets.SyncPreviewRequest
import com.raylabs.laundryhub.core.domain.model.sheets.SyncQueueState
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunRequest
import com.raylabs.laundryhub.core.domain.model.sheets.SyncStatusResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.syncRoutes(
    syncStateManager: SyncStateManager,
    batchSyncJob: SheetsBatchSyncJob?,
    sheetsPushScheduler: SheetsPushScheduler?,
    syncRunManager: SyncRunManager?
) {
    route("/api/sync") {
        get("/status") {
            handleGetSyncStatus(call, syncStateManager, batchSyncJob, sheetsPushScheduler, syncRunManager)
        }

        post("/preview") {
            handlePostSyncPreview(call, syncStateManager, syncRunManager)
        }

        post("/runs") {
            handlePostSyncRuns(call, syncRunManager)
        }

        get("/runs/{runId}") {
            handleGetSyncRunDetail(call, syncRunManager)
        }

        put("/config") {
            handlePutSyncConfig(call, syncStateManager)
        }

        post("/trigger") {
            handlePostSyncTrigger(call)
        }
    }
}

private suspend fun handleGetSyncStatus(
    call: ApplicationCall,
    syncStateManager: SyncStateManager,
    batchSyncJob: SheetsBatchSyncJob?,
    sheetsPushScheduler: SheetsPushScheduler?,
    syncRunManager: SyncRunManager?
) {
    val config = syncStateManager.config.value
    val pendingPushCount = batchSyncJob?.pendingPushCount() ?: 0
    val pendingDeleteCount = batchSyncJob?.pendingDeleteCount() ?: 0
    val differenceCounts = syncRunManager
        ?.currentDifferenceCounts(config.masterSourceOfTruth)
    val dataDifferenceCount = differenceCounts?.appOwned ?: 0
    val reportingDifferenceCount = differenceCounts?.reporting ?: 0
    val hasPendingPush = pendingPushCount + pendingDeleteCount > 0
    val hasDataDifferences = dataDifferenceCount > 0
    val hasReportingDifferences = reportingDifferenceCount > 0
    call.respond(
        HttpStatusCode.OK,
        SyncStatusResponse(
            lastSyncTime = syncStateManager.lastSyncTime,
            changesCount = syncStateManager.lastChangesCount,
            autoSyncIntervalMinutes = config.intervalMinutes,
            reverseSyncSchedule = config.reverseSyncSchedule,
            masterSourceOfTruth = config.masterSourceOfTruth,
            isSyncing = syncStateManager.isSyncing,
            lastSyncStatus = syncStateManager.lastSyncStatus,
            lastSyncError = syncStateManager.lastSyncError,
            pendingPushCount = pendingPushCount,
            pendingDeleteCount = pendingDeleteCount,
            nextScheduledPushTime = sheetsPushScheduler?.nextScheduledPushTime,
            dataDifferenceCount = dataDifferenceCount,
            hasDataDifferences = hasDataDifferences,
            reportingDifferenceCount = reportingDifferenceCount,
            hasReportingDifferences = hasReportingDifferences,
            syncQueueState = when {
                syncRunManager == null -> SyncQueueState.UNAVAILABLE
                hasPendingPush && hasDataDifferences -> SyncQueueState.PENDING_PUSH_AND_DATA_DIFFERENCES
                hasPendingPush -> SyncQueueState.PENDING_PUSH
                hasDataDifferences -> SyncQueueState.DATA_DIFFERENCES
                else -> SyncQueueState.IDLE
            }
        )
    )
}

private suspend fun handlePostSyncPreview(
    call: ApplicationCall,
    syncStateManager: SyncStateManager,
    syncRunManager: SyncRunManager?
) {
    val manager = syncRunManager
    if (manager == null) {
        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Sync jobs are not running (Check SPREADSHEET_ID configuration)."))
        return
    }

    val request = runCatching { call.receive<SyncPreviewRequest>() }.getOrDefault(SyncPreviewRequest())
    val sourceOfTruth = request.sourceOfTruth ?: syncStateManager.config.value.masterSourceOfTruth
    val preview = manager.createPreview(sourceOfTruth)
    call.respond(HttpStatusCode.OK, preview)
}

private suspend fun handlePostSyncRuns(
    call: ApplicationCall,
    syncRunManager: SyncRunManager?
) {
    val manager = syncRunManager
    if (manager == null) {
        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Sync jobs are not running (Check SPREADSHEET_ID configuration)."))
        return
    }

    val request = call.receive<SyncRunRequest>()
    try {
        val run = manager.startRun(request.previewId, request.sourceOfTruth)
        call.respond(HttpStatusCode.Accepted, run)
    } catch (e: IllegalStateException) {
        call.respond(HttpStatusCode.Conflict, mapOf("message" to (e.message ?: "Sync cannot start.")))
    }
}

private suspend fun handleGetSyncRunDetail(
    call: ApplicationCall,
    syncRunManager: SyncRunManager?
) {
    val manager = syncRunManager
    if (manager == null) {
        call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Sync jobs are not running (Check SPREADSHEET_ID configuration)."))
        return
    }

    val runId = call.parameters["runId"]
    val run = runId?.let { manager.getRun(it) }
    if (run == null) {
        call.respond(HttpStatusCode.NotFound, mapOf("message" to "Sync run not found."))
    } else {
        call.respond(HttpStatusCode.OK, run)
    }
}

private suspend fun handlePutSyncConfig(
    call: ApplicationCall,
    syncStateManager: SyncStateManager
) {
    val request = call.receive<SyncConfigUpdateRequest>()
    request.autoSyncIntervalMinutes?.let { syncStateManager.updateInterval(it) }
    request.reverseSyncSchedule?.let { syncStateManager.updateReverseSchedule(it) }
    request.masterSourceOfTruth?.let { syncStateManager.updateMasterSourceOfTruth(it) }

    call.respond(HttpStatusCode.OK, mapOf("message" to "Configuration updated successfully"))
}

private suspend fun handlePostSyncTrigger(
    call: ApplicationCall
) {
    call.respond(
        HttpStatusCode.Gone,
        mapOf("message" to "Manual sync now requires /api/sync/preview followed by /api/sync/runs.")
    )
}
