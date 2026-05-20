package com.raylabs.laundryhub.ui.home

import androidx.paging.PagingData
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.usecase.reminder.EvaluateReminderCandidatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderLocalStatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadGrossDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val summaryUseCase: ReadSpreadsheetDataUseCase = mock()
    private val grossUseCase: ReadGrossDataUseCase = mock()
    private val readIncomeUseCase: ReadIncomeTransactionUseCase = mock()
    private val userUseCase: UserUseCase = mock()
    private val observeReminderSettingsUseCase: ObserveReminderSettingsUseCase = mock()
    private val observeReminderLocalStatesUseCase: ObserveReminderLocalStatesUseCase = mock()
    private val evaluateReminderCandidatesUseCase: EvaluateReminderCandidatesUseCase = mock()
    private val repository: com.raylabs.laundryhub.core.domain.repository.LaundryRepository = mock()

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        whenever(observeReminderSettingsUseCase.invoke()).thenReturn(flowOf(mock()))
        whenever(observeReminderLocalStatesUseCase.invoke()).thenReturn(flowOf(emptyMap()))
        whenever(summaryUseCase.invoke(anyOrNull())).thenReturn(Resource.Success(listOf(SpreadsheetData("key", "value"))))
        whenever(grossUseCase.invoke(anyOrNull())).thenReturn(Resource.Success(emptyList()))
        whenever(repository.getSyncStatus()).thenReturn(Resource.Success(com.raylabs.laundryhub.core.domain.model.sheets.SyncStatusResponse(
            lastSyncTime = null,
            changesCount = 0,
            autoSyncIntervalMinutes = 15,
            reverseSyncSchedule = com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule.MANUAL,
            masterSourceOfTruth = com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth.SUPABASE,
            isSyncing = false,
            lastSyncStatus = "SUCCESS",
            lastSyncError = null,
            pendingPushCount = 0,
            pendingDeleteCount = 0,
            nextScheduledPushTime = null
        )))
        
        // Also mock the parameterless calls if defaults are used
        whenever(summaryUseCase.invoke()).thenReturn(Resource.Success(listOf(SpreadsheetData("key", "value"))))
        whenever(grossUseCase.invoke()).thenReturn(Resource.Success(emptyList()))

        whenever(grossUseCase.getPagingData()).thenReturn(flowOf(mock()))
        whenever(readIncomeUseCase.getPagingData(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(flowOf(PagingData.empty()))
        whenever(readIncomeUseCase.invoke(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Resource.Success(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init fetches summary and today income`() = runTest {
        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.summary.data)
        assertNotNull(viewModel.uiState.value.todayIncome.data)
    }

    @Test
    fun `onSearchQueryChanged updates state`() = runTest {
        val viewModel = createViewModel()
        viewModel.onSearchQueryChanged("test")
        assertEquals("test", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `pending search ignores one character and debounces remote queries`() = runTest {
        val viewModel = createViewModel()
        val job = launch {
            viewModel.pendingOrdersPagingData.collect {}
        }
        dispatcher.scheduler.advanceUntilIdle()
        clearInvocations(readIncomeUseCase)

        viewModel.onSearchQueryChanged("a")
        dispatcher.scheduler.advanceTimeBy(500)
        dispatcher.scheduler.advanceUntilIdle()

        verify(readIncomeUseCase, never()).getPagingData(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())

        viewModel.onSearchQueryChanged("ab")
        dispatcher.scheduler.advanceTimeBy(449)
        dispatcher.scheduler.runCurrent()
        verify(readIncomeUseCase, never()).getPagingData(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())

        dispatcher.scheduler.advanceTimeBy(1)
        dispatcher.scheduler.advanceUntilIdle()

        verify(readIncomeUseCase).getPagingData(
            filter = eq(FILTER.SHOW_UNPAID_DATA),
            rangeDate = anyOrNull(),
            searchQuery = eq("ab"),
            sort = anyOrNull()
        )
        job.cancel()
    }

    @Test
    fun `refreshAllData triggers manual sync and updates state`() = runTest {
        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        
        whenever(summaryUseCase.invoke(anyOrNull())).thenReturn(Resource.Success(emptyList()))
        whenever(grossUseCase.invoke(anyOrNull())).thenReturn(Resource.Success(emptyList()))
        whenever(repository.getSyncStatus()).thenReturn(Resource.Success(com.raylabs.laundryhub.core.domain.model.sheets.SyncStatusResponse(
            lastSyncTime = null,
            changesCount = 0,
            autoSyncIntervalMinutes = 15,
            reverseSyncSchedule = com.raylabs.laundryhub.core.domain.model.sheets.ReverseSyncSchedule.MANUAL,
            masterSourceOfTruth = com.raylabs.laundryhub.core.domain.model.sheets.MasterSourceOfTruth.SUPABASE,
            isSyncing = false,
            lastSyncStatus = "SUCCESS",
            lastSyncError = null,
            pendingPushCount = 0,
            pendingDeleteCount = 0,
            nextScheduledPushTime = null
        )))
        
        viewModel.refreshAllData()
        
        // Wait for coroutines to execute
        dispatcher.scheduler.advanceTimeBy(3000) // Advance past delays
        dispatcher.scheduler.advanceUntilIdle()
        
        verify(repository).triggerManualSync()
        assertEquals(false, viewModel.uiState.value.isRefreshing)
        assertEquals(false, viewModel.uiState.value.isSummaryRefreshing)
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            repository = repository,
            summaryUseCase = summaryUseCase,
            grossUseCase = grossUseCase,
            readIncomeUseCase = readIncomeUseCase,
            userUseCase = userUseCase,
            observeReminderSettingsUseCase = observeReminderSettingsUseCase,
            observeReminderLocalStatesUseCase = observeReminderLocalStatesUseCase,
            evaluateReminderCandidatesUseCase = evaluateReminderCandidatesUseCase
        )
    }
}
