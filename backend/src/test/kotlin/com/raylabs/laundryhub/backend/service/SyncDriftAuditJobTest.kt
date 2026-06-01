package com.raylabs.laundryhub.backend.service

import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import com.raylabs.laundryhub.core.domain.model.sheets.SyncEntityPreview
import com.raylabs.laundryhub.core.domain.model.sheets.SyncPreviewResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncDriftAuditJobTest {

    private val previewService: SyncPreviewService = mock()
    private val syncStateManager = SyncStateManager()

    private fun createJob(scope: kotlinx.coroutines.CoroutineScope, intervalMinutes: Int = 15) = SyncDriftAuditJob(
        previewService = previewService,
        syncStateManager = syncStateManager,
        intervalMinutes = intervalMinutes,
        scope = scope
    )

    @Test
    fun testAuditOnceSkippedWhenAlreadySyncing() = runTest {
        val job = createJob(this)
        syncStateManager.setSyncing(true)

        job.auditOnce()
        
        // Ensure createPreview was never called since we were already syncing
        verify(previewService, times(0)).createPreview(any())
    }

    @Test
    fun testAuditOnceClean() = runTest {
        val job = createJob(this)
        val cleanResponse = SyncPreviewResponse(
            previewId = "test-preview-id",
            sourceOfTruth = MasterSourceOfTruth.SUPABASE,
            generatedAt = "2026-06-01T12:00:00Z",
            entities = emptyList(),
            totalDifferences = 0,
            hasBlockingConflicts = false,
            recommendedAction = "NONE"
        )

        whenever(previewService.createPreview(MasterSourceOfTruth.SUPABASE)).thenReturn(cleanResponse)

        job.auditOnce()

        verify(previewService).createPreview(MasterSourceOfTruth.SUPABASE)
    }

    @Test
    fun testAuditOnceWithDifferences() = runTest {
        val job = createJob(this)
        val entities = listOf(
            SyncEntityPreview(
                entity = "Orders",
                onlyInSheets = 2,
                onlyInDatabase = 1,
                changedRows = 3,
                duplicateKeys = 0,
                pendingDeletes = 0,
                onlyInSheetKeys = listOf("key1", "key2"),
                onlyInDatabaseKeys = listOf("key3"),
                changedRowKeys = listOf("key4", "key5", "key6")
            )
        )
        val driftResponse = SyncPreviewResponse(
            previewId = "test-preview-id",
            sourceOfTruth = MasterSourceOfTruth.SUPABASE,
            generatedAt = "2026-06-01T12:00:00Z",
            entities = entities,
            totalDifferences = 6,
            hasBlockingConflicts = false,
            recommendedAction = "PUSH"
        )

        whenever(previewService.createPreview(MasterSourceOfTruth.SUPABASE)).thenReturn(driftResponse)

        job.auditOnce()

        verify(previewService).createPreview(MasterSourceOfTruth.SUPABASE)
    }

    @Test
    fun testAuditOnceFailure() = runTest {
        val job = createJob(this)

        whenever(previewService.createPreview(MasterSourceOfTruth.SUPABASE)).doThrow(RuntimeException("Network error"))

        job.auditOnce()

        verify(previewService).createPreview(MasterSourceOfTruth.SUPABASE)
    }

    @Test
    fun testAuditJobLoop() = runTest {
        try {
            val job = createJob(this, intervalMinutes = 15)

            val cleanResponse = SyncPreviewResponse(
                previewId = "test-preview-id",
                sourceOfTruth = MasterSourceOfTruth.SUPABASE,
                generatedAt = "2026-06-01T12:00:00Z",
                entities = emptyList(),
                totalDifferences = 0,
                hasBlockingConflicts = false,
                recommendedAction = "NONE"
            )
            whenever(previewService.createPreview(MasterSourceOfTruth.SUPABASE)).thenReturn(cleanResponse)

            job.start()
            runCurrent()

            // Drift audit job has an initial delay of 30 seconds
            advanceTimeBy(30 * 1000L)
            runCurrent()
            verify(previewService, times(1)).createPreview(MasterSourceOfTruth.SUPABASE)

            // Advance by interval (15 minutes)
            advanceTimeBy(15 * 60 * 1000L)
            runCurrent()
            verify(previewService, times(2)).createPreview(MasterSourceOfTruth.SUPABASE)
        } finally {
            coroutineContext.cancelChildren()
        }
    }
}
