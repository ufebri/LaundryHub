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
}
