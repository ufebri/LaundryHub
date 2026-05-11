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
                    reverseSyncSchedule = config.reverseSyncSchedule
                )
            )
        }

        put("/config") {
            val request = call.receive<SyncConfigUpdateRequest>()
            request.autoSyncIntervalMinutes?.let { syncStateManager.updateInterval(it) }
            request.reverseSyncSchedule?.let { syncStateManager.updateReverseSchedule(it) }

            call.respond(HttpStatusCode.OK, mapOf("message" to "Configuration updated successfully"))
        }

        post("/trigger") {
            if (batchSyncJob == null || reverseSyncJob == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("message" to "Sync jobs are not running (Check SPREADSHEET_ID configuration)."))
                return@post
            }

            try {
                // Run job 1 (Push to sheets)
                val pushedCount = batchSyncJob.processUnsyncedAll()
                
                // Run job 2 (Pull from sheets)
                val pulledCount = reverseSyncJob.processReverseSync()

                val totalChanges = pushedCount + pulledCount
                if (totalChanges > 0) {
                    syncStateManager.recordSync(totalChanges)
                }

                call.respond(
                    HttpStatusCode.OK,
                    SyncTriggerResponse(
                        success = true,
                        message = "Manual sync completed.",
                        itemsPushed = pushedCount,
                        itemsPulled = pulledCount
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    SyncTriggerResponse(
                        success = false,
                        message = e.message ?: "Unknown error occurred during manual sync.",
                        itemsPushed = 0,
                        itemsPulled = 0
                    )
                )
            }
        }
    }
}
