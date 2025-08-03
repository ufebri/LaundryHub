package com.raylabs.laundryhub.ui.home

import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val mockSummaryUseCase: ReadSpreadsheetDataUseCase = mock()
    private val mockReadIncomeUseCase: ReadIncomeTransactionUseCase = mock()
    private val mockUserUseCase: UserUseCase = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init fetches all sections and updates uiState`() = runTest {
        // Mock user
        whenever(mockUserUseCase.getCurrentUser()).thenReturn(
            User(
                uid = "1",
                displayName = "Raihan",
                email = "rai@labs.com",
                urlPhoto = "http://img.com/pp.jpg"
            )
        )
        // Mock today income
        whenever(mockReadIncomeUseCase.invoke(filter = FILTER.TODAY_TRANSACTION_ONLY))
            .thenReturn(Resource.Success(emptyList()))
        // Mock summary
        whenever(mockSummaryUseCase.invoke()).thenReturn(Resource.Success(emptyList()))
        // Mock unpaid order
        whenever(mockReadIncomeUseCase.invoke(filter = FILTER.SHOW_UNPAID_DATA))
            .thenReturn(Resource.Success(emptyList()))

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
        whenever(mockReadIncomeUseCase.invoke(filter = FILTER.TODAY_TRANSACTION_ONLY))
            .thenReturn(
                Resource.Success(
                    listOf(
                        TransactionData(
                            orderID = "1",
                            name = "A",
                            date = "2024-08-03",
                            totalPrice = "1000",
                            packageType = "Reguler",
                            paymentStatus = "PAID",
                            paymentMethod = "Cash",
                            weight = "1",
                            pricePerKg = "1000",
                            remark = "",
                            phoneNumber = "08",
                            dueDate = "2024-08-05"
                        )
                    )
                )
            )

        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.fetchTodayIncome()
        runCurrent() // <-- tanpa lambda

        val state = vm.uiState.value
        assertNotNull(state.todayIncome.data)
        assertFalse(state.todayIncome.isLoading)
        assertNull(state.todayIncome.errorMessage)
    }

    @Test
    fun `fetchTodayIncome handles Resource Error`() = runTest {
        whenever(mockReadIncomeUseCase.invoke(filter = FILTER.TODAY_TRANSACTION_ONLY))
            .thenReturn(Resource.Error("error"))
        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.fetchTodayIncome()
        runCurrent()

        val state = vm.uiState.value
        assertEquals("error", state.todayIncome.errorMessage)
        assertFalse(state.todayIncome.isLoading)
    }

    @Test
    fun `fetchTodayIncome handles Resource Empty`() = runTest {
        whenever(mockReadIncomeUseCase.invoke(filter = FILTER.TODAY_TRANSACTION_ONLY))
            .thenReturn(Resource.Empty)
        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.fetchTodayIncome()
        runCurrent()

        val state = vm.uiState.value
        assertEquals("Tidak ada data hari ini", state.todayIncome.errorMessage)
        assertFalse(state.todayIncome.isLoading)
    }

    @Test
    fun `fetchSummary updates state on success`() = runTest {
        whenever(mockSummaryUseCase.invoke())
            .thenReturn(
                Resource.Success(
                    listOf(
                        SpreadsheetData(key = "a", value = "b")
                    )
                )
            )
        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.fetchSummary()
        runCurrent()

        val state = vm.uiState.value
        assertNotNull(state.summary.data)
        assertFalse(state.summary.isLoading)
        assertNull(state.summary.errorMessage)
    }

    @Test
    fun `fetchOrder updates unpaidOrder on success`() = runTest {
        whenever(mockReadIncomeUseCase.invoke(filter = FILTER.SHOW_UNPAID_DATA))
            .thenReturn(
                Resource.Success(
                    listOf(
                        TransactionData(
                            orderID = "1",
                            name = "A",
                            date = "2024-08-03",
                            totalPrice = "1000",
                            packageType = "Reguler",
                            paymentStatus = "UNPAID",
                            paymentMethod = "Cash",
                            weight = "1",
                            pricePerKg = "1000",
                            remark = "",
                            phoneNumber = "08",
                            dueDate = "2024-08-05"
                        )
                    )
                )
            )
        val vm = HomeViewModel(mockSummaryUseCase, mockReadIncomeUseCase, mockUserUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.fetchOrder()
        runCurrent()

        val state = vm.uiState.value
        assertNotNull(state.unpaidOrder.data)
        assertFalse(state.unpaidOrder.isLoading)
        assertNull(state.unpaidOrder.errorMessage)
    }
}