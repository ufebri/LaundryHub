package com.raylabs.laundryhub.ui.home

import androidx.paging.PagingData
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderBucket
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderCandidate
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.usecase.reminder.EvaluateReminderCandidatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderLocalStatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadGrossDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.home.state.SortOption
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
import org.junit.Assert.assertTrue
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
        whenever(summaryUseCase.invoke(null)).thenReturn(Resource.Success(listOf(SpreadsheetData("key", "value"))))
        whenever(grossUseCase.invoke(null)).thenReturn(Resource.Success(emptyList()))
        // Also mock the parameterless calls if defaults are used
        whenever(summaryUseCase.invoke()).thenReturn(Resource.Success(listOf(SpreadsheetData("key", "value"))))
        whenever(grossUseCase.invoke()).thenReturn(Resource.Success(emptyList()))

        whenever(grossUseCase.getPagingData()).thenReturn(flowOf(mock()))
        whenever(readIncomeUseCase.getPagingData(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(flowOf(PagingData.empty()))
        val today = com.raylabs.laundryhub.shared.util.PlatformDate.getTodayDate("yyyy-MM-dd")
        val range = RangeDate(startDate = today, endDate = today)
        whenever(readIncomeUseCase.invoke(null, FILTER.RANGE_TRANSACTION_DATA, range)).thenReturn(Resource.Success(emptyList()))
        whenever(readIncomeUseCase.invoke(null, FILTER.SHOW_UNPAID_DATA, null)).thenReturn(Resource.Success(emptyList()))
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
        
        whenever(summaryUseCase.invoke(null)).thenReturn(Resource.Success(emptyList()))
        whenever(grossUseCase.invoke(null)).thenReturn(Resource.Success(emptyList()))
        
        viewModel.refreshAllData()
        
        dispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(false, viewModel.uiState.value.isRefreshing)
        assertEquals(false, viewModel.uiState.value.isSummaryRefreshing)
    }

    @Test
    fun `summary uses current gross row when endpoint returns oldest first`() = runTest {
        val currentLocalTime = java.time.LocalDate.now()
        val currentYear = currentLocalTime.year
        val currentMonthNumber = currentLocalTime.monthValue
        val indonesianMonths = listOf(
            "", "Januari", "Februari", "Maret", "April", "Mei", "Juni", 
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val currentMonthName = indonesianMonths[currentMonthNumber]
        val currentMonthString = "$currentMonthName $currentYear"

        whenever(grossUseCase.invoke(null)).thenReturn(
            Resource.Success(
                listOf(
                    GrossData(month = "Maret 2025", totalNominal = "1038150", orderCount = "35", tax = "5191"),
                    GrossData(month = currentMonthString, totalNominal = "3343000", orderCount = "115", tax = "16715"),
                    GrossData(month = "Desember 2999", totalNominal = "9999", orderCount = "9", tax = "99")
                )
            )
        )
        whenever(grossUseCase.invoke()).thenReturn(
            Resource.Success(
                listOf(
                    GrossData(month = "Maret 2025", totalNominal = "1038150", orderCount = "35", tax = "5191"),
                    GrossData(month = currentMonthString, totalNominal = "3343000", orderCount = "115", tax = "16715"),
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

    @Test
    fun `todayIncome error and empty branches`() = runTest {
        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val today = com.raylabs.laundryhub.shared.util.PlatformDate.getTodayDate("yyyy-MM-dd")
        val range = RangeDate(startDate = today, endDate = today)
        whenever(readIncomeUseCase.invoke(null, FILTER.RANGE_TRANSACTION_DATA, range)).thenReturn(Resource.Error("Failed to fetch"))
        viewModel.refreshAllData()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Failed to fetch", viewModel.uiState.value.todayIncome.errorMessage)

        whenever(readIncomeUseCase.invoke(null, FILTER.RANGE_TRANSACTION_DATA, range)).thenReturn(Resource.Empty)
        viewModel.refreshAllData()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.todayIncome.data.orEmpty().isEmpty())
    }

    @Test
    fun `summary error and gross reconstruction branches`() = runTest {
        val dummyGross = GrossData(id = 1, month = "Juni 2026", totalNominal = "50000", orderCount = "5", tax = "0")
        whenever(grossUseCase.invoke(null)).thenReturn(Resource.Success(listOf(dummyGross)))
        whenever(summaryUseCase.invoke(null)).thenReturn(Resource.Success(listOf(SpreadsheetData("key", "val"))))

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val initialGross = viewModel.uiState.value.summary.data?.single { it.title == "Gross Income" }
        assertEquals("Rp 50.000", initialGross?.body)
        assertEquals("5 order", initialGross?.footer)

        whenever(grossUseCase.invoke(null)).thenReturn(Resource.Error("Failed to fetch gross"))
        whenever(summaryUseCase.invoke(null)).thenReturn(Resource.Success(listOf(SpreadsheetData("key", "val"))))

        viewModel.refreshAllData()
        dispatcher.scheduler.advanceUntilIdle()

        val grossItem = viewModel.uiState.value.summary.data?.single { it.title == "Gross Income" }
        assertEquals("Rp 50.000", grossItem?.body)
        assertEquals("5 order", grossItem?.footer)

        whenever(summaryUseCase.invoke(null)).thenReturn(Resource.Error("Summary failed"))
        viewModel.refreshAllData()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Summary failed", viewModel.uiState.value.summary.errorMessage)
    }

    @Test
    fun `fetchReminderDiscovery with various candidate states`() = runTest {
        val fakeSettings = ReminderSettings(isReminderEnabled = true, notificationHour = 9, notificationMinute = 0)
        val fakeLocalStates = emptyMap<String, ReminderLocalState>()
        
        whenever(observeReminderSettingsUseCase.invoke()).thenReturn(flowOf(fakeSettings))
        whenever(observeReminderLocalStatesUseCase.invoke()).thenReturn(flowOf(fakeLocalStates))
        
        val fakeUnpaidOrder = TransactionData(
            orderID = "ORD-UNPAID",
            date = "2026-06-01",
            name = "John",
            weight = "2.0",
            pricePerKg = "10000",
            totalPrice = "20000",
            paymentStatus = "belum",
            packageType = "Regular",
            remark = "",
            paymentMethod = "cash",
            phoneNumber = "",
            dueDate = ""
        )
        whenever(readIncomeUseCase.invoke(null, FILTER.SHOW_UNPAID_DATA, null)).thenReturn(Resource.Success(listOf(fakeUnpaidOrder)))
        
        val candidate1 = ReminderCandidate(
            orderId = "ORD-UNPAID",
            customerName = "John",
            packageName = "Regular",
            paymentStatus = "belum",
            orderDate = "2026-06-01",
            dueDate = "",
            bucket = ReminderBucket.DUE_TODAY,
            overdueDays = 0
        )
        whenever(evaluateReminderCandidatesUseCase.invoke(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(listOf(candidate1))
        
        var viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(1, viewModel.uiState.value.reminderDiscovery?.eligibleCount)
        assertEquals("Open Reminder Inbox", viewModel.uiState.value.reminderDiscovery?.ctaLabel)
        assertTrue(viewModel.uiState.value.reminderDiscovery?.supportingText.orEmpty().contains("Open Reminder Inbox to review"))

        val candidate2 = ReminderCandidate(
            orderId = "ORD-UNPAID",
            customerName = "John",
            packageName = "Regular",
            paymentStatus = "belum",
            orderDate = "2026-06-01",
            dueDate = "",
            bucket = ReminderBucket.OVERDUE_3_TO_6_DAYS,
            overdueDays = 5
        )
        whenever(evaluateReminderCandidatesUseCase.invoke(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(listOf(candidate2))
        
        viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.reminderDiscovery?.supportingText.orEmpty().contains("5 days past the due date"))

        val fakeSettingsDisabled = fakeSettings.copy(isReminderEnabled = false)
        whenever(observeReminderSettingsUseCase.invoke()).thenReturn(flowOf(fakeSettingsDisabled))
        whenever(evaluateReminderCandidatesUseCase.invoke(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(listOf(candidate1))
        
        viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Turn on reminder", viewModel.uiState.value.reminderDiscovery?.ctaLabel)
        assertTrue(viewModel.uiState.value.reminderDiscovery?.supportingText.orEmpty().contains("Turn on reminders to review"))

        whenever(evaluateReminderCandidatesUseCase.invoke(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(listOf(candidate2))
        
        viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.reminderDiscovery?.supportingText.orEmpty().contains("Turn on reminders. The oldest one"))
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `retryOptimisticOrder handles success and error paths`() = runTest {
        var shouldSucceed = true
        val orderViewModel = mock<com.raylabs.laundryhub.ui.order.OrderViewModel>(defaultAnswer = org.mockito.stubbing.Answer { invocation ->
            if (invocation.method.name == "submitOrder") {
                val onComplete = invocation.arguments[1] as Function2<String, Any?, Any?>
                val onError = invocation.arguments[2] as Function2<String, Any?, Any?>
                val dummyContinuation = object : kotlin.coroutines.Continuation<Unit> {
                    override val context = kotlin.coroutines.EmptyCoroutineContext
                    override fun resumeWith(result: Result<Unit>) {}
                }
                if (shouldSucceed) {
                    onComplete.invoke("real-123", dummyContinuation)
                } else {
                    onError.invoke("Network failure", dummyContinuation)
                }
            }
            null
        })

        val viewModel = createViewModel()

        val sampleOrderData = OrderData(
            orderId = "fake-1",
            name = "Test",
            phoneNumber = "",
            packageName = "",
            priceKg = "",
            totalPrice = "",
            paidStatus = "",
            paymentMethod = "",
            remark = "",
            weight = "",
            orderDate = "",
            dueDate = ""
        )
        val dummyOrder = com.raylabs.laundryhub.ui.home.state.UnpaidOrderItem(
            orderID = "fake-1",
            customerName = "Test customer",
            packageType = "Regular",
            nowStatus = "Unpaid",
            dueDate = "",
            orderDate = "",
            syncStatus = com.raylabs.laundryhub.ui.home.state.SyncStatus.FAILED,
            rawPayload = sampleOrderData
        )

        viewModel.addOptimisticOrder(dummyOrder)

        var successId = ""
        shouldSucceed = true
        viewModel.retryOptimisticOrder(
            fakeId = "fake-1",
            orderViewModel = orderViewModel,
            scope = this,
            onSuccess = { successId = it },
            onError = {}
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("real-123", successId)

        var errorMsg = ""
        shouldSucceed = false
        viewModel.addOptimisticOrder(dummyOrder)
        viewModel.retryOptimisticOrder(
            fakeId = "fake-1",
            orderViewModel = orderViewModel,
            scope = this,
            onSuccess = {},
            onError = { errorMsg = it }
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Network failure", errorMsg)
    }

    @Test
    fun `TransactionData toUnpaidOrderItem mapping works correctly`() {
        val tx = TransactionData(
            orderID = "ORD-999",
            date = "2026-06-03",
            name = "Customer",
            weight = "2.5",
            pricePerKg = "10000",
            totalPrice = "25000",
            paymentStatus = "lunas",
            packageType = "Express",
            remark = "Fast",
            paymentMethod = "qris",
            phoneNumber = "0812",
            dueDate = "2026-06-04"
        )
        val unpaid = tx.toUnpaidOrderItem()
        assertEquals("ORD-999", unpaid.orderID)
        assertEquals("Customer", unpaid.customerName)
        assertEquals("Express", unpaid.packageType)
        assertEquals("Paid by QRIS", unpaid.nowStatus)
        assertEquals("2026-06-04", unpaid.dueDate)
        assertEquals("2026-06-03", unpaid.orderDate)
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
