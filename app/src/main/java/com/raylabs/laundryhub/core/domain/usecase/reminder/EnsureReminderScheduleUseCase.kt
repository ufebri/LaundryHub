package com.raylabs.laundryhub.core.domain.usecase.reminder

import com.raylabs.laundryhub.core.reminder.ReminderNotificationScheduler
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class EnsureReminderScheduleUseCase @Inject constructor(
    private val observeReminderSettingsUseCase: ObserveReminderSettingsUseCase,
    private val reminderNotificationScheduler: ReminderNotificationScheduler
) {
    suspend operator fun invoke() {
        val settings = observeReminderSettingsUseCase().first()
        if (settings.isReminderEnabled && settings.isDailyNotificationEnabled) {
            reminderNotificationScheduler.scheduleDailySummary(
                settings.notificationHour,
                settings.notificationMinute
            )
        } else {
            reminderNotificationScheduler.cancelDailySummary()
        }
    }
}
