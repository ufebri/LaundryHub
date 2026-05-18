package com.raylabs.laundryhub.core.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LaundryHubFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var deviceTokenManager: DeviceTokenManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        deviceTokenManager.sendTokenToBackend(token, refreshBackendConfig = true)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val type = data["type"]
        val title = remoteMessage.notification?.title ?: data["title"] ?: "LaundryHub"
        val body = remoteMessage.notification?.body ?: data["body"] ?: ""

        when (type) {
            "SYNC_SUCCESS" -> {
                showNotification("sync_channel", "Sync Notifications", title, body)
            }
            "REMINDER" -> {
                showNotification("reminder_channel", "Reminders", title, body, "laundryhub://reminder_inbox")
            }
            else -> {
                // Default handling
                showNotification("default_channel", "General", title, body)
            }
        }
    }

    private fun showNotification(channelId: String, channelName: String, title: String, body: String, deepLink: String? = null) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (deepLink != null) {
                data = Uri.parse(deepLink)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_branding) // Or your actual notification icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
