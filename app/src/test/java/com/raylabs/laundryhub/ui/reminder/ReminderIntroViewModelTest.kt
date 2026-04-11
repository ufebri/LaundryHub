package com.raylabs.laundryhub.ui.reminder

import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.domain.usecase.reminder.FakeReminderRepository
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.SetDailyReminderNotificationEnabledUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.SetDailyReminderNotificationTimeUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.SetReminderEnabledUseCase
import com.raylabs.laundryhub.core.reminder.ReminderNotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderIntroViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeReminderRepository
    private lateinit var scheduler: ReminderNotificationScheduler

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        scheduler = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init observes reminder settings`() = runTest {
        val viewModel = createViewModel(
            initialSettings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = false,
                notificationHour = 10,
                notificationMinute = 30
            )
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.reminderSettings.isReminderEnabled)
        assertFalse(viewModel.uiState.value.reminderSettings.isDailyNotificationEnabled)
        assertTrue(viewModel.uiState.value.reminderSettings.notificationHour == 10)

        repository.updateSettings(
            ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true,
                notificationHour = 11,
                notificationMinute = 45
            )
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.reminderSettings.isDailyNotificationEnabled)
        assertTrue(viewModel.uiState.value.reminderSettings.notificationMinute == 45)
    }

    @Test
    fun `setReminderEnabled schedules daily summary when daily notifications are already on`() = runTest {
        val viewModel = createViewModel(
            initialSettings = ReminderSettings(
                isReminderEnabled = false,
                isDailyNotificationEnabled = true,
                notificationHour = 7,
                notificationMinute = 15
            )
        )
        advanceUntilIdle()

        viewModel.setReminderEnabled(true)
        advanceUntilIdle()

        assertTrue(repository.currentSettings().isReminderEnabled)
        verify(scheduler).scheduleDailySummary(7, 15)
        verifyNoMoreInteractions(scheduler)
    }

    @Test
    fun `setReminderEnabled false cancels daily summary`() = runTest {
        val viewModel = createViewModel(
            initialSettings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true
            )
        )
        advanceUntilIdle()

        viewModel.setReminderEnabled(false)
        advanceUntilIdle()

        assertFalse(repository.currentSettings().isReminderEnabled)
        verify(scheduler).cancelDailySummary()
        verifyNoMoreInteractions(scheduler)
    }

    @Test
    fun `setDailyNotificationsEnabled schedules when reminder is already enabled`() = runTest {
        val viewModel = createViewModel(
            initialSettings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = false,
                notificationHour = 6,
                notificationMinute = 20
            )
        )
        advanceUntilIdle()

        viewModel.setDailyNotificationsEnabled(true)
        advanceUntilIdle()

        assertTrue(repository.currentSettings().isDailyNotificationEnabled)
        verify(scheduler).scheduleDailySummary(6, 20)
        verifyNoMoreInteractions(scheduler)
    }

    @Test
    fun `setDailyNotificationsEnabled false cancels daily summary`() = runTest {
        val viewModel = createViewModel(
            initialSettings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true
            )
        )
        advanceUntilIdle()

        viewModel.setDailyNotificationsEnabled(false)
        advanceUntilIdle()

        assertFalse(repository.currentSettings().isDailyNotificationEnabled)
        verify(scheduler).cancelDailySummary()
        verifyNoMoreInteractions(scheduler)
    }

    @Test
    fun `setDailyNotificationTime updates repository and reschedules when reminder and notifications are enabled`() = runTest {
        val viewModel = createViewModel(
            initialSettings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true
            )
        )
        advanceUntilIdle()

        viewModel.setDailyNotificationTime(14, 5)
        advanceUntilIdle()

        assertTrue(repository.currentSettings().notificationHour == 14)
        assertTrue(repository.currentSettings().notificationMinute == 5)
        verify(scheduler).scheduleDailySummary(14, 5)
        verifyNoMoreInteractions(scheduler)
    }

    @Test
    fun `setDailyNotificationTime does not reschedule when daily notifications are off`() = runTest {
        val viewModel = createViewModel(
            initialSettings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = false
            )
        )
        advanceUntilIdle()

        viewModel.setDailyNotificationTime(14, 5)
        advanceUntilIdle()

        assertTrue(repository.currentSettings().notificationHour == 14)
        assertTrue(repository.currentSettings().notificationMinute == 5)
        verifyNoMoreInteractions(scheduler)
    }

    private fun createViewModel(initialSettings: ReminderSettings): ReminderIntroViewModel {
        repository = FakeReminderRepository(initialSettings = initialSettings)
        return ReminderIntroViewModel(
            observeReminderSettingsUseCase = ObserveReminderSettingsUseCase(repository),
            setReminderEnabledUseCase = SetReminderEnabledUseCase(repository),
            setDailyReminderNotificationEnabledUseCase = SetDailyReminderNotificationEnabledUseCase(repository),
            setDailyReminderNotificationTimeUseCase = SetDailyReminderNotificationTimeUseCase(repository),
            reminderNotificationScheduler = scheduler
        )
    }
}
