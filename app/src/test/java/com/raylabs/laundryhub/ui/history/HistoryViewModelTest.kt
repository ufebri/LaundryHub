package com.raylabs.laundryhub.ui.history

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.DeleteOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.outcome.state.DateListItemUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val readIncomeUseCase: ReadIncomeTransactionUseCase = mock()
    private val deleteOrderUseCase: DeleteOrderUseCase = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        whenever(readIncomeUseCase.getPagingData(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(mock())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deleteOrderOptimistic updates state and hiddenOrderIds immediately and on success`() = runTest {
        whenever(deleteOrderUseCase.invoke(anyOrNull(), eq("ORD-1")))
            .thenReturn(Resource.Success(true))

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        var completed = false
        viewModel.deleteOrderOptimistic("ORD-1", onSuccess = { completed = true })
        
        // Assert optimistic behavior immediately (before advancing dispatcher clock)
        assertTrue(viewModel.uiState.hiddenOrderIds.contains("ORD-1"))

        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(completed)
        assertTrue(viewModel.uiState.hiddenOrderIds.contains("ORD-1"))
        assertEquals(true, viewModel.uiState.deleteOrder.data)
    }

    @Test
    fun `deleteOrderOptimistic rolls back hiddenOrderIds on error`() = runTest {
        whenever(deleteOrderUseCase.invoke(anyOrNull(), eq("ORD-1")))
            .thenReturn(Resource.Error("Database deletion failed"))

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        var failed = false
        viewModel.deleteOrderOptimistic("ORD-1", onError = { failed = true })
        
        // Assert optimistic hide is applied immediately
        assertTrue(viewModel.uiState.hiddenOrderIds.contains("ORD-1"))

        dispatcher.scheduler.advanceUntilIdle()

        // Assert rollback occurred (removed from hiddenOrderIds)
        assertFalse(viewModel.uiState.hiddenOrderIds.contains("ORD-1"))
        assertTrue(failed)
        assertEquals("Database deletion failed", viewModel.uiState.deleteOrder.errorMessage)
    }

    @Test
    fun `verify dummyHistoryUiState is initialized`() {
        org.junit.Assert.assertNotNull(com.raylabs.laundryhub.ui.common.dummy.history.dummyHistoryUiState)
    }

    @Test
    fun `historyPagingData maps and inserts separators`() = runTest {
        val dummyTransaction1 = TransactionData(
            orderID = "ORD-1",
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
        val dummyTransaction2 = TransactionData(
            orderID = "ORD-2",
            date = "2026-06-02",
            name = "Jane",
            weight = "3.0",
            pricePerKg = "10000",
            totalPrice = "30000",
            paymentStatus = "lunas",
            packageType = "Express",
            remark = "",
            paymentMethod = "qris",
            phoneNumber = "",
            dueDate = ""
        )

        whenever(readIncomeUseCase.getPagingData(com.raylabs.laundryhub.core.domain.model.sheets.FILTER.SHOW_ALL_DATA))
            .thenReturn(kotlinx.coroutines.flow.flowOf(androidx.paging.PagingData.from(listOf(dummyTransaction1, dummyTransaction2))))

        val viewModel = createViewModel()
        
        val diffCallback = object : androidx.recyclerview.widget.DiffUtil.ItemCallback<DateListItemUI>() {
            override fun areItemsTheSame(oldItem: DateListItemUI, newItem: DateListItemUI): Boolean = oldItem == newItem
            override fun areContentsTheSame(oldItem: DateListItemUI, newItem: DateListItemUI): Boolean = oldItem == newItem
        }
        val listUpdateCallback = object : androidx.recyclerview.widget.ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {}
            override fun onRemoved(position: Int, count: Int) {}
            override fun onMoved(fromPosition: Int, toPosition: Int) {}
            override fun onChanged(position: Int, count: Int, payload: Any?) {}
        }
        val differ = androidx.paging.AsyncPagingDataDiffer(
            diffCallback = diffCallback,
            updateCallback = listUpdateCallback,
            mainDispatcher = dispatcher,
            workerDispatcher = dispatcher
        )

        val job = launch {
            viewModel.historyPagingData.collect {
                differ.submitData(it)
            }
        }
        dispatcher.scheduler.advanceUntilIdle()
        job.cancel()

        val snapshot = differ.snapshot()
        assertTrue(snapshot.items.isNotEmpty())
        // Verify we have headers and entries
        assertTrue(snapshot.items.any { it is DateListItemUI.Header })
        assertTrue(snapshot.items.any { it is DateListItemUI.Entry })
    }

    @Test
    fun `deleteOrderOptimistic rolls back on Loading or unexpected Resource`() = runTest {
        whenever(deleteOrderUseCase.invoke(anyOrNull(), eq("ORD-1")))
            .thenReturn(Resource.Loading)

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteOrderOptimistic("ORD-1")
        
        assertTrue(viewModel.uiState.hiddenOrderIds.contains("ORD-1"))

        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.hiddenOrderIds.contains("ORD-1"))
    }

    private fun createViewModel(): HistoryViewModel {
        return HistoryViewModel(
            readIncomeUseCase = readIncomeUseCase,
            deleteOrderUseCase = deleteOrderUseCase
        )
    }
}
