package com.raylabs.laundryhub.core.domain.usecase.reminder

import com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.reminder.ReminderNotificationScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

class ReminderUseCasesTest {

    @Test
    fun `observe use cases expose repository flows`() = runTest {
        val localStates = mapOf(
            "A-1" to ReminderLocalState(snoozedUntilEpochMillis = 1_000L)
        )
        val repository = FakeReminderRepository(
            initialSettings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true,
                notificationHour = 7,
                notificationMinute = 30
            ),
            initialLocalStates = localStates
        )

        val settings = ObserveReminderSettingsUseCase(repository).invoke().first()
        val observedLocalStates = ObserveReminderLocalStatesUseCase(repository).invoke().first()

        assertEquals(
            ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true,
                notificationHour = 7,
                notificationMinute = 30
            ),
            settings
        )
        assertEquals(localStates, observedLocalStates)
    }

    @Test
    fun `delegate use cases update repository state and preserve timestamps`() = runTest {
        val repository = FakeReminderRepository()

        SetReminderEnabledUseCase(repository).invoke(true)
        SetDailyReminderNotificationEnabledUseCase(repository).invoke(true)
        SetDailyReminderNotificationTimeUseCase(repository).invoke(6, 45)
        MarkReminderCheckedUseCase(repository).invoke(orderId = "A-1", timestampMillis = 11L)
        MarkReminderAssumedPickedUpUseCase(repository).invoke(orderId = "A-2", timestampMillis = 22L)
        DismissReminderUseCase(repository).invoke(orderId = "A-3", timestampMillis = 33L)
        SnoozeReminderUseCase(repository).invoke(orderId = "A-4", untilEpochMillis = 44L)

        assertEquals(
            ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true,
                notificationHour = 6,
                notificationMinute = 45
            ),
            repository.currentSettings()
        )
        assertEquals(ReminderLocalState(checkedAtEpochMillis = 11L), repository.currentLocalStates()["A-1"])
        assertEquals(
            ReminderLocalState(assumedPickedUpAtEpochMillis = 22L),
            repository.currentLocalStates()["A-2"]
        )
        assertEquals(
            ReminderLocalState(dismissedAtEpochMillis = 33L),
            repository.currentLocalStates()["A-3"]
        )
        assertEquals(
            ReminderLocalState(snoozedUntilEpochMillis = 44L),
            repository.currentLocalStates()["A-4"]
        )
        assertEquals(listOf(true), repository.setReminderEnabledCalls)
        assertEquals(listOf(true), repository.setDailyNotificationEnabledCalls)
        assertEquals(listOf(6 to 45), repository.setDailyNotificationTimeCalls)
        assertEquals(listOf("A-1" to 11L), repository.markCheckedCalls)
        assertEquals(listOf("A-2" to 22L), repository.markAssumedPickedUpCalls)
        assertEquals(listOf("A-3" to 33L), repository.dismissCalls)
        assertEquals(listOf("A-4" to 44L), repository.snoozeCalls)
    }

    @Test
    fun `ensure reminder schedule schedules when both reminder settings are enabled`() = runTest {
        val scheduler: ReminderNotificationScheduler = mock()
        val repository = FakeReminderRepository(
            initialSettings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true,
                notificationHour = 8,
                notificationMinute = 15
            )
        )

        EnsureReminderScheduleUseCase(
            observeReminderSettingsUseCase = ObserveReminderSettingsUseCase(repository),
            reminderNotificationScheduler = scheduler
        ).invoke()

        verify(scheduler).scheduleDailySummary(8, 15)
        verifyNoMoreInteractions(scheduler)
    }

    @Test
    fun `ensure reminder schedule cancels when one of the settings is disabled`() = runTest {
        val scheduler: ReminderNotificationScheduler = mock()
        val repository = FakeReminderRepository(
            initialSettings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = false
            )
        )

        EnsureReminderScheduleUseCase(
            observeReminderSettingsUseCase = ObserveReminderSettingsUseCase(repository),
            reminderNotificationScheduler = scheduler
        ).invoke()

        verify(scheduler).cancelDailySummary()
        verifyNoMoreInteractions(scheduler)
    }

    @Test
    fun `resolved local state reports as resolved`() {
        assertTrue(ReminderLocalState(checkedAtEpochMillis = 10L).isResolved)
        assertTrue(ReminderLocalState(assumedPickedUpAtEpochMillis = 10L).isResolved)
        assertTrue(ReminderLocalState(dismissedAtEpochMillis = 10L).isResolved)
    }
}
