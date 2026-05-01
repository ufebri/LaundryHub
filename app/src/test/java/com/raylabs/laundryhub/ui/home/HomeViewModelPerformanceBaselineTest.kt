package com.raylabs.laundryhub.ui.home

import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.RangeDate
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.core.domain.usecase.reminder.EvaluateReminderCandidatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderLocalStatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadGrossDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.ReadSpreadsheetDataUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.core.domain.usecase.user.UserUseCase
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelPerformanceBaselineTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var slowRepository: SlowGoogleSheetRepository
    private lateinit var mockUserUseCase: UserUseCase
    private lateinit var mockObserveReminderSettingsUseCase: ObserveReminderSettingsUseCase
    private lateinit var mockObserveReminderLocalStatesUseCase: ObserveReminderLocalStatesUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        slowRepository = SlowGoogleSheetRepository(perCallDelayMs = 1_000L)
        mockUserUseCase = mock()
        mockObserveReminderSettingsUseCase = mock()
        mockObserveReminderLocalStatesUseCase = mock()

        whenever(mockUserUseCase.getCurrentUser()).thenReturn(
            User(uid = "perf-user", displayName = "Perf User", email = "perf@raylabs.com", urlPhoto = "")
        )
        whenever(mockObserveReminderSettingsUseCase.invoke()).thenReturn(flowOf(ReminderSettings()))
        whenever(mockObserveReminderLocalStatesUseCase.invoke()).thenReturn(flowOf(emptyMap()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshAllData baseline keeps fetch orchestration parallel`() = runTest(testDispatcher.scheduler) {
        val vm = createViewModel()

        advanceUntilIdle() // settle init work first

        val beforeSummaryCalls = slowRepository.summaryCalls
        val beforeGrossCalls = slowRepository.grossCalls
        val beforeTodayCalls = slowRepository.todayIncomeCalls
        val beforeUnpaidCalls = slowRepository.unpaidCalls
        val beforeAllCalls = slowRepository.allIncomeCalls

        val startedAt = currentTime
        vm.refreshAllData()

        assertTrue(vm.uiState.value.isRefreshing)

        advanceUntilIdle()

        val elapsedMs = currentTime - startedAt
        assertEquals(1_000L, elapsedMs)
        assertEquals(beforeSummaryCalls + 1, slowRepository.summaryCalls)
        assertEquals(beforeGrossCalls + 1, slowRepository.grossCalls)
        assertEquals(beforeTodayCalls + 1, slowRepository.todayIncomeCalls)
        assertEquals(beforeUnpaidCalls + 1, slowRepository.unpaidCalls)
        assertEquals(beforeAllCalls + 1, slowRepository.allIncomeCalls)
        assertFalse(vm.uiState.value.isRefreshing)

        println(
            "PERF_BASELINE owner=HomeViewModel method=refreshAllData " +
                "virtual_elapsed_ms=$elapsedMs per_call_delay_ms=${slowRepository.perCallDelayMs} " +
                "sequential_equivalent_ms=${slowRepository.perCallDelayMs * 5}"
        )
    }

    @Test
    fun `refreshAfterOrderChanged baseline keeps post-submit refresh parallel`() = runTest(testDispatcher.scheduler) {
        val vm = createViewModel()

        advanceUntilIdle() // settle init work first

        val beforeSummaryCalls = slowRepository.summaryCalls
        val beforeGrossCalls = slowRepository.grossCalls
        val beforeTodayCalls = slowRepository.todayIncomeCalls
        val beforeUnpaidCalls = slowRepository.unpaidCalls
        val beforeAllCalls = slowRepository.allIncomeCalls

        val startedAt = currentTime
        vm.refreshAfterOrderChanged()

        val elapsedMs = currentTime - startedAt
        assertEquals(1_000L, elapsedMs)
        assertEquals(beforeSummaryCalls + 1, slowRepository.summaryCalls)
        assertEquals(beforeGrossCalls + 1, slowRepository.grossCalls)
        assertEquals(beforeTodayCalls + 1, slowRepository.todayIncomeCalls)
        assertEquals(beforeUnpaidCalls + 1, slowRepository.unpaidCalls)
        assertEquals(beforeAllCalls + 1, slowRepository.allIncomeCalls)
        assertFalse(vm.uiState.value.isRefreshing)

        println(
            "PERF_BASELINE owner=HomeViewModel method=refreshAfterOrderChanged " +
                "virtual_elapsed_ms=$elapsedMs per_call_delay_ms=${slowRepository.perCallDelayMs} " +
                "sequential_equivalent_ms=${slowRepository.perCallDelayMs * 5}"
        )
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            summaryUseCase = ReadSpreadsheetDataUseCase(slowRepository),
            grossUseCase = ReadGrossDataUseCase(slowRepository),
            readIncomeUseCase = ReadIncomeTransactionUseCase(slowRepository),
            userUseCase = mockUserUseCase,
            observeReminderSettingsUseCase = mockObserveReminderSettingsUseCase,
            observeReminderLocalStatesUseCase = mockObserveReminderLocalStatesUseCase,
            evaluateReminderCandidatesUseCase = EvaluateReminderCandidatesUseCase()
        )
    }

    private class SlowGoogleSheetRepository(
        val perCallDelayMs: Long
    ) : GoogleSheetRepository {

        var summaryCalls: Int = 0
        var grossCalls: Int = 0
        var todayIncomeCalls: Int = 0
        var unpaidCalls: Int = 0
        var allIncomeCalls: Int = 0

        override suspend fun readSummaryTransaction(): Resource<List<SpreadsheetData>> {
            summaryCalls++
            delay(perCallDelayMs)
            return Resource.Success(listOf(SpreadsheetData("ReadyToPickup Status", "1")))
        }

        override suspend fun readGrossData(): Resource<List<GrossData>> {
            grossCalls++
            delay(perCallDelayMs)
            return Resource.Success(listOf(GrossData("march", "Rp7000", "1", "Rp0")))
        }

        override suspend fun readIncomeTransaction(
            filter: FILTER,
            rangeDate: RangeDate?
        ): Resource<List<TransactionData>> {
            delay(perCallDelayMs)
            return when (filter) {
                FILTER.TODAY_TRANSACTION_ONLY -> {
                    todayIncomeCalls++
                    Resource.Success(emptyList())
                }

                FILTER.SHOW_UNPAID_DATA -> {
                    unpaidCalls++
                    Resource.Success(emptyList())
                }

                FILTER.SHOW_ALL_DATA -> {
                    allIncomeCalls++
                    Resource.Success(emptyList())
                }

                else -> Resource.Success(emptyList())
            }
        }

        override suspend fun readPackageData(): Resource<List<PackageData>> = unsupported()
        override suspend fun addPackage(packageData: PackageData): Resource<Boolean> = unsupported()
        override suspend fun updatePackage(packageData: PackageData): Resource<Boolean> = unsupported()
        override suspend fun deletePackage(sheetRowIndex: Int): Resource<Boolean> = unsupported()
        override suspend fun readOtherPackage(): Resource<List<String>> = unsupported()
        override suspend fun getLastOrderId(): Resource<String> = unsupported()
        override suspend fun addOrder(order: OrderData): Resource<Boolean> = unsupported()
        override suspend fun getOrderById(orderId: String): Resource<TransactionData> = unsupported()
        override suspend fun updateOrder(order: OrderData): Resource<Boolean> = unsupported()
        override suspend fun deleteOrder(orderId: String): Resource<Boolean> = unsupported()
        override suspend fun readOutcomeTransaction(): Resource<List<OutcomeData>> = unsupported()
        override suspend fun addOutcome(outcome: OutcomeData): Resource<Boolean> = unsupported()
        override suspend fun getLastOutcomeId(): Resource<String> = unsupported()
        override suspend fun updateOutcome(outcome: OutcomeData): Resource<Boolean> = unsupported()
        override suspend fun getOutcomeById(outcomeId: String): Resource<OutcomeData> = unsupported()
        override suspend fun deleteOutcome(outcomeId: String): Resource<Boolean> = unsupported()

        @Suppress("UNCHECKED_CAST")
        private fun <T> unsupported(): T {
            throw UnsupportedOperationException("This baseline fake only supports Home refresh reads.")
        }
    }
}
