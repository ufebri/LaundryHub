package com.raylabs.laundryhub.ui.reminder

import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.domain.usecase.reminder.FakeReminderRepository
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.SetDailyReminderNotificationEnabledUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.SetDailyReminderNotificationTimeUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.SetReminderEnabledUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderIntroViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeReminderRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
    fun `setReminderEnabled updates repository`() = runTest {
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
    }

    @Test
    fun `setDailyNotificationsEnabled updates repository`() = runTest {
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
    }

    @Test
    fun `setDailyNotificationTime updates repository`() = runTest {
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
    }

    private fun createViewModel(initialSettings: ReminderSettings): ReminderIntroViewModel {
        repository = FakeReminderRepository(initialSettings = initialSettings)
        return ReminderIntroViewModel(
            observeReminderSettingsUseCase = ObserveReminderSettingsUseCase(repository),
            setReminderEnabledUseCase = SetReminderEnabledUseCase(repository),
            setDailyReminderNotificationEnabledUseCase = SetDailyReminderNotificationEnabledUseCase(repository),
            setDailyReminderNotificationTimeUseCase = SetDailyReminderNotificationTimeUseCase(repository)
        )
    }
}
