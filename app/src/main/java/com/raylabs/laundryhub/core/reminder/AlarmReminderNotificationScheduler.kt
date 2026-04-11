package com.raylabs.laundryhub.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmReminderNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderNotificationScheduler {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleDailySummary(hourOfDay: Int, minute: Int) {
        ReminderNotificationConfig.ensureChannel(context)
        val triggerAtMillis = nextTriggerAtMillis(hourOfDay, minute)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            AlarmManager.INTERVAL_DAY,
            reminderPendingIntent()
        )
    }

    override fun cancelDailySummary() {
        alarmManager.cancel(reminderPendingIntent())
    }

    private fun nextTriggerAtMillis(hourOfDay: Int, minute: Int): Long {
        return calculateNextReminderTriggerAtMillis(
            nowMillis = System.currentTimeMillis(),
            hourOfDay = hourOfDay,
            minute = minute
        )
    }

    private fun reminderPendingIntent(): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderNotificationConfig.ACTION_DAILY_SUMMARY
        }
        return PendingIntent.getBroadcast(
            context,
            ReminderNotificationConfig.NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

internal fun calculateNextReminderTriggerAtMillis(
    nowMillis: Long,
    hourOfDay: Int,
    minute: Int,
    timeZone: TimeZone = TimeZone.getDefault()
): Long {
    return Calendar.getInstance(timeZone).apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= nowMillis) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }.timeInMillis
}
