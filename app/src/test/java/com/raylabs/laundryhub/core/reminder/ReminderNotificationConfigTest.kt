package com.raylabs.laundryhub.core.reminder

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.raylabs.laundryhub.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ReminderNotificationConfigTest {

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun `ensureChannel creates the reminder notification channel only once`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        ReminderNotificationConfig.ensureChannel(context)
        ReminderNotificationConfig.ensureChannel(context)

        val channel = manager.getNotificationChannel(ReminderNotificationConfig.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(ReminderNotificationConfig.CHANNEL_NAME, channel?.name?.toString())
        assertEquals(
            context.getString(R.string.reminder_notification_channel_description),
            channel?.description
        )
        assertEquals(1, shadowOf(manager).notificationChannels.size)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N_MR1])
    fun `ensureChannel is a no-op below android o`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        ReminderNotificationConfig.ensureChannel(context)

        assertNull(shadowOf(manager).notificationChannels.firstOrNull())
    }
}
