package com.raylabs.laundryhub.core.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.raylabs.laundryhub.R

object ReminderNotificationConfig {
    const val CHANNEL_ID = "reminder_daily_summary"
    const val CHANNEL_NAME = "Reminder"
    const val NOTIFICATION_ID = 7011
    const val EXTRA_DESTINATION = "notification_destination"
    const val DESTINATION_REMINDER_INBOX = "reminder_inbox"
    const val ACTION_DAILY_SUMMARY = "com.raylabs.laundryhub.action.REMINDER_DAILY_SUMMARY"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.reminder_notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
