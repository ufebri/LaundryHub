package com.raylabs.laundryhub.ui.history


import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.history.state.HistoryUiItem
import com.raylabs.laundryhub.ui.history.state.HistoryUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockUseCase: ReadIncomeTransactionUseCase = mock()

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
        val transactions = listOf(
            TransactionData(orderID = "1", name = "Raihan", date = "2024-08-03", totalPrice = "2000", packageType = "Express", paymentStatus = "PAID", paymentMethod = "Cash", weight = "2", pricePerKg = "1000", remark = "", phoneNumber = "0812", dueDate = "2024-08-04"),
            TransactionData(orderID = "2", name = "Agus", date = "2024-08-03", totalPrice = "3000", packageType = "Reguler", paymentStatus = "UNPAID", paymentMethod = "QR", weight = "3", pricePerKg = "1000", remark = "", phoneNumber = "0813", dueDate = "2024-08-05")
        )
        whenever(mockUseCase.invoke(filter = FILTER.SHOW_ALL_DATA))
            .thenReturn(Resource.Success(transactions))

        val vm = HistoryViewModel(mockUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state: HistoryUiState = vm.uiState
        assertFalse(state.history.isLoading)
        assertNull(state.history.errorMessage)
        assertNotNull(state.history.data)
        assertTrue(state.history.data!!.any { it is HistoryUiItem.Header })
        assertTrue(state.history.data!!.any { it is HistoryUiItem.Entry })
    }

    @Test
    fun `uiState updated with error message on Resource_Error`() = runTest {
        whenever(mockUseCase.invoke(filter = FILTER.SHOW_ALL_DATA))
            .thenReturn(Resource.Error("network error"))

        val vm = HistoryViewModel(mockUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState
        assertFalse(state.history.isLoading)
        assertEquals("network error", state.history.errorMessage)
    }

    @Test
    fun `uiState updated with empty message on Resource_Empty`() = runTest {
        whenever(mockUseCase.invoke(filter = FILTER.SHOW_ALL_DATA))
            .thenReturn(Resource.Empty)

        val vm = HistoryViewModel(mockUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState
        assertFalse(state.history.isLoading)
        assertEquals("Data Kosong", state.history.errorMessage)
    }
}