package com.raylabs.laundryhub.ui.history


import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.DeleteOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.dummy.history.dummyHistoryItem
import com.raylabs.laundryhub.ui.common.dummy.history.dummyHistoryUiState
import com.raylabs.laundryhub.ui.history.state.HistoryUiState
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockUseCase: ReadIncomeTransactionUseCase = mock()
    private val mockDeleteUseCase: DeleteOrderUseCase = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init triggers fetchHistory and updates uiState with data`() = runTest {
        val transactions = dummyHistoryUiState.history.data
            ?.filterIsInstance<DateListItemUI.Entry>()
            ?.map {
                TransactionData(
                    orderID = it.item.id,
                    name = it.item.name,
                    date = it.item.date,
                    totalPrice = it.item.price,
                    packageType = it.item.remark,
                    paymentStatus = it.item.paymentStatus,
                    paymentMethod = "",
                    weight = "",
                    pricePerKg = "",
                    remark = "",
                    phoneNumber = "",
                    dueDate = ""
                )
            }.orEmpty()
        whenever(mockUseCase.invoke(filter = FILTER.SHOW_ALL_DATA))
            .thenReturn(Resource.Success(transactions))

        val vm = HistoryViewModel(mockUseCase, mockDeleteUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state: HistoryUiState = vm.uiState
        assertFalse(state.history.isLoading)
        assertNull(state.history.errorMessage)
        assertNotNull(state.history.data)
        assertTrue(state.history.data!!.any { it is DateListItemUI.Header })
        assertEquals(dummyHistoryUiState.history.data?.size, state.history.data?.size)
        val firstEntry = state.history.data!!.first { it is DateListItemUI.Entry } as DateListItemUI.Entry
        assertEquals(dummyHistoryItem.name, firstEntry.item.name)
    }

    @Test
    fun `uiState updated with error message on Resource_Error`() = runTest {
        whenever(mockUseCase.invoke(filter = FILTER.SHOW_ALL_DATA))
            .thenReturn(Resource.Error("network error"))

        val vm = HistoryViewModel(mockUseCase, mockDeleteUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState
        assertFalse(state.history.isLoading)
        assertEquals("network error", state.history.errorMessage)
    }

    @Test
    fun `uiState updated with empty message on Resource_Empty`() = runTest {
        whenever(mockUseCase.invoke(filter = FILTER.SHOW_ALL_DATA))
            .thenReturn(Resource.Empty)

        val vm = HistoryViewModel(mockUseCase, mockDeleteUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState
        assertFalse(state.history.isLoading)
        assertEquals("Data Kosong", state.history.errorMessage)
    }

    @Test
    fun `deleteOrder removes item locally on success`() = runTest {
        val transactions = listOf(
            TransactionData(
                orderID = "ORD-001",
                name = "Ny Emy",
                date = "02/06/2025",
                totalPrice = "Rp50.000",
                packageType = "Express - 24H",
                paymentStatus = "lunas",
                paymentMethod = "cash",
                weight = "1",
                pricePerKg = "50000",
                remark = "-",
                phoneNumber = "0812",
                dueDate = "03/06/2025"
            )
        )
        whenever(mockUseCase.invoke(filter = FILTER.SHOW_ALL_DATA))
            .thenReturn(Resource.Success(transactions))
        whenever(mockDeleteUseCase.invoke(onRetry = anyOrNull(), orderId = eq("ORD-001")))
            .thenReturn(Resource.Success(true))

        val vm = HistoryViewModel(mockUseCase, mockDeleteUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Initial check: has 1 entry + 1 header (from toUiItems)
        val initialData = vm.uiState.history.data.orEmpty()
        assertTrue(initialData.any { it is DateListItemUI.Entry && it.item.id == "ORD-001" })

        var completed = false
        vm.deleteOrder(orderId = "ORD-001", onComplete = { completed = true })
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(completed)
        assertEquals(true, vm.uiState.deleteOrder.data)
        
        // Verify it was NOT re-fetched (called only once in init)
        verify(mockUseCase, org.mockito.kotlin.times(1)).invoke(filter = FILTER.SHOW_ALL_DATA)
        
        // Verify local state mutation: ORD-001 should be gone
        val finalData = vm.uiState.history.data.orEmpty()
        assertFalse(finalData.any { it is DateListItemUI.Entry && it.item.id == "ORD-001" })
    }

    @Test
    fun `refreshHistory with isManual true updates isRefreshing flag`() = runTest {
        whenever(mockUseCase.invoke(filter = FILTER.SHOW_ALL_DATA))
            .thenReturn(Resource.Loading) // Keep it loading to check state

        val vm = HistoryViewModel(mockUseCase, mockDeleteUseCase)
        
        vm.refreshHistory(isManual = true)
        assertTrue(vm.uiState.isRefreshing)
        
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.isRefreshing)
    }

    @Test
    fun `refreshHistory with isManual false does not update isRefreshing flag`() = runTest {
        whenever(mockUseCase.invoke(filter = FILTER.SHOW_ALL_DATA))
            .thenReturn(Resource.Loading)

        val vm = HistoryViewModel(mockUseCase, mockDeleteUseCase)
        // advance until init call finishes its first part
        testDispatcher.scheduler.runCurrent()
        
        vm.refreshHistory(isManual = false)
        assertFalse(vm.uiState.isRefreshing)
    }

    @Test
    fun `deleteOrder stores error and forwards callback when delete fails`() = runTest {
        whenever(mockUseCase.invoke(filter = FILTER.SHOW_ALL_DATA))
            .thenReturn(Resource.Success(emptyList()))
        whenever(mockDeleteUseCase.invoke(onRetry = anyOrNull(), orderId = eq("ORD-404")))
            .thenReturn(Resource.Error("delete fail"))

        val vm = HistoryViewModel(mockUseCase, mockDeleteUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        var receivedError: String? = null
        vm.deleteOrder(
            orderId = "ORD-404",
            onComplete = {},
            onError = { receivedError = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("delete fail", receivedError)
        assertEquals("delete fail", vm.uiState.deleteOrder.errorMessage)
        assertFalse(vm.uiState.deleteOrder.isLoading)
    }
}
