package com.raylabs.laundryhub.ui.reminder.state

import com.raylabs.laundryhub.core.domain.model.reminder.ReminderBucket
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderCandidate
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.ui.common.util.SectionState

data class ReminderInboxUiState(
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val reminders: SectionState<List<ReminderSectionUi>> = SectionState(isLoading = true)
)

data class ReminderIntroUiState(
    val reminderSettings: ReminderSettings = ReminderSettings()
)

data class ReminderSectionUi(
    val bucket: ReminderBucket,
    val title: String,
    val items: List<ReminderItemUi>
)

data class ReminderItemUi(
    val orderId: String,
    val customerName: String,
    val packageName: String,
    val paymentStatus: String,
    val orderDate: String,
    val dueDate: String,
    val bucketLabel: String,
    val overdueLabel: String
)

fun List<ReminderCandidate>.toReminderSections(): List<ReminderSectionUi> {
    return groupBy { it.bucket }
        .entries
        .sortedBy { it.key.ordinal }
        .map { (bucket, items) ->
            ReminderSectionUi(
                bucket = bucket,
                title = bucket.title(),
                items = items.map { candidate ->
                    ReminderItemUi(
                        orderId = candidate.orderId,
                        customerName = candidate.customerName,
                        packageName = candidate.packageName,
                        paymentStatus = candidate.paymentStatus,
                        orderDate = candidate.orderDate,
                        dueDate = candidate.dueDate,
                        bucketLabel = bucket.title(),
                        overdueLabel = bucket.overdueLabel(candidate.overdueDays)
                    )
                }
            )
        }
}

fun ReminderBucket.title(): String = when (this) {
    ReminderBucket.DUE_TODAY -> "Due today"
    ReminderBucket.OVERDUE_1_TO_2_DAYS -> "Overdue 1-2 days"
    ReminderBucket.OVERDUE_3_TO_6_DAYS -> "Overdue 3-6 days"
    ReminderBucket.OVERDUE_1_WEEK -> "Overdue 1 week"
    ReminderBucket.OVERDUE_2_WEEKS -> "Overdue 2 weeks"
    ReminderBucket.OVERDUE_3_WEEKS -> "Overdue 3 weeks"
    ReminderBucket.OVERDUE_1_MONTH_PLUS -> "Overdue 1 month or more"
}

fun ReminderBucket.overdueLabel(overdueDays: Int): String = when (this) {
    ReminderBucket.DUE_TODAY -> "Due today, needs a cross-check"
    else -> "Overdue $overdueDays days from the due date"
}
