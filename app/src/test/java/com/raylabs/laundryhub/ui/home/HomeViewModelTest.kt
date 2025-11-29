package com.raylabs.laundryhub.ui.home

import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.ui.common.dummy.home.DUMMY_UNPAID_ORDER_ITEM_ARIFIN
import com.raylabs.laundryhub.ui.common.dummy.home.DUMMY_UNPAID_ORDER_ITEM_EMY
import com.raylabs.laundryhub.ui.common.dummy.home.DUMMY_UNPAID_ORDER_ITEM_GABRIEL
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.home.state.SortOption
import com.raylabs.laundryhub.ui.home.state.UnpaidOrderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val mockSummaryUseCase: ReadSpreadsheetDataUseCase = mock()
    private val mockReadIncomeUseCase: ReadIncomeTransactionUseCase = mock()
    private val mockUserUseCase: UserUseCase = mock()

    private val unpaidOrderItemsForSort = listOf(
        DUMMY_UNPAID_ORDER_ITEM_EMY,
        DUMMY_UNPAID_ORDER_ITEM_GABRIEL,
        DUMMY_UNPAID_ORDER_ITEM_ARIFIN
    )

    private fun List<UnpaidOrderItem>.toTransactionDataList(): List<TransactionData> =
        this.map {
            TransactionData(
                orderID = it.orderID,
                name = it.customerName,
                date = it.orderDate,
                totalPrice = "",
                packageType = it.packageType,
                paymentStatus = it.nowStatus, // pastikan "UNPAID" kalau filtermu case-sensitive
                paymentMethod = "",
                weight = "",
                pricePerKg = "",
                remark = "",
                phoneNumber = "",
                dueDate = it.dueDate
            )
        }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runTest { // runTest scope for doReturn on suspend functions
            whenever(mockUserUseCase.getCurrentUser()).thenReturn(
                User(
                    "defaultUser",
                    "Default",
                    "default@email.com",
                    "url"
                )
            )
            doReturn(Resource.Success(emptyList<TransactionData>())).whenever(mockReadIncomeUseCase)
                .invoke(filter = FILTER.TODAY_TRANSACTION_ONLY)
            doReturn(Resource.Success(emptyList<SpreadsheetData>())).whenever(mockSummaryUseCase)
                .invoke()
            doReturn(Resource.Success(emptyList<TransactionData>())).whenever(mockReadIncomeUseCase)
                .invoke(filter = FILTER.SHOW_UNPAID_DATA)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init fetches all sections and updates uiState`() = runTest {
        val testUser = User(
            uid = "1",
            displayName = "Raihan",
            email = "rai@labs.com",
            urlPhoto = "http://img.com/pp.jpg"
        )
        whenever(mockUserUseCase.getCurrentUser()).thenReturn(testUser)
        doReturn(Resource.Success(emptyList<TransactionData>())).whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.TODAY_TRANSACTION_ONLY)
        doReturn(Resource.Success(emptyList<SpreadsheetData>())).whenever(mockSummaryUseCase)
            .invoke()
        doReturn(Resource.Success(emptyList<TransactionData>())).whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.SHOW_UNPAID_DATA)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.user.data)
        assertEquals("Raihan", state.user.data?.displayName)
        assertNotNull(state.todayIncome.data)
        assertNotNull(state.summary.data)
        assertNotNull(state.unpaidOrder.data)
        assertFalse(state.todayIncome.isLoading)
        assertFalse(state.summary.isLoading)
        assertFalse(state.unpaidOrder.isLoading)
    }

    @Test
    fun `fetchTodayIncome updates state on success`() = runTest {
        val transactionList = listOf(
            TransactionData(
                orderID = "1",
                name = "A",
                date = "03/08/2024",
                totalPrice = "1000",
                packageType = "Reguler",
                paymentStatus = "PAID",
                paymentMethod = "Cash",
                weight = "1",
                pricePerKg = "1000",
                remark = "",
                phoneNumber = "08",
                dueDate = "05/08/2024"
            )
        )
        doReturn(Resource.Success(transactionList)).whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.TODAY_TRANSACTION_ONLY)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.todayIncome.data)
        assertEquals(1, state.todayIncome.data?.size)
        assertFalse(state.todayIncome.isLoading)
        assertNull(state.todayIncome.errorMessage)
    }

    @Test
    fun `fetchTodayIncome handles Resource Error`() = runTest {
        val errorMessage = "Today income error"
        doReturn(Resource.Error(errorMessage)).whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.TODAY_TRANSACTION_ONLY)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(errorMessage, state.todayIncome.errorMessage)
        assertFalse(state.todayIncome.isLoading)
        assertNull(state.todayIncome.data)
    }

    @Test
    fun `fetchTodayIncome handles Resource Empty`() = runTest {
        // GIVEN
        whenever(mockReadIncomeUseCase.invoke(filter = FILTER.TODAY_TRANSACTION_ONLY))
            .thenReturn(Resource.Empty) // atau Empty()
        whenever(mockReadIncomeUseCase.invoke(filter = FILTER.SHOW_UNPAID_DATA))
            .thenReturn(Resource.Empty)
        whenever(mockSummaryUseCase.invoke())
            .thenReturn(Resource.Empty)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        val state = vm.uiState.value
        assertFalse(state.todayIncome.isLoading)
        assertNull(state.todayIncome.errorMessage)
        assertNotNull(state.todayIncome.data)
        assertTrue((state.todayIncome.data as List<*>).isEmpty())
    }

    @Test
    fun `fetchSummary updates state on success`() = runTest {
        val summaryList = listOf(SpreadsheetData(key = "a", value = "b"))
        doReturn(Resource.Success(summaryList)).whenever(mockSummaryUseCase).invoke()

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.summary.data)
        assertEquals(1, state.summary.data?.size)
        assertFalse(state.summary.isLoading)
        assertNull(state.summary.errorMessage)
    }

    @Test
    fun `fetchOrder updates unpaidOrder on success`() = runTest {
        val transactionList = listOf(
            TransactionData(
                orderID = "1",
                name = "A",
                date = "03/08/2024",
                totalPrice = "1000",
                packageType = "Reguler",
                paymentStatus = "UNPAID",
                paymentMethod = "Cash",
                weight = "1",
                pricePerKg = "1000",
                remark = "",
                phoneNumber = "08",
                dueDate = "05/08/2024"
            )
        )
        doReturn(Resource.Success(transactionList)).whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.SHOW_UNPAID_DATA)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.unpaidOrder.data)
        assertEquals(1, state.unpaidOrder.data?.size)
        assertFalse(state.unpaidOrder.isLoading)
        assertNull(state.unpaidOrder.errorMessage)
    }

    @Test
    fun `init sets user to null when getCurrentUser returns null`() = runTest {
        whenever(mockUserUseCase.getCurrentUser()).thenReturn(null)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.user.data)
    }

    @Test
    fun `fetchSummary handles Resource Error`() = runTest {
        val errorMessage = "Summary fetch error"
        doReturn(Resource.Error(errorMessage)).whenever(mockSummaryUseCase).invoke()

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(errorMessage, state.summary.errorMessage)
        assertFalse(state.summary.isLoading)
        assertNull(state.summary.data)
    }

    @Test
    fun `fetchSummary handles Resource Empty`() = runTest {
        // GIVEN
        whenever(mockSummaryUseCase.invoke()).thenReturn(Resource.Empty) // atau Empty() jika class
        whenever(mockReadIncomeUseCase.invoke()).thenReturn(Resource.Empty)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        val state = vm.uiState.value
        assertFalse(state.summary.isLoading)
        assertNull(state.summary.errorMessage)
        assertNotNull(state.summary.data)
        assertTrue((state.summary.data as List<*>).isEmpty())
    }

    @Test
    fun `fetchOrder handles Resource Error`() = runTest {
        val errorMessage = "Order fetch error"
        doReturn(Resource.Error(errorMessage)).whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.SHOW_UNPAID_DATA)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(errorMessage, state.unpaidOrder.errorMessage)
        assertFalse(state.unpaidOrder.isLoading)
        assertNull(state.unpaidOrder.data)
    }

    @Test
    fun `fetchOrder handles Resource Empty and sets empty list to data`() = runTest {
        doReturn(Resource.Empty).whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.SHOW_UNPAID_DATA)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.unpaidOrder.data)
        assertTrue(state.unpaidOrder.data!!.isEmpty())
        assertFalse(state.unpaidOrder.isLoading)
        assertNull(state.unpaidOrder.errorMessage)
    }

    @Test
    fun `changeSortOrder updates sort option and sorts data by ORDER_DATE_ASC`() = runTest {
        doReturn(Resource.Success(unpaidOrderItemsForSort.toTransactionDataList())).whenever(
            mockReadIncomeUseCase
        ).invoke(filter = FILTER.SHOW_UNPAID_DATA)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialOrderUpdateKey = vm.uiState.value.orderUpdateKey
        vm.changeSortOrder(SortOption.ORDER_DATE_ASC)

        val state = vm.uiState.value
        assertEquals(SortOption.ORDER_DATE_ASC, state.currentSortOption)
        assertFalse(state.unpaidOrder.isLoading)
        assertNotNull(state.unpaidOrder.data)
        assertTrue(state.orderUpdateKey != initialOrderUpdateKey)

        val sortedData = state.unpaidOrder.data!!
        // Dates are unparsable, so ordering follows insertion (Emy, Gabriel, Arifin)
        assertEquals(DUMMY_UNPAID_ORDER_ITEM_EMY.customerName, sortedData[0].customerName)
        assertEquals(DUMMY_UNPAID_ORDER_ITEM_GABRIEL.customerName, sortedData[1].customerName)
        assertEquals(DUMMY_UNPAID_ORDER_ITEM_ARIFIN.customerName, sortedData[2].customerName)
    }

    @Test
    fun `changeSortOrder sorts data by DUE_DATE_ASC`() = runTest {
        doReturn(Resource.Success(unpaidOrderItemsForSort.toTransactionDataList())).whenever(
            mockReadIncomeUseCase
        ).invoke(filter = FILTER.SHOW_UNPAID_DATA)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.changeSortOrder(SortOption.DUE_DATE_ASC)

        val state = vm.uiState.value
        assertEquals(SortOption.DUE_DATE_ASC, state.currentSortOption)
        val sortedData = state.unpaidOrder.data!!
        assertEquals(DUMMY_UNPAID_ORDER_ITEM_EMY.customerName, sortedData[0].customerName)
        assertEquals(DUMMY_UNPAID_ORDER_ITEM_GABRIEL.customerName, sortedData[1].customerName)
        assertEquals(DUMMY_UNPAID_ORDER_ITEM_ARIFIN.customerName, sortedData[2].customerName)
    }

    @Test
    fun `changeSortOrder with empty list updates option and keeps list empty`() = runTest {
        doReturn(Resource.Success(emptyList<TransactionData>())).whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.SHOW_UNPAID_DATA)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.changeSortOrder(SortOption.DUE_DATE_DESC)

        val state = vm.uiState.value
        assertEquals(SortOption.DUE_DATE_DESC, state.currentSortOption)
        assertTrue(state.unpaidOrder.data!!.isEmpty())
        assertFalse(state.unpaidOrder.isLoading)
    }

    @Test
    fun `refreshAllData calls all fetch methods and updates refreshing state`() = runTest {
        val user = User("uid", "User", "email", "url")
        val todayIncomeData = listOf(
            TransactionData(
                "td1",
                "01/01/2024",
                "Income Name",
                "",
                "",
                "100",
                "",
                "",
                "",
                "",
                "",
                "02/01/2024"
            )
        )
        val summaryData = listOf(SpreadsheetData("Monthly Target", "v1"))
        val unpaidOrderData = listOf(
            TransactionData(
                "uo1",
                "03/01/2024",
                "Unpaid Name",
                "",
                "",
                "200",
                "",
                "",
                "",
                "",
                "",
                "04/01/2024"
            )
        )

        whenever(mockUserUseCase.getCurrentUser()).thenReturn(user)
        doReturn(Resource.Success(todayIncomeData)).whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.TODAY_TRANSACTION_ONLY)
        doReturn(Resource.Success(summaryData)).whenever(mockSummaryUseCase).invoke()
        doReturn(Resource.Success(unpaidOrderData)).whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.SHOW_UNPAID_DATA)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.refreshAllData()
        assertTrue(
            "isRefreshing should be true immediately after refreshAllData()",
            vm.uiState.value.isRefreshing
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse("isRefreshing should be false after all fetches complete", state.isRefreshing)
        assertNotNull(state.user.data)
        assertEquals(user.displayName, state.user.data?.displayName)

        assertNotNull(state.todayIncome.data)
        assertEquals(todayIncomeData.first().orderID, state.todayIncome.data?.first()?.id)

        assertNotNull(state.summary.data)
        assertEquals(summaryData.first().key, state.summary.data?.first()?.title)

        assertNotNull(state.unpaidOrder.data)
        assertEquals(unpaidOrderData.first().orderID, state.unpaidOrder.data?.first()?.orderID)
    }

    @Test
    fun `refreshAllData sets refreshing false even if a fetch fails`() = runTest {
        whenever(mockUserUseCase.getCurrentUser()).thenReturn(User("uid", "User", "email", "url"))
        doReturn(Resource.Success(emptyList<TransactionData>())).whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.TODAY_TRANSACTION_ONLY)
        doReturn(Resource.Error("Network Error during refresh")).whenever(mockSummaryUseCase)
            .invoke()
        doReturn(Resource.Success(emptyList<TransactionData>())).whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.SHOW_UNPAID_DATA)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.refreshAllData()
        assertTrue(vm.uiState.value.isRefreshing)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isRefreshing)
        assertEquals("Network Error during refresh", state.summary.errorMessage)
        assertNull(state.summary.data)
        assertNotNull(state.user.data)
        assertNotNull(state.todayIncome.data)
    }

    @Test
    fun `sort handles null or invalid dates with nullsFirst`() = runTest {
        val arr = listOf(
            UnpaidOrderItem("1", "A", "Reg", "Unpaid", "01/08/2024", "null"),
            UnpaidOrderItem("2", "B", "Reg", "Unpaid", "01/08/2024", "xx/yy/zzzz"),
            UnpaidOrderItem("3", "C", "Reg", "Unpaid", "01/08/2024", "02/08/2024"),
        )
        doReturn(Resource.Success(arr.toTransactionDataList()))
            .whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.SHOW_UNPAID_DATA)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.changeSortOrder(SortOption.DUE_DATE_ASC)
        val names = vm.uiState.value.unpaidOrder.data!!.map { it.customerName }
        // null & invalid (parsed null) di depan
        assertEquals(listOf("A", "B", "C"), names)
    }

    @Test
    fun `search is case-insensitive and updates list`() = runTest {
        val arr = listOf(
            UnpaidOrderItem("1", "Alice", "Reg", "Unpaid", "01/08/2024", "02/08/2024"),
            UnpaidOrderItem("2", "bob", "Reg", "Unpaid", "01/08/2024", "02/08/2024"),
            UnpaidOrderItem("3", "CHARLIE", "Reg", "Unpaid", "01/08/2024", "02/08/2024"),
        )
        doReturn(Resource.Success(arr.toTransactionDataList()))
            .whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.SHOW_UNPAID_DATA)

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onSearchQueryChanged("BO")
        val names = vm.uiState.value.unpaidOrder.data!!.map { it.customerName }
        assertEquals(listOf("bob"), names)
    }

    @Test
    fun `toggleSearch ON keeps current query and filtered result`() = runTest {
        // GIVEN: unpaid orders ada 3 item
        doReturn(Resource.Success(unpaidOrderItemsForSort.toTransactionDataList()))
            .whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.SHOW_UNPAID_DATA)
        doReturn(Resource.Success(emptyList<TransactionData>()))
            .whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.TODAY_TRANSACTION_ONLY)
        doReturn(Resource.Success(emptyList<SpreadsheetData>()))
            .whenever(mockSummaryUseCase).invoke()

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // WHEN: user mencari "emy" (match Ny Emy)
        vm.onSearchQueryChanged("emy")
        var names = vm.uiState.value.unpaidOrder.data!!.map { it.customerName }
        assertEquals(listOf(DUMMY_UNPAID_ORDER_ITEM_EMY.customerName), names)
        assertFalse(vm.uiState.value.isSearchActive) // default-nya false

        // AND: toggle sekali → ON, query tidak dihapus
        vm.toggleSearch()

        // THEN
        assertTrue(vm.uiState.value.isSearchActive)
        assertEquals("emy", vm.uiState.value.searchQuery)
        names = vm.uiState.value.unpaidOrder.data!!.map { it.customerName }
        assertEquals(listOf(DUMMY_UNPAID_ORDER_ITEM_EMY.customerName), names) // hasil filter tetap
        assertFalse(vm.uiState.value.unpaidOrder.isLoading)
    }

    @Test
    fun `toggleSearch OFF clears query and restores full list`() = runTest {
        // GIVEN
        doReturn(Resource.Success(unpaidOrderItemsForSort.toTransactionDataList()))
            .whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.SHOW_UNPAID_DATA)
        doReturn(Resource.Success(emptyList<TransactionData>()))
            .whenever(mockReadIncomeUseCase)
            .invoke(filter = FILTER.TODAY_TRANSACTION_ONLY)
        doReturn(Resource.Success(emptyList<SpreadsheetData>()))
            .whenever(mockSummaryUseCase).invoke()

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Set query & aktifkan search
        vm.onSearchQueryChanged("emy")
        assertEquals(
            listOf(DUMMY_UNPAID_ORDER_ITEM_EMY.customerName),
            vm.uiState.value.unpaidOrder.data!!.map { it.customerName })
        vm.toggleSearch() // ON dulu
        assertTrue(vm.uiState.value.isSearchActive)

        val beforeKey = vm.uiState.value.orderUpdateKey

        // WHEN: toggle lagi → OFF, harus clear query + update list
        vm.toggleSearch()

        // THEN
        val state = vm.uiState.value
        assertFalse(state.isSearchActive)
        assertEquals("", state.searchQuery)
        assertFalse(state.unpaidOrder.isLoading)
        assertTrue(state.orderUpdateKey != beforeKey)

        // Default sort = ORDER_DATE_DESC → 03/08 > 02/08 > 01/08
        val names = state.unpaidOrder.data!!.map { it.customerName }
        assertEquals(
            listOf(
                DUMMY_UNPAID_ORDER_ITEM_EMY.customerName,
                DUMMY_UNPAID_ORDER_ITEM_GABRIEL.customerName,
                DUMMY_UNPAID_ORDER_ITEM_ARIFIN.customerName
            ), names
        )
    }
}
