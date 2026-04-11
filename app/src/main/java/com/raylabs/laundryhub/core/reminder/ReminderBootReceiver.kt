package com.raylabs.laundryhub.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class ReminderBootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var observeReminderSettingsUseCase: ObserveReminderSettingsUseCase

    @Inject
    lateinit var reminderNotificationScheduler: ReminderNotificationScheduler

    override fun onReceive(context: Context, intent: Intent?) {
        if (!shouldHandleReminderBootBroadcast(intent?.action)) return
        runBlocking {
            applyReminderBootSchedule(
                settings = observeReminderSettingsUseCase().first(),
                reminderNotificationScheduler = reminderNotificationScheduler
            )
        }
    }
}

internal fun shouldHandleReminderBootBroadcast(action: String?): Boolean {
    return action == Intent.ACTION_BOOT_COMPLETED
}

internal fun applyReminderBootSchedule(
    settings: ReminderSettings,
    reminderNotificationScheduler: ReminderNotificationScheduler
) {
    if (settings.isReminderEnabled && settings.isDailyNotificationEnabled) {
        reminderNotificationScheduler.scheduleDailySummary(
            settings.notificationHour,
            settings.notificationMinute
        )
    } else {
        reminderNotificationScheduler.cancelDailySummary()
    }
}
