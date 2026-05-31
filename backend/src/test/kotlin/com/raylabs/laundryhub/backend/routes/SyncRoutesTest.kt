package com.raylabs.laundryhub.backend.routes

import com.raylabs.laundryhub.backend.service.SheetsBatchSyncJob
import com.raylabs.laundryhub.backend.service.SheetsPushScheduler
import com.raylabs.laundryhub.backend.service.SyncRunManager
import com.raylabs.laundryhub.backend.service.SyncStateManager
import com.raylabs.laundryhub.backend.service.SyncConfig
import com.raylabs.laundryhub.backend.service.SyncDifferenceCounts
import com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule
import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStatusResponse
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStatus
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStage
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.flow.MutableStateFlow
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncRoutesTest {

    private val syncStateManager: SyncStateManager = mock()
    private val batchSyncJob: SheetsBatchSyncJob = mock()
    private val sheetsPushScheduler: SheetsPushScheduler = mock()
    private val syncRunManager: SyncRunManager = mock()

    @Test
    fun `get status returns correct sync status payload`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        val configFlow = MutableStateFlow(
            SyncConfig(
                intervalMinutes = 15,
                reverseSyncSchedule = ReverseSyncSchedule.DEFAULT_23,
                masterSourceOfTruth = MasterSourceOfTruth.BOTH
            )
        )
        whenever(syncStateManager.config).thenReturn(configFlow)
        whenever(syncStateManager.lastSyncTime).thenReturn("2026-05-31T12:00:00Z")
        whenever(syncStateManager.lastChangesCount).thenReturn(5)
        whenever(syncStateManager.isSyncing).thenReturn(false)
        whenever(syncStateManager.lastSyncStatus).thenReturn("Success")
        whenever(syncStateManager.lastSyncError).thenReturn(null)
        whenever(batchSyncJob.pendingPushCount()).thenReturn(2)
        whenever(batchSyncJob.pendingDeleteCount()).thenReturn(1)
        whenever(sheetsPushScheduler.nextScheduledPushTime).thenReturn("2026-05-31T13:00:00Z")
        whenever(syncRunManager.currentDifferenceCounts(any())).thenReturn(SyncDifferenceCounts(appOwned = 3, reporting = 2))

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                syncRoutes(
                    syncStateManager = syncStateManager,
                    batchSyncJob = batchSyncJob,
                    sheetsPushScheduler = sheetsPushScheduler,
                    syncRunManager = syncRunManager
                )
            }
        }

        val response = client.get("/api/sync/status")
        assertEquals(HttpStatusCode.OK, response.status)
        val text = response.bodyAsText()
        assertTrue(text.contains("2026-05-31T12:00:00Z"))
        assertTrue(text.contains("PENDING_PUSH_AND_DATA_DIFFERENCES"))
    }

    @Test
    fun `put config updates settings successfully`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                syncRoutes(
                    syncStateManager = syncStateManager,
                    batchSyncJob = batchSyncJob,
                    sheetsPushScheduler = sheetsPushScheduler,
                    syncRunManager = syncRunManager
                )
            }
        }

        val response = client.put("/api/sync/config") {
            contentType(ContentType.Application.Json)
            setBody("""{"autoSyncIntervalMinutes":30,"reverseSyncSchedule":"MANUAL","masterSourceOfTruth":"SUPABASE"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("successfully"))
        verify(syncStateManager).updateInterval(30)
        verify(syncStateManager).updateReverseSchedule(ReverseSyncSchedule.MANUAL)
        verify(syncStateManager).updateMasterSourceOfTruth(MasterSourceOfTruth.SUPABASE)
    }

    @Test
    fun `post runs triggers a sync run`() = testApplication {
        environment {
            config = MapApplicationConfig()
        }
        val runResponse = SyncRunStatusResponse(
            runId = "run-123",
            previewId = "preview-123",
            status = SyncRunStatus.SUCCEEDED,
            stage = SyncRunStage.COMPLETED,
            message = "Queued",
            processedItems = 0,
            totalItems = 5
        )
        whenever(syncRunManager.startRun(any(), any())).thenReturn(runResponse)

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                syncRoutes(
                    syncStateManager = syncStateManager,
                    batchSyncJob = batchSyncJob,
                    sheetsPushScheduler = sheetsPushScheduler,
                    syncRunManager = syncRunManager
                )
            }
        }

        val response = client.post("/api/sync/runs") {
            contentType(ContentType.Application.Json)
            setBody("""{"previewId":"preview-123","sourceOfTruth":"SUPABASE"}""")
        }

        assertEquals(HttpStatusCode.Accepted, response.status)
        assertTrue(response.bodyAsText().contains("run-123"))
    }
}
