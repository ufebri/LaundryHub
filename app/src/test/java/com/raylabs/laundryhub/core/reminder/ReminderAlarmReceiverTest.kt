package com.raylabs.laundryhub.core.reminder

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderBucket
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderCandidate
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.shared.util.Resource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class ReminderAlarmReceiverTest {

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
    fun `shouldHandleReminderAlarmBroadcast only accepts daily summary with permission`() {
        assertTrue(
            shouldHandleReminderAlarmBroadcast(
                action = ReminderNotificationConfig.ACTION_DAILY_SUMMARY,
                hasNotificationPermission = true
            )
        )
        assertFalse(
            shouldHandleReminderAlarmBroadcast(
                action = Intent.ACTION_BOOT_COMPLETED,
                hasNotificationPermission = true
            )
        )
        assertFalse(
            shouldHandleReminderAlarmBroadcast(
                action = ReminderNotificationConfig.ACTION_DAILY_SUMMARY,
                hasNotificationPermission = false
            )
        )
    }

    @Test
    fun `onReceive ignores unrelated broadcasts`() {
        ReminderAlarmReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
    }

    @Test
    fun `dispatchReminderSummaryNotification cancels existing notification when reminder is disabled`() {
        ReminderNotificationPublisher.showSummary(context, title = "Old", message = "Message")

        dispatchReminderSummaryNotification(
            context = context,
            settings = ReminderSettings(
                isReminderEnabled = false,
                isDailyNotificationEnabled = true
            ),
            transactions = Resource.Success(emptyList()),
            localStates = emptyMap(),
            evaluateReminderCandidates = { _, _ -> emptyList() }
        )

        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
    }

    @Test
    fun `dispatchReminderSummaryNotification cancels notification when no reminder candidates can be produced`() {
        ReminderNotificationPublisher.showSummary(context, title = "Old", message = "Message")

        dispatchReminderSummaryNotification(
            context = context,
            settings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true
            ),
            transactions = Resource.Error("Unable to read transactions"),
            localStates = emptyMap(),
            evaluateReminderCandidates = { _, _ -> emptyList() }
        )

        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
    }

    @Test
    fun `dispatchReminderSummaryNotification shows summary notification for the highest priority reminder bucket`() {
        dispatchReminderSummaryNotification(
            context = context,
            settings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true
            ),
            transactions = Resource.Success(
                listOf(
                    TransactionData(
                        orderID = "ORD-1",
                        date = "01/04/2026",
                        name = "Alya",
                        totalPrice = "12000",
                        packageType = "Regular",
                        paymentStatus = "belum",
                        paymentMethod = "Cash",
                        weight = "1",
                        pricePerKg = "12000",
                        remark = "",
                        phoneNumber = "08123",
                        dueDate = "01/04/2026"
                    )
                )
            ),
            localStates = mapOf(
                "ORD-2" to ReminderLocalState(snoozedUntilEpochMillis = Long.MAX_VALUE)
            ),
            evaluateReminderCandidates = { _, _ ->
                listOf(
                    ReminderCandidate(
                        orderId = "ORD-1",
                        customerName = "Alya",
                        packageName = "Regular",
                        paymentStatus = "Belum lunas",
                        orderDate = "01/04/2026",
                        dueDate = "02/04/2026",
                        bucket = ReminderBucket.OVERDUE_1_WEEK,
                        overdueDays = 7
                    )
                )
            }
        )

        val notification = shadowOf(notificationManager).allNotifications.single()
        val expectedTitle = context.resources.getQuantityString(
            R.plurals.reminder_notification_title,
            1,
            1
        )
        val expectedMessage = context.getString(
            R.string.reminder_notification_message,
            context.getString(R.string.reminder_bucket_overdue_1_week)
        )

        assertEquals(expectedTitle, notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(expectedMessage, notification.extras.getString(Notification.EXTRA_TEXT))
    }

    @Test
    fun `onReceive with real dependencies exits without posting when reminder settings are disabled by default`() {
        ReminderAlarmReceiver().onReceive(
            context,
            Intent().apply { action = ReminderNotificationConfig.ACTION_DAILY_SUMMARY }
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
    }
}
