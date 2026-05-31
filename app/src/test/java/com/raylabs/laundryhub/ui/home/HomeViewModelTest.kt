package com.raylabs.laundryhub.ui.home

import androidx.paging.PagingData
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.usecase.reminder.EvaluateReminderCandidatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderLocalStatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadGrossDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.ui.home.state.SortOption
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
    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        whenever(observeReminderSettingsUseCase.invoke()).thenReturn(flowOf(mock()))
        whenever(observeReminderLocalStatesUseCase.invoke()).thenReturn(flowOf(emptyMap()))
        whenever(summaryUseCase.invoke(anyOrNull())).thenReturn(Resource.Success(listOf(SpreadsheetData("key", "value"))))
        whenever(grossUseCase.invoke(anyOrNull())).thenReturn(Resource.Success(emptyList()))
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
    fun `refreshAllData fetches visible data without manual sync`() = runTest {
        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        
        whenever(summaryUseCase.invoke(anyOrNull())).thenReturn(Resource.Success(emptyList()))
        whenever(grossUseCase.invoke(anyOrNull())).thenReturn(Resource.Success(emptyList()))
        
        viewModel.refreshAllData()
        
        dispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(false, viewModel.uiState.value.isRefreshing)
        assertEquals(false, viewModel.uiState.value.isSummaryRefreshing)
    }

    @Test
    fun `summary uses current gross row when endpoint returns oldest first`() = runTest {
        whenever(grossUseCase.invoke(anyOrNull())).thenReturn(
            Resource.Success(
                listOf(
                    GrossData(month = "Maret 2025", totalNominal = "1038150", orderCount = "35", tax = "5191"),
                    GrossData(month = "Mei 2026", totalNominal = "3343000", orderCount = "115", tax = "16715"),
                    GrossData(month = "Desember 2999", totalNominal = "9999", orderCount = "9", tax = "99")
                )
            )
        )
        whenever(grossUseCase.invoke()).thenReturn(
            Resource.Success(
                listOf(
                    GrossData(month = "Maret 2025", totalNominal = "1038150", orderCount = "35", tax = "5191"),
                    GrossData(month = "Mei 2026", totalNominal = "3343000", orderCount = "115", tax = "16715"),
                    GrossData(month = "Desember 2999", totalNominal = "9999", orderCount = "9", tax = "99")
                )
            )
        )

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val grossSummary = viewModel.uiState.value.summary.data?.single { it.title == "Gross Income" }
        assertEquals("Rp 3.343.000", grossSummary?.body)
        assertEquals("115 order", grossSummary?.footer)
    }

    @Test
    fun `verify dummyHomeUiState variables are initialized`() {
        assertNotNull(com.raylabs.laundryhub.ui.common.dummy.home.dummyState)
        assertNotNull(com.raylabs.laundryhub.ui.common.dummy.home.DUMMY_UNPAID_ORDER_ITEM_EMY)
        assertNotNull(com.raylabs.laundryhub.ui.common.dummy.home.DUMMY_UNPAID_ORDER_ITEM_GABRIEL)
        assertNotNull(com.raylabs.laundryhub.ui.common.dummy.home.DUMMY_UNPAID_ORDER_ITEM_ARIFIN)
    }

    @Test
    fun `toggleSearch updates search active state`() = runTest {
        val viewModel = createViewModel()
        assertEquals(false, viewModel.uiState.value.isSearchActive)
        
        viewModel.toggleSearch()
        assertEquals(true, viewModel.uiState.value.isSearchActive)
        
        viewModel.toggleSearch()
        assertEquals(false, viewModel.uiState.value.isSearchActive)
    }

    @Test
    fun `changeSortOrder updates state`() = runTest {
        val viewModel = createViewModel()
        viewModel.changeSortOrder(SortOption.DUE_DATE_ASC)
        assertEquals(SortOption.DUE_DATE_ASC, viewModel.uiState.value.currentSortOption)
    }

    @Test
    fun `refresh methods do not crash`() = runTest {
        val viewModel = createViewModel()
        viewModel.refreshAfterOrderChanged()
        viewModel.refreshAfterOrderChangedSilent()
        viewModel.refreshAfterOutcomeChanged()
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `optimistic order operations update state`() = runTest {
        val viewModel = createViewModel()
        val dummyOrder = com.raylabs.laundryhub.ui.home.state.UnpaidOrderItem(
            orderID = "fake-1",
            customerName = "Test customer",
            packageType = "Regular",
            nowStatus = "Unpaid",
            dueDate = "",
            orderDate = ""
        )
        
        viewModel.addOptimisticOrder(dummyOrder)
        assertEquals(1, viewModel.uiState.value.optimisticOrders.size)
        assertEquals("fake-1", viewModel.uiState.value.optimisticOrders.first().orderID)
        
        viewModel.updateOptimisticOrderStatus("fake-1", com.raylabs.laundryhub.ui.home.state.SyncStatus.SYNCED, "real-1")
        assertEquals("real-1", viewModel.uiState.value.optimisticOrders.first().orderID)
        assertEquals(com.raylabs.laundryhub.ui.home.state.SyncStatus.SYNCED, viewModel.uiState.value.optimisticOrders.first().syncStatus)
        
        viewModel.removeOptimisticOrder("real-1")
        assertEquals(0, viewModel.uiState.value.optimisticOrders.size)
    }

    @Test
    fun `optimistic update operations update state`() = runTest {
        val viewModel = createViewModel()
        val dummyOrder = com.raylabs.laundryhub.ui.home.state.UnpaidOrderItem(
            orderID = "1",
            customerName = "Test customer",
            packageType = "Regular",
            nowStatus = "Unpaid",
            dueDate = "",
            orderDate = ""
        )
        
        viewModel.addOptimisticUpdate("1", dummyOrder)
        assertEquals(1, viewModel.uiState.value.optimisticUpdates.size)
        
        viewModel.removeOptimisticUpdate("1")
        assertEquals(0, viewModel.uiState.value.optimisticUpdates.size)
        
        viewModel.addOptimisticUpdate("2", dummyOrder)
        viewModel.clearOptimisticUpdates()
        assertEquals(0, viewModel.uiState.value.optimisticUpdates.size)
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
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
