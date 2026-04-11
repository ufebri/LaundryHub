package com.raylabs.laundryhub.ui.reminder

import com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.usecase.reminder.DismissReminderUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.EvaluateReminderCandidatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.FakeReminderRepository
import com.raylabs.laundryhub.core.domain.usecase.reminder.MarkReminderAssumedPickedUpUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.MarkReminderCheckedUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderLocalStatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.SnoozeReminderUseCase
import com.raylabs.laundryhub.core.domain.usecase.sheets.income.ReadIncomeTransactionUseCase
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeReminderRepository
    private lateinit var readIncomeTransactionUseCase: ReadIncomeTransactionUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        readIncomeTransactionUseCase = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads reminder settings and visible reminder sections`() = runTest {
        stubReadIncome(
            Resource.Success(
                listOf(
                    overdueTransaction(orderId = "A-1"),
                    futureTransaction(orderId = "A-2")
                )
            )
        )

        val viewModel = createViewModel(
            initialSettings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.reminderSettings.isReminderEnabled)
        assertTrue(state.reminderSettings.isDailyNotificationEnabled)
        assertFalse(state.reminders.isLoading)
        assertEquals(1, state.reminders.data?.sumOf { it.items.size })
        assertEquals("A-1", state.reminders.data?.first()?.items?.first()?.orderId)
        assertEquals("Overdue 1 month or more", state.reminders.data?.first()?.title)
    }

    @Test
    fun `refresh keeps loading state when repository reports loading`() = runTest {
        stubReadIncome(Resource.Empty)
        val viewModel = createViewModel()
        advanceUntilIdle()

        stubReadIncome(Resource.Loading)
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.reminders.isLoading)
        assertTrue(viewModel.uiState.value.reminders.data?.isEmpty() == true)
    }

    @Test
    fun `refresh exposes empty list when repository returns empty`() = runTest {
        stubReadIncome(Resource.Empty)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.reminders.isLoading)
        assertTrue(viewModel.uiState.value.reminders.data?.isEmpty() == true)
    }

    @Test
    fun `init keeps inbox in loading state until the first transaction fetch completes`() = runTest {
        stubReadIncome(Resource.Loading)

        val viewModel = createViewModel(
            initialSettings = ReminderSettings(isReminderEnabled = true)
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.reminders.isLoading)
        assertTrue(viewModel.uiState.value.reminders.data == null)
    }

    @Test
    fun `refresh exposes error state when repository returns an error`() = runTest {
        stubReadIncome(Resource.Error("unable to read transactions"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("unable to read transactions", viewModel.uiState.value.reminders.errorMessage)
        assertFalse(viewModel.uiState.value.reminders.isLoading)
    }

    @Test
    fun `markChecked updates repository state and removes the reminder item`() = runTest {
        stubReadIncome(Resource.Success(listOf(overdueTransaction(orderId = "A-1"))))

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.reminders.data?.first()?.items?.size)

        viewModel.markChecked("A-1")
        advanceUntilIdle()

        assertEquals(1, repository.markCheckedCalls.size)
        assertEquals("A-1", repository.markCheckedCalls.single().first)
        assertNotNull(repository.currentLocalStates()["A-1"]?.checkedAtEpochMillis)
        assertTrue(viewModel.uiState.value.reminders.data?.isEmpty() == true)
    }

    @Test
    fun `markAssumedPickedUp and dismiss both resolve the reminder item`() = runTest {
        stubReadIncome(Resource.Success(listOf(overdueTransaction(orderId = "A-1"))))

        val pickedUpViewModel = createViewModel()
        advanceUntilIdle()
        pickedUpViewModel.markAssumedPickedUp("A-1")
        advanceUntilIdle()

        assertNotNull(repository.currentLocalStates()["A-1"]?.assumedPickedUpAtEpochMillis)
        assertTrue(pickedUpViewModel.uiState.value.reminders.data?.isEmpty() == true)

        stubReadIncome(Resource.Success(listOf(overdueTransaction(orderId = "B-1"))))
        val dismissedViewModel = createViewModel()
        advanceUntilIdle()
        dismissedViewModel.dismiss("B-1")
        advanceUntilIdle()

        assertNotNull(repository.currentLocalStates()["B-1"]?.dismissedAtEpochMillis)
        assertTrue(dismissedViewModel.uiState.value.reminders.data?.isEmpty() == true)
    }

    @Test
    fun `snooze hides the reminder item until the snooze window ends`() = runTest {
        stubReadIncome(Resource.Success(listOf(overdueTransaction(orderId = "A-1"))))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.snooze("A-1")
        advanceUntilIdle()

        val snoozedState = repository.currentLocalStates()["A-1"]
        assertNotNull(snoozedState?.snoozedUntilEpochMillis)
        assertTrue(viewModel.uiState.value.reminders.data?.isEmpty() == true)
    }

    @Test
    fun `incoming local state changes recompute the visible reminder list`() = runTest {
        stubReadIncome(Resource.Success(listOf(overdueTransaction(orderId = "A-1"))))

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.reminders.data?.sumOf { it.items.size })

        repository.updateSettings(
            ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true
            )
        )
        repository.snooze(orderId = "A-1", untilEpochMillis = System.currentTimeMillis() + 86_400_000L)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.reminders.data?.isEmpty() == true)
    }

    private fun createViewModel(
        initialSettings: ReminderSettings = ReminderSettings(),
        initialLocalStates: Map<String, ReminderLocalState> = emptyMap()
    ): ReminderViewModel {
        repository = FakeReminderRepository(
            initialSettings = initialSettings,
            initialLocalStates = initialLocalStates
        )
        return ReminderViewModel(
            readIncomeTransactionUseCase = readIncomeTransactionUseCase,
            observeReminderSettingsUseCase = ObserveReminderSettingsUseCase(repository),
            observeReminderLocalStatesUseCase = ObserveReminderLocalStatesUseCase(repository),
            evaluateReminderCandidatesUseCase = EvaluateReminderCandidatesUseCase(),
            markReminderCheckedUseCase = MarkReminderCheckedUseCase(repository),
            markReminderAssumedPickedUpUseCase = MarkReminderAssumedPickedUpUseCase(repository),
            dismissReminderUseCase = DismissReminderUseCase(repository),
            snoozeReminderUseCase = SnoozeReminderUseCase(repository)
        )
    }

    private suspend fun stubReadIncome(result: Resource<List<TransactionData>>) {
        doReturn(result).whenever(readIncomeTransactionUseCase)
            .invoke(filter = FILTER.SHOW_ALL_DATA)
    }

    private fun overdueTransaction(orderId: String): TransactionData {
        return TransactionData(
            orderID = orderId,
            date = "01/04/2026",
            name = "Customer $orderId",
            weight = "2",
            pricePerKg = "12000",
            totalPrice = "24000",
            paymentStatus = "belum",
            packageType = "Regular",
            remark = "",
            paymentMethod = "cash",
            phoneNumber = "08123",
            dueDate = "01/01/2020"
        )
    }

    private fun futureTransaction(orderId: String): TransactionData {
        return TransactionData(
            orderID = orderId,
            date = "01/04/2026",
            name = "Future $orderId",
            weight = "2",
            pricePerKg = "12000",
            totalPrice = "24000",
            paymentStatus = "belum",
            packageType = "Regular",
            remark = "",
            paymentMethod = "cash",
            phoneNumber = "08123",
            dueDate = "01/01/2099"
        )
    }
}
