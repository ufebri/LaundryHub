package com.raylabs.laundryhub.ui.history

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.DeleteOrderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.shared.util.Resource
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
    fun `init loads history`() = runTest {
        val sample = TransactionData(
            orderID = "1",
            name = "Test",
            date = "10/05/2026",
            totalPrice = "1000",
            paymentStatus = "Paid",
            weight = "1",
            pricePerKg = "1000",
            packageType = "Regular",
            remark = "",
            paymentMethod = "Cash",
            phoneNumber = "123",
            dueDate = "10/05/2026"
        )
        whenever(readIncomeUseCase.invoke(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(Resource.Success(listOf(sample)))

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        // Size 2 because toUiItems() returns [Header, Entry]
        assertEquals(2, viewModel.uiState.history.data?.size)
        val entry = (viewModel.uiState.history.data?.get(1) as? DateListItemUI.Entry)?.item
        assertEquals("Test", entry?.name)
    }

    @Test
    fun `deleteOrder updates state and hiddenOrderIds`() = runTest {
        whenever(readIncomeUseCase.invoke(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(Resource.Success(emptyList()))
        whenever(deleteOrderUseCase.invoke(anyOrNull(), eq("ORD-1")))
            .thenReturn(Resource.Success(true))

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        var completed = false
        viewModel.deleteOrder("ORD-1", onComplete = { completed = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(completed)
        assertTrue(viewModel.uiState.hiddenOrderIds.contains("ORD-1"))
        assertEquals(true, viewModel.uiState.deleteOrder.data)
    }

    @Test
    fun `loadHistory sets error message when Empty`() = runTest {
        whenever(readIncomeUseCase.invoke(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(Resource.Empty)

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Data Kosong", viewModel.uiState.history.errorMessage)
        assertFalse(viewModel.uiState.history.isLoading)
    }

    private fun createViewModel(): HistoryViewModel {
        return HistoryViewModel(
            readIncomeUseCase = readIncomeUseCase,
            deleteOrderUseCase = deleteOrderUseCase
        )
    }
}
