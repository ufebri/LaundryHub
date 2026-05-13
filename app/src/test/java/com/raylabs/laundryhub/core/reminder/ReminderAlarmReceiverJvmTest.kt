package com.raylabs.laundryhub.core.reminder

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderBucket
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderCandidate
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.shared.util.Resource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReminderAlarmReceiverJvmTest {

    private val context: Context = mock()
    private val resources: Resources = mock()

    @Before
    fun setUp() {
        whenever(context.resources).thenReturn(resources)
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
    fun `dispatchReminderSummaryNotification cancels when reminder feature is disabled`() {
        var cancelled = false
        var shown = false

        dispatchReminderSummaryNotification(
            context = context,
            settings = ReminderSettings(
                isReminderEnabled = false,
                isDailyNotificationEnabled = true
            ),
            transactions = Resource.Success(emptyList()),
            localStates = emptyMap(),
            evaluateReminderCandidates = { _, _ -> emptyList() },
            cancelNotification = { cancelled = true },
            showSummaryNotification = { _, _, _ -> shown = true }
        )

        assertTrue(cancelled)
        assertFalse(shown)
    }

    @Test
    fun `dispatchReminderSummaryNotification cancels when daily summary is disabled`() {
        var cancelled = false
        var shown = false

        dispatchReminderSummaryNotification(
            context = context,
            settings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = false
            ),
            transactions = Resource.Success(emptyList()),
            localStates = emptyMap(),
            evaluateReminderCandidates = { _, _ -> emptyList() },
            cancelNotification = { cancelled = true },
            showSummaryNotification = { _, _, _ -> shown = true }
        )

        assertTrue(cancelled)
        assertFalse(shown)
    }

    @Test
    fun `dispatchReminderSummaryNotification cancels when transactions cannot produce candidates`() {
        var cancelled = false
        var shown = false

        dispatchReminderSummaryNotification(
            context = context,
            settings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true
            ),
            transactions = Resource.Error("boom"),
            localStates = emptyMap(),
            evaluateReminderCandidates = { _, _ -> error("should not evaluate candidates") },
            cancelNotification = { cancelled = true },
            showSummaryNotification = { _, _, _ -> shown = true }
        )

        assertTrue(cancelled)
        assertFalse(shown)
    }

    @Test
    fun `dispatchReminderSummaryNotification cancels when evaluated candidates are empty`() {
        var cancelled = false
        var shown = false

        dispatchReminderSummaryNotification(
            context = context,
            settings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true
            ),
            transactions = Resource.Success(listOf(sampleTransaction())),
            localStates = emptyMap(),
            evaluateReminderCandidates = { _, _ -> emptyList() },
            cancelNotification = { cancelled = true },
            showSummaryNotification = { _, _, _ -> shown = true }
        )

        assertTrue(cancelled)
        assertFalse(shown)
    }

    @Test
    fun `dispatchReminderSummaryNotification shows summary message using highest priority bucket`() {
        whenever(context.getString(R.string.reminder_bucket_overdue_1_week))
            .thenReturn("1 week overdue")
        whenever(resources.getQuantityString(R.plurals.reminder_notification_title, 2, 2))
            .thenReturn("2 reminders ready")
        whenever(context.getString(R.string.reminder_notification_message, "1 week overdue"))
            .thenReturn("Check 1 week overdue")

        var capturedTitle: String? = null
        var capturedMessage: String? = null
        var cancelled = false

        dispatchReminderSummaryNotification(
            context = context,
            settings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true
            ),
            transactions = Resource.Success(listOf(sampleTransaction())),
            localStates = mapOf("other" to ReminderLocalState(dismissedAtEpochMillis = 10L)),
            evaluateReminderCandidates = { _, _ ->
                listOf(
                    sampleCandidate(
                        orderId = "ORD-1",
                        bucket = ReminderBucket.OVERDUE_1_WEEK
                    ),
                    sampleCandidate(
                        orderId = "ORD-2",
                        bucket = ReminderBucket.DUE_TODAY
                    )
                )
            },
            cancelNotification = { cancelled = true },
            showSummaryNotification = { _, title, message ->
                capturedTitle = title
                capturedMessage = message
            }
        )

        assertFalse(cancelled)
        assertEquals("2 reminders ready", capturedTitle)
        assertEquals("Check 1 week overdue", capturedMessage)
    }

    @Test
    fun `toNotificationLabel maps every bucket to its string resource`() {
        whenever(context.getString(R.string.reminder_bucket_due_today)).thenReturn("Due today")
        whenever(context.getString(R.string.reminder_bucket_overdue_1_2)).thenReturn("1-2 days")
        whenever(context.getString(R.string.reminder_bucket_overdue_3_6)).thenReturn("3-6 days")
        whenever(context.getString(R.string.reminder_bucket_overdue_1_week)).thenReturn("1 week")
        whenever(context.getString(R.string.reminder_bucket_overdue_2_weeks)).thenReturn("2 weeks")
        whenever(context.getString(R.string.reminder_bucket_overdue_3_weeks)).thenReturn("3 weeks")
        whenever(context.getString(R.string.reminder_bucket_overdue_1_month))
            .thenReturn("1 month+")

        assertEquals("Due today", ReminderBucket.DUE_TODAY.toNotificationLabel(context))
        assertEquals(
            "1-2 days",
            ReminderBucket.OVERDUE_1_TO_2_DAYS.toNotificationLabel(context)
        )
        assertEquals(
            "3-6 days",
            ReminderBucket.OVERDUE_3_TO_6_DAYS.toNotificationLabel(context)
        )
        assertEquals("1 week", ReminderBucket.OVERDUE_1_WEEK.toNotificationLabel(context))
        assertEquals("2 weeks", ReminderBucket.OVERDUE_2_WEEKS.toNotificationLabel(context))
        assertEquals("3 weeks", ReminderBucket.OVERDUE_3_WEEKS.toNotificationLabel(context))
        assertEquals(
            "1 month+",
            ReminderBucket.OVERDUE_1_MONTH_PLUS.toNotificationLabel(context)
        )
    }

    private fun sampleTransaction(): TransactionData {
        return TransactionData(
            orderID = "ORD-1",
            date = "01/04/2026",
            name = "Alya",
            weight = "1",
            pricePerKg = "12000",
            totalPrice = "12000",
            paymentStatus = "belum",
            packageType = "Regular",
            remark = "",
            paymentMethod = "cash",
            phoneNumber = "08123",
            dueDate = "02/04/2026"
        )
    }

    private fun sampleCandidate(orderId: String, bucket: ReminderBucket): ReminderCandidate {
        return ReminderCandidate(
            orderId = orderId,
            customerName = "Alya",
            packageName = "Regular",
            paymentStatus = "Belum lunas",
            orderDate = "01/04/2026",
            dueDate = "02/04/2026",
            bucket = bucket,
            overdueDays = 7
        )
    }
}
