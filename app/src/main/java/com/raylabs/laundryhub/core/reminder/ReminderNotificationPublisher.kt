package com.raylabs.laundryhub.core.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.MainActivity

object ReminderNotificationPublisher {

    fun showSummary(
        context: Context,
        title: String,
        message: String
    ) {
        ReminderNotificationConfig.ensureChannel(context)

        val notification = NotificationCompat.Builder(
            context,
            ReminderNotificationConfig.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_assignment)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(reminderInboxPendingIntent(context))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context)
            .notify(ReminderNotificationConfig.NOTIFICATION_ID, notification)
    }

    fun showTestNotification(context: Context) {
        showSummary(
            context = context,
            title = context.getString(R.string.reminder_test_notification_title),
            message = context.getString(R.string.reminder_test_notification_message)
        )
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context)
            .cancel(ReminderNotificationConfig.NOTIFICATION_ID)
    }

    private fun reminderInboxPendingIntent(context: Context): PendingIntent {
        return PendingIntent.getActivity(
            context,
            ReminderNotificationConfig.NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                putExtra(
                    ReminderNotificationConfig.EXTRA_DESTINATION,
                    ReminderNotificationConfig.DESTINATION_REMINDER_INBOX
                )
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
