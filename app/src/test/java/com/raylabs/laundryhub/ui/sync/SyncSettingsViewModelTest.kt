package com.raylabs.laundryhub.ui.sync

import com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule
import com.raylabs.laundryhub.core.domain.model.sheets.SyncStatusResponse
import com.raylabs.laundryhub.core.domain.model.sheets.SyncTriggerResponse
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
            reverseSyncSchedule = ReverseSyncSchedule.TWICE_DAILY
        )
        whenever(repository.getSyncStatus()).thenReturn(Resource.Success(fakeResponse))

        viewModel = SyncSettingsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("2026-05-11T12:00:00", state.lastSyncTime)
        assertEquals(5, state.changesCount)
        assertEquals(30, state.autoSyncIntervalMinutes)
        assertEquals(ReverseSyncSchedule.TWICE_DAILY, state.reverseSyncSchedule)
        assertEquals(false, state.isLoading)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun `triggerManualSync updates uiState and refreshes status`() = runTest {
        whenever(repository.getSyncStatus()).thenReturn(Resource.Success(
            SyncStatusResponse("old_time", 0, 15, ReverseSyncSchedule.DEFAULT_23)
        ))
        viewModel = SyncSettingsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        whenever(repository.triggerManualSync()).thenReturn(
            Resource.Success(SyncTriggerResponse(true, "Sync successful", 2, 1))
        )
        whenever(repository.getSyncStatus()).thenReturn(Resource.Success(
            SyncStatusResponse("new_time", 3, 15, ReverseSyncSchedule.DEFAULT_23)
        ))

        viewModel.triggerManualSync()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isSyncing)
        assertEquals("Sync successful", state.successMessage)
        assertEquals("new_time", state.lastSyncTime)
        assertEquals(3, state.changesCount)
    }
}
