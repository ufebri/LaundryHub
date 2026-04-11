package com.raylabs.laundryhub.core.domain.model.reminder

data class ReminderSettings(
    val isReminderEnabled: Boolean = false,
    val isDailyNotificationEnabled: Boolean = false,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0
)

data class ReminderLocalState(
    val checkedAtEpochMillis: Long? = null,
    val assumedPickedUpAtEpochMillis: Long? = null,
    val dismissedAtEpochMillis: Long? = null,
    val snoozedUntilEpochMillis: Long? = null
) {
    val isResolved: Boolean
        get() = checkedAtEpochMillis != null ||
            assumedPickedUpAtEpochMillis != null ||
            dismissedAtEpochMillis != null
}

enum class ReminderBucket {
    DUE_TODAY,
    OVERDUE_1_TO_2_DAYS,
    OVERDUE_3_TO_6_DAYS,
    OVERDUE_1_WEEK,
    OVERDUE_2_WEEKS,
    OVERDUE_3_WEEKS,
    OVERDUE_1_MONTH_PLUS
}

data class ReminderCandidate(
    val orderId: String,
    val customerName: String,
    val packageName: String,
    val paymentStatus: String,
    val orderDate: String,
    val dueDate: String,
    val bucket: ReminderBucket,
    val overdueDays: Int
)
