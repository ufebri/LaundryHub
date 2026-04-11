package com.raylabs.laundryhub.ui.reminder.state

import com.raylabs.laundryhub.core.domain.model.reminder.ReminderBucket
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderCandidate
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.ui.common.util.SectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderUiStateTest {

    @Test
    fun `toReminderSections groups candidates by bucket and sorts by bucket order`() {
        val sections = listOf(
            candidate(
                orderId = "three-days",
                bucket = ReminderBucket.OVERDUE_3_TO_6_DAYS,
                overdueDays = 4
            ),
            candidate(
                orderId = "due-today",
                bucket = ReminderBucket.DUE_TODAY,
                overdueDays = 0
            ),
            candidate(
                orderId = "two-days",
                bucket = ReminderBucket.OVERDUE_1_TO_2_DAYS,
                overdueDays = 2
            ),
            candidate(
                orderId = "another-three-days",
                bucket = ReminderBucket.OVERDUE_3_TO_6_DAYS,
                overdueDays = 5
            )
        ).toReminderSections()

        assertEquals(
            listOf(
                ReminderBucket.DUE_TODAY,
                ReminderBucket.OVERDUE_1_TO_2_DAYS,
                ReminderBucket.OVERDUE_3_TO_6_DAYS
            ),
            sections.map { it.bucket }
        )
        assertEquals("Due today", sections.first().title)
        assertEquals(listOf("three-days", "another-three-days"), sections.last().items.map { it.orderId })
        assertEquals("Due today, needs a cross-check", sections.first().items.first().overdueLabel)
        assertEquals(
            "Overdue 5 days from the due date",
            sections.last().items.last().overdueLabel
        )
    }

    @Test
    fun `bucket titles and overdue labels use the expected copy`() {
        val titles = ReminderBucket.entries.associateWith { it.title() }

        assertEquals("Due today", titles[ReminderBucket.DUE_TODAY])
        assertEquals("Overdue 1-2 days", titles[ReminderBucket.OVERDUE_1_TO_2_DAYS])
        assertEquals("Overdue 3-6 days", titles[ReminderBucket.OVERDUE_3_TO_6_DAYS])
        assertEquals("Overdue 1 week", titles[ReminderBucket.OVERDUE_1_WEEK])
        assertEquals("Overdue 2 weeks", titles[ReminderBucket.OVERDUE_2_WEEKS])
        assertEquals("Overdue 3 weeks", titles[ReminderBucket.OVERDUE_3_WEEKS])
        assertEquals("Overdue 1 month or more", titles[ReminderBucket.OVERDUE_1_MONTH_PLUS])
        assertEquals(
            "Overdue 14 days from the due date",
            ReminderBucket.OVERDUE_2_WEEKS.overdueLabel(14)
        )
    }

    @Test
    fun `ui state data classes keep the values passed to them`() {
        val inboxState = ReminderInboxUiState(
            reminderSettings = ReminderSettings(isReminderEnabled = true),
            reminders = SectionState(
                data = listOf(
                    ReminderSectionUi(
                        bucket = ReminderBucket.DUE_TODAY,
                        title = "Due today",
                        items = listOf(
                            ReminderItemUi(
                                orderId = "A-1",
                                customerName = "Ray",
                                packageName = "Regular",
                                paymentStatus = "Unpaid",
                                orderDate = "01/04/2026",
                                dueDate = "08/04/2026",
                                bucketLabel = "Due today",
                                overdueLabel = "Due today, needs a cross-check"
                            )
                        )
                    )
                )
            )
        )
        val introState = ReminderIntroUiState(
            reminderSettings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true
            )
        )

        assertTrue(inboxState.reminderSettings.isReminderEnabled)
        assertEquals("A-1", inboxState.reminders.data?.first()?.items?.first()?.orderId)
        assertTrue(introState.reminderSettings.isDailyNotificationEnabled)
    }

    private fun candidate(
        orderId: String,
        bucket: ReminderBucket,
        overdueDays: Int
    ): ReminderCandidate {
        return ReminderCandidate(
            orderId = orderId,
            customerName = "Customer $orderId",
            packageName = "Regular",
            paymentStatus = "Unpaid",
            orderDate = "01/04/2026",
            dueDate = "08/04/2026",
            bucket = bucket,
            overdueDays = overdueDays
        )
    }
}
