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

    private fun createViewModel(): HistoryViewModel {
        return HistoryViewModel(
            readIncomeUseCase = readIncomeUseCase,
            deleteOrderUseCase = deleteOrderUseCase
        )
    }
}
