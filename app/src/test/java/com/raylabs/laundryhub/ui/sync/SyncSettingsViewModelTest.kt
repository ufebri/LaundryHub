package com.raylabs.laundryhub.ui.sync

import com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth
import com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule
import com.raylabs.laundryhub.core.domain.model.sheets.SyncEntityPreview
import com.raylabs.laundryhub.core.domain.model.sheets.SyncPreviewResponse
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStage
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStartResponse
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStatus
import com.raylabs.laundryhub.core.domain.model.sheets.SyncRunStatusResponse
import com.raylabs.laundryhub.core.domain.model.sheets.SyncStatusResponse
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SyncSettingsViewModelTest {

    private lateinit var viewModel: SyncSettingsViewModel
    private val repository: LaundryRepository = mock()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchStatus updates uiState successfully`() = runTest {
        val fakeResponse = SyncStatusResponse(
            lastSyncTime = "2026-05-11T12:00:00",
            changesCount = 5,
            autoSyncIntervalMinutes = 30,
            reverseSyncSchedule = ReverseSyncSchedule.TWICE_DAILY,
            dataDifferenceCount = 2,
            reportingDifferenceCount = 3
        )
        whenever(repository.getSyncStatus()).thenReturn(Resource.Success(fakeResponse))

        viewModel = SyncSettingsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("2026-05-11T12:00:00", state.lastSyncTime)
        assertEquals(5, state.changesCount)
        assertEquals(30, state.autoSyncIntervalMinutes)
        assertEquals(ReverseSyncSchedule.TWICE_DAILY, state.reverseSyncSchedule)
        assertEquals(2, state.dataDifferenceCount)
        assertEquals(3, state.reportingDifferenceCount)
        assertEquals(false, state.isLoading)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun `checkDifferences stores preview for user confirmation`() = runTest {
        whenever(repository.getSyncStatus()).thenReturn(Resource.Success(
            SyncStatusResponse("old_time", 0, 15, ReverseSyncSchedule.MANUAL)
        ))
        whenever(repository.previewSync(any())).thenReturn(Resource.Success(previewWithDifferences()))

        viewModel = SyncSettingsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.checkDifferences()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isCheckingDifferences)
        assertEquals(3, state.syncPreview?.totalDifferences)
        assertEquals(null, state.successMessage)
    }

    @Test
    fun `confirmSyncNow starts run and stores completed progress`() = runTest {
        whenever(repository.getSyncStatus()).thenReturn(Resource.Success(
            SyncStatusResponse("old_time", 0, 15, ReverseSyncSchedule.MANUAL)
        ))
        whenever(repository.previewSync(any())).thenReturn(Resource.Success(previewWithDifferences()))
        whenever(repository.startSyncRun(any())).thenReturn(Resource.Success(SyncRunStartResponse("run-1")))
        whenever(repository.getSyncRunStatus("run-1")).thenReturn(
            Resource.Success(
                SyncRunStatusResponse(
                    runId = "run-1",
                    previewId = "preview-1",
                    status = SyncRunStatus.SUCCEEDED,
                    stage = SyncRunStage.COMPLETED,
                    message = "Sync completed",
                    processedItems = 3,
                    totalItems = 3,
                    finalDifferenceCount = 0
                )
            )
        )
        viewModel = SyncSettingsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.checkDifferences()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.confirmSyncNow()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isSyncing)
        assertEquals("Sync completed successfully.", state.successMessage)
        assertEquals(SyncRunStatus.SUCCEEDED, state.activeRun?.status)
    }

    @Test
    fun `fetchStatus handles masterSourceOfTruth BOTH by fallback to SHEETS`() = runTest {
        val fakeResponse = SyncStatusResponse(
            lastSyncTime = "2026-05-11T12:00:00",
            changesCount = 0,
            autoSyncIntervalMinutes = 15,
            reverseSyncSchedule = ReverseSyncSchedule.MANUAL,
            masterSourceOfTruth = MasterSourceOfTruth.BOTH
        )
        whenever(repository.getSyncStatus()).thenReturn(Resource.Success(fakeResponse))

        viewModel = SyncSettingsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(MasterSourceOfTruth.SHEETS, state.selectedSourceOfTruth)
    }

    @Test
    fun `fetchStatus handles Resource Error`() = runTest {
        whenever(repository.getSyncStatus()).thenReturn(Resource.Error("Network failure"))

        viewModel = SyncSettingsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Network failure", state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `selectSourceOfTruth updates state and ignores BOTH`() = runTest {
        whenever(repository.getSyncStatus()).thenReturn(Resource.Success(
            SyncStatusResponse("old_time", 0, 15, ReverseSyncSchedule.MANUAL, MasterSourceOfTruth.SHEETS)
        ))

        viewModel = SyncSettingsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectSourceOfTruth(MasterSourceOfTruth.BOTH)
        assertEquals(MasterSourceOfTruth.SHEETS, viewModel.uiState.value.selectedSourceOfTruth)

        viewModel.selectSourceOfTruth(MasterSourceOfTruth.SUPABASE)
        assertEquals(MasterSourceOfTruth.SUPABASE, viewModel.uiState.value.selectedSourceOfTruth)
    }

    @Test
    fun `fetchStatus starts polling when isSyncing is true`() = runTest {
        val response1 = SyncStatusResponse("old_time", 0, 15, ReverseSyncSchedule.MANUAL, isSyncing = true)
        val response2 = SyncStatusResponse("old_time", 0, 15, ReverseSyncSchedule.MANUAL, isSyncing = false)
        
        whenever(repository.getSyncStatus()).thenReturn(
            Resource.Success(response1),
            Resource.Success(response2)
        )

        viewModel = SyncSettingsViewModel(repository)
        testDispatcher.scheduler.runCurrent()

        assertEquals(true, viewModel.uiState.value.isSyncing)

        testDispatcher.scheduler.advanceTimeBy(3000)
        testDispatcher.scheduler.runCurrent()

        assertEquals(false, viewModel.uiState.value.isSyncing)
        assertEquals("Sync completed successfully.", viewModel.uiState.value.successMessage)
    }

    private fun previewWithDifferences(): SyncPreviewResponse {
        return SyncPreviewResponse(
            previewId = "preview-1",
            sourceOfTruth = MasterSourceOfTruth.SHEETS,
            generatedAt = "2026-05-11T12:00:00",
            entities = listOf(
                SyncEntityPreview(
                    entity = "Orders",
                    onlyInSheets = 1,
                    onlyInDatabase = 1,
                    changedRows = 1,
                    duplicateKeys = 0,
                    pendingDeletes = 0
                )
            ),
            totalDifferences = 3,
            hasBlockingConflicts = false,
            recommendedAction = "Use Google Sheets for this sync"
        )
    }
}
