package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.service.SheetsBatchSyncJob
import com.raylabs.laundryhub.backend.service.SheetsReverseSyncJob
import com.raylabs.laundryhub.backend.service.SyncStateManager
import com.raylabs.laundryhub.core.domain.model.sheets.SyncConfigUpdateRequest
import com.raylabs.laundryhub.core.domain.model.sheets.SyncStatusResponse
import com.raylabs.laundryhub.core.domain.model.sheets.SyncTriggerResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Route.syncRoutes(
    syncStateManager: SyncStateManager,
    batchSyncJob: SheetsBatchSyncJob?,
    reverseSyncJob: SheetsReverseSyncJob?
) {
    route("/api/sync") {
        get("/status") {
            val config = syncStateManager.config.value
            call.respond(
                HttpStatusCode.OK,
                SyncStatusResponse(
                    lastSyncTime = syncStateManager.lastSyncTime,
                    changesCount = syncStateManager.lastChangesCount,
                    autoSyncIntervalMinutes = config.intervalMinutes,
                    reverseSyncSchedule = config.reverseSyncSchedule,
                    isSyncing = syncStateManager.isSyncing
                )
            )
        }

        put("/config") {
            val request = call.receive<SyncConfigUpdateRequest>()
            request.autoSyncIntervalMinutes?.let { syncStateManager.updateInterval(it) }
            request.reverseSyncSchedule?.let { syncStateManager.updateReverseSchedule(it) }
            request.masterSourceOfTruth?.let { syncStateManager.updateMasterSourceOfTruth(it) }

            call.respond(HttpStatusCode.OK, mapOf("message" to "Configuration updated successfully"))
        }

        post("/trigger") {
            if (batchSyncJob == null || reverseSyncJob == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Sync jobs are not running (Check SPREADSHEET_ID configuration)."))
                return@post
            }

            if (syncStateManager.isSyncing) {
                call.respond(HttpStatusCode.Conflict, mapOf("message" to "Sync is already running."))
                return@post
            }

            syncStateManager.setSyncing(true)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val sourceOfTruth = syncStateManager.config.value.masterSourceOfTruth.name
                    var totalChanges = 0

                    if (sourceOfTruth.equals("SUPABASE", ignoreCase = true) || sourceOfTruth.equals("BOTH", ignoreCase = true)) {
                        // Push to sheets
                        totalChanges += batchSyncJob.processUnsyncedAll()
                    }

                    if (sourceOfTruth.equals("SHEETS", ignoreCase = true) || sourceOfTruth.equals("BOTH", ignoreCase = true)) {
                        // Pull from sheets and overwrite local Database
                        totalChanges += reverseSyncJob.processReverseSync()
                    }

                    if (totalChanges > 0) {
                        syncStateManager.recordSync(totalChanges)
                    }
                } catch (e: Exception) {
                    // Log error if needed, state is reset in finally
                } finally {
                    syncStateManager.setSyncing(false)
                }
            }

            call.respond(
                HttpStatusCode.Accepted,
                SyncTriggerResponse(
                    success = true,
                    message = "Background sync started.",
                    itemsPushed = 0,
                    itemsPulled = 0
                )
            )
        }
    }
}
