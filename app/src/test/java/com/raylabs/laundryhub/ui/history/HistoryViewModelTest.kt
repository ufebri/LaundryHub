package com.raylabs.laundryhub.ui.history


import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.ui.common.dummy.history.dummyHistoryItem
import com.raylabs.laundryhub.ui.common.dummy.history.dummyHistoryUiState
import com.raylabs.laundryhub.ui.common.util.Resource
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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

        val vm = HistoryViewModel(mockUseCase)
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
