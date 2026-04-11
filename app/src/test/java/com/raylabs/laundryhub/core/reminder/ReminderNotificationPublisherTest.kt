package com.raylabs.laundryhub.core.reminder

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.MainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class ReminderNotificationPublisherTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    @After
    fun tearDown() {
        notificationManager.cancelAll()
    }

    @Test
    fun `showSummary posts a notification that deep links into reminder inbox`() {
        ReminderNotificationPublisher.showSummary(
            context = context,
            title = "1 order needs review",
            message = "Open Reminder Inbox"
        )

        val notification = shadowOf(notificationManager).allNotifications.single()
        val launchedIntent = shadowOf(notification.contentIntent).savedIntent

        assertEquals("1 order needs review", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Open Reminder Inbox", notification.extras.getString(Notification.EXTRA_TEXT))
        assertNotNull(notification.contentIntent)
        assertEquals(MainActivity::class.java.name, launchedIntent.component?.className)
        assertEquals(
            ReminderNotificationConfig.DESTINATION_REMINDER_INBOX,
            launchedIntent.getStringExtra(ReminderNotificationConfig.EXTRA_DESTINATION)
        )
        assertTrue(launchedIntent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertTrue(launchedIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
    }

    @Test
    fun `showTestNotification uses reminder test copy`() {
        ReminderNotificationPublisher.showTestNotification(context)

        val notification = shadowOf(notificationManager).allNotifications.single()

        assertEquals(
            context.getString(R.string.reminder_test_notification_title),
            notification.extras.getString(Notification.EXTRA_TITLE)
        )
        assertEquals(
            context.getString(R.string.reminder_test_notification_message),
            notification.extras.getString(Notification.EXTRA_TEXT)
        )
    }

    @Test
    fun `cancel removes the posted reminder notification`() {
        ReminderNotificationPublisher.showSummary(context, title = "Old", message = "Message")

        ReminderNotificationPublisher.cancel(context)

        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
    }
}
