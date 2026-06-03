package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import com.raylabs.laundryhub.core.domain.model.sheets.SyncEntityPreview
import com.raylabs.laundryhub.core.domain.model.sheets.SyncPreviewResponse
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStatus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncRunManagerTest {

    private val previewService: SyncPreviewService = mock()
    private val batchSyncJob: SheetsBatchSyncJob = mock()
    private val reverseSyncJob: SheetsReverseSyncJob = mock()
    private val syncStateManager = SyncStateManager()

    @Test
    fun `SHEETS run with only reporting drift refreshes Gross and Summary without pulling app-owned rows`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SyncRunManager(
            previewService = previewService,
            batchSyncJob = batchSyncJob,
            reverseSyncJob = reverseSyncJob,
            syncStateManager = syncStateManager,
            scope = TestScope(dispatcher)
        )
        whenever(previewService.createPreview(MasterSourceOfTruth.SHEETS)).doReturn(
            reportingOnlyPreview(totalDifferences = 16),
            reportingOnlyPreview(totalDifferences = 0)
        )
        whenever(reverseSyncJob.pullGrossFromSheets()).doReturn(1)
        whenever(reverseSyncJob.pullSummaryFromSheets()).doReturn(15)

        val preview = manager.createPreview(MasterSourceOfTruth.SHEETS)
        val startedRun = manager.startRun(preview.previewId, MasterSourceOfTruth.SHEETS)
        testScheduler.advanceUntilIdle()

        val completedRun = manager.getRun(startedRun.runId)
        assertEquals(SyncRunStatus.SUCCEEDED, completedRun?.status)
        assertEquals(0, completedRun?.finalDifferenceCount)
        verify(reverseSyncJob).pullGrossFromSheets()
        verify(reverseSyncJob).pullSummaryFromSheets()
        verify(reverseSyncJob, never()).pullOrdersFromSheets()
        verify(reverseSyncJob, never()).pullOutcomesFromSheets()
        verify(reverseSyncJob, never()).pullPackagesFromSheets()
    }

    private fun reportingOnlyPreview(totalDifferences: Int): SyncPreviewResponse {
        val reportingDifferences = if (totalDifferences == 0) 0 else 16
        return SyncPreviewResponse(
            previewId = "preview-1",
            sourceOfTruth = MasterSourceOfTruth.SHEETS,
            generatedAt = "2026-05-30T06:52:12",
            entities = listOf(
                SyncEntityPreview("Orders", 0, 0, 0, 0, 0),
                SyncEntityPreview("Outcomes", 0, 0, 0, 0, 0),
                SyncEntityPreview("Packages", 0, 0, 0, 0, 0),
                SyncEntityPreview("Gross", 0, 0, if (reportingDifferences > 0) 1 else 0, 0, 0),
                SyncEntityPreview("Summary", 0, 0, if (reportingDifferences > 0) 15 else 0, 0, 0)
            ),
            totalDifferences = totalDifferences,
            hasBlockingConflicts = false,
            recommendedAction = "Use Google Sheets for this sync",
            appOwnedDifferenceCount = 0,
            reportingDifferenceCount = reportingDifferences
        )
    }

    @Test
    fun `test currentDifferenceCount and currentDifferenceCounts`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SyncRunManager(
            previewService = previewService,
            batchSyncJob = batchSyncJob,
            reverseSyncJob = reverseSyncJob,
            syncStateManager = syncStateManager,
            scope = TestScope(dispatcher)
        )
        whenever(previewService.createPreview(MasterSourceOfTruth.SHEETS)).doReturn(
            reportingOnlyPreview(totalDifferences = 16)
        )

        val count = manager.currentDifferenceCount(MasterSourceOfTruth.SHEETS)
        assertEquals(16, count)

        val counts = manager.currentDifferenceCounts(MasterSourceOfTruth.SHEETS)
        assertEquals(0, counts.appOwned)
        assertEquals(16, counts.reporting)
    }

    @Test
    fun `startRun throws error when preview is expired`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SyncRunManager(
            previewService = previewService,
            batchSyncJob = batchSyncJob,
            reverseSyncJob = reverseSyncJob,
            syncStateManager = syncStateManager,
            scope = TestScope(dispatcher)
        )
        val error = kotlin.test.assertFailsWith<IllegalStateException> {
            manager.startRun("invalid-id", MasterSourceOfTruth.SHEETS)
        }
        assertTrue(error.message.orEmpty().contains("Preview expired"))
    }

    @Test
    fun `startRun throws error when hasBlockingConflicts is true`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SyncRunManager(
            previewService = previewService,
            batchSyncJob = batchSyncJob,
            reverseSyncJob = reverseSyncJob,
            syncStateManager = syncStateManager,
            scope = TestScope(dispatcher)
        )
        
        val conflictPreview = SyncPreviewResponse(
            previewId = "preview-conflict",
            sourceOfTruth = MasterSourceOfTruth.SHEETS,
            generatedAt = "2026-05-30T06:52:12",
            entities = emptyList(),
            totalDifferences = 5,
            hasBlockingConflicts = true,
            recommendedAction = "Conflict",
            appOwnedDifferenceCount = 5,
            reportingDifferenceCount = 0
        )
        previewsField().set(manager, java.util.concurrent.ConcurrentHashMap<String, SyncPreviewResponse>().apply { put("preview-conflict", conflictPreview) })

        val error = kotlin.test.assertFailsWith<IllegalStateException> {
            manager.startRun("preview-conflict", MasterSourceOfTruth.SHEETS)
        }
        assertTrue(error.message.orEmpty().contains("Resolve duplicate keys"))
    }

    @Test
    fun `startRun throws error when sourceOfTruth is BOTH`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SyncRunManager(
            previewService = previewService,
            batchSyncJob = batchSyncJob,
            reverseSyncJob = reverseSyncJob,
            syncStateManager = syncStateManager,
            scope = TestScope(dispatcher)
        )
        val okPreview = reportingOnlyPreview(0)
        previewsField().set(manager, java.util.concurrent.ConcurrentHashMap<String, SyncPreviewResponse>().apply { put("preview-ok", okPreview) })

        val error = kotlin.test.assertFailsWith<IllegalStateException> {
            manager.startRun("preview-ok", MasterSourceOfTruth.BOTH)
        }
        assertTrue(error.message.orEmpty().contains("Two-way sync is disabled"))
    }

    @Test
    fun `startRun throws error when already syncing`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SyncRunManager(
            previewService = previewService,
            batchSyncJob = batchSyncJob,
            reverseSyncJob = reverseSyncJob,
            syncStateManager = syncStateManager,
            scope = TestScope(dispatcher)
        )
        val okPreview = reportingOnlyPreview(0)
        previewsField().set(manager, java.util.concurrent.ConcurrentHashMap<String, SyncPreviewResponse>().apply { put("preview-ok", okPreview) })
        syncStateManager.setSyncing(true)

        val error = kotlin.test.assertFailsWith<IllegalStateException> {
            manager.startRun("preview-ok", MasterSourceOfTruth.SHEETS)
        }
        assertTrue(error.message.orEmpty().contains("Another sync is already running"))
        syncStateManager.setSyncing(false)
    }

    @Test
    fun `SUPABASE run applies database changes to sheets`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SyncRunManager(
            previewService = previewService,
            batchSyncJob = batchSyncJob,
            reverseSyncJob = reverseSyncJob,
            syncStateManager = syncStateManager,
            scope = TestScope(dispatcher)
        )
        val okPreview = SyncPreviewResponse(
            previewId = "preview-ok",
            sourceOfTruth = MasterSourceOfTruth.SUPABASE,
            generatedAt = "2026-05-30T06:52:12",
            entities = emptyList(),
            totalDifferences = 5,
            hasBlockingConflicts = false,
            recommendedAction = "Push",
            appOwnedDifferenceCount = 5,
            reportingDifferenceCount = 0
        )
        previewsField().set(manager, java.util.concurrent.ConcurrentHashMap<String, SyncPreviewResponse>().apply { put("preview-ok", okPreview) })
        
        whenever(previewService.createPreview(MasterSourceOfTruth.SUPABASE)).doReturn(
            okPreview.copy(totalDifferences = 0)
        )
        whenever(batchSyncJob.processAllOrdersToSheets()).doReturn(2)
        whenever(batchSyncJob.processAllOutcomesToSheets()).doReturn(2)
        whenever(batchSyncJob.processAllPackagesToSheets()).doReturn(1)
        whenever(batchSyncJob.processPendingDeletes()).doReturn(0)

        val startedRun = manager.startRun("preview-ok", MasterSourceOfTruth.SUPABASE)
        testScheduler.advanceUntilIdle()

        val completedRun = manager.getRun(startedRun.runId)
        assertEquals(SyncRunStatus.SUCCEEDED, completedRun?.status)
        verify(batchSyncJob).processAllOrdersToSheets()
        verify(batchSyncJob).processAllOutcomesToSheets()
        verify(batchSyncJob).processAllPackagesToSheets()
        verify(batchSyncJob).processPendingDeletes()
    }

    @Test
    fun `SHEETS run with app-owned drift pulls all entity tables`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = SyncRunManager(
            previewService = previewService,
            batchSyncJob = batchSyncJob,
            reverseSyncJob = reverseSyncJob,
            syncStateManager = syncStateManager,
            scope = TestScope(dispatcher)
        )
        val previewWithAppOwned = SyncPreviewResponse(
            previewId = "preview-app",
            sourceOfTruth = MasterSourceOfTruth.SHEETS,
            generatedAt = "2026-05-30T06:52:12",
            entities = emptyList(),
            totalDifferences = 5,
            hasBlockingConflicts = false,
            recommendedAction = "Pull",
            appOwnedDifferenceCount = 5,
            reportingDifferenceCount = 0
        )
        previewsField().set(manager, java.util.concurrent.ConcurrentHashMap<String, SyncPreviewResponse>().apply { put("preview-app", previewWithAppOwned) })
        
        whenever(previewService.createPreview(MasterSourceOfTruth.SHEETS)).doReturn(
            previewWithAppOwned.copy(totalDifferences = 0)
        )
        whenever(reverseSyncJob.pullOrdersFromSheets()).doReturn(1)
        whenever(reverseSyncJob.pullOutcomesFromSheets()).doReturn(1)
        whenever(reverseSyncJob.pullPackagesFromSheets()).doReturn(1)
        whenever(reverseSyncJob.pullGrossFromSheets()).doReturn(1)
        whenever(reverseSyncJob.pullSummaryFromSheets()).doReturn(1)

        val startedRun = manager.startRun("preview-app", MasterSourceOfTruth.SHEETS)
        testScheduler.advanceUntilIdle()

        val completedRun = manager.getRun(startedRun.runId)
        assertEquals(SyncRunStatus.SUCCEEDED, completedRun?.status)
        verify(reverseSyncJob).pullOrdersFromSheets()
        verify(reverseSyncJob).pullOutcomesFromSheets()
        verify(reverseSyncJob).pullPackagesFromSheets()
        verify(reverseSyncJob).pullGrossFromSheets()
        verify(reverseSyncJob).pullSummaryFromSheets()
    }

    @Test
    fun `executeRun handles exception and marks status as FAILED`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val failingReverseSyncJob = mock<SheetsReverseSyncJob>(defaultAnswer = org.mockito.stubbing.Answer { invocation ->
            if (invocation.method.name == "pullGrossFromSheets") {
                throw RuntimeException("DB write error")
            }
            0
        })
        val manager = SyncRunManager(
            previewService = previewService,
            batchSyncJob = batchSyncJob,
            reverseSyncJob = failingReverseSyncJob,
            syncStateManager = syncStateManager,
            scope = TestScope(dispatcher)
        )
        val okPreview = reportingOnlyPreview(5)
        previewsField().set(manager, java.util.concurrent.ConcurrentHashMap<String, SyncPreviewResponse>().apply { put("preview-ok", okPreview) })

        val startedRun = manager.startRun("preview-ok", MasterSourceOfTruth.SHEETS)
        testScheduler.advanceUntilIdle()

        val completedRun = manager.getRun(startedRun.runId)
        assertEquals(SyncRunStatus.FAILED, completedRun?.status)
        assertEquals("DB write error", completedRun?.lastError)
    }

    @Suppress("UNCHECKED_CAST")
    private fun previewsField() = SyncRunManager::class.java.getDeclaredField("previews").apply {
        isAccessible = true
    }
}
