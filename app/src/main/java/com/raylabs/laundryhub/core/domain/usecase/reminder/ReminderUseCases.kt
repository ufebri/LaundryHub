package com.raylabs.laundryhub.core.domain.usecase.reminder

import com.raylabs.laundryhub.core.domain.model.reminder.ReminderBucket
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderCandidate
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import com.raylabs.laundryhub.core.domain.repository.ReminderRepository
import com.raylabs.laundryhub.ui.common.util.DateUtil
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ObserveReminderSettingsUseCase @Inject constructor(
    private val repository: ReminderRepository
) {
    operator fun invoke(): Flow<ReminderSettings> = repository.reminderSettings
}

class ObserveReminderLocalStatesUseCase @Inject constructor(
    private val repository: ReminderRepository
) {
    operator fun invoke(): Flow<Map<String, ReminderLocalState>> = repository.reminderLocalStates
}

class SetReminderEnabledUseCase @Inject constructor(
    private val repository: ReminderRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setReminderEnabled(enabled)
    }
}

class SetDailyReminderNotificationEnabledUseCase @Inject constructor(
    private val repository: ReminderRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setDailyNotificationEnabled(enabled)
    }
}

class SetDailyReminderNotificationTimeUseCase @Inject constructor(
    private val repository: ReminderRepository
) {
    suspend operator fun invoke(hourOfDay: Int, minute: Int) {
        repository.setDailyNotificationTime(hourOfDay, minute)
    }
}

class MarkReminderCheckedUseCase @Inject constructor(
    private val repository: ReminderRepository
) {
    suspend operator fun invoke(orderId: String, timestampMillis: Long = System.currentTimeMillis()) {
        repository.markChecked(orderId, timestampMillis)
    }
}

class MarkReminderAssumedPickedUpUseCase @Inject constructor(
    private val repository: ReminderRepository
) {
    suspend operator fun invoke(orderId: String, timestampMillis: Long = System.currentTimeMillis()) {
        repository.markAssumedPickedUp(orderId, timestampMillis)
    }
}

class DismissReminderUseCase @Inject constructor(
    private val repository: ReminderRepository
) {
    suspend operator fun invoke(orderId: String, timestampMillis: Long = System.currentTimeMillis()) {
        repository.dismiss(orderId, timestampMillis)
    }
}

class SnoozeReminderUseCase @Inject constructor(
    private val repository: ReminderRepository
) {
    suspend operator fun invoke(orderId: String, untilEpochMillis: Long) {
        repository.snooze(orderId, untilEpochMillis)
    }
}

class EvaluateReminderCandidatesUseCase @Inject constructor() {

    operator fun invoke(
        transactions: List<TransactionData>,
        localStates: Map<String, ReminderLocalState>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<ReminderCandidate> {
        val todayStartMillis = startOfDay(nowMillis)

        return transactions.mapNotNull { transaction ->
            val dueDateRaw = transaction.dueDate.trim()
            if (dueDateRaw.isBlank()) return@mapNotNull null

            val dueDate = DateUtil.parseSupportedAppDate(dueDateRaw)
                ?: return@mapNotNull null
            val localState = localStates[transaction.orderID]
            if (localState != null) {
                if (localState.isResolved) return@mapNotNull null
                if ((localState.snoozedUntilEpochMillis ?: Long.MIN_VALUE) > todayStartMillis) {
                    return@mapNotNull null
                }
            }

            val dueMillis = startOfDay(dueDate.time)
            if (dueMillis > todayStartMillis) return@mapNotNull null

            val overdueDays = TimeUnit.MILLISECONDS.toDays(todayStartMillis - dueMillis).toInt()

            ReminderCandidate(
                orderId = transaction.orderID,
                customerName = transaction.name,
                packageName = transaction.packageType,
                paymentStatus = transaction.paidDescription(),
                orderDate = transaction.date,
                dueDate = DateUtil.formatDate(dueDate),
                bucket = overdueDays.toReminderBucket(),
                overdueDays = overdueDays
            )
        }.sortedWith(
            compareBy<ReminderCandidate>({ it.bucket.ordinal })
                .thenByDescending { it.overdueDays }
                .thenBy { it.customerName.lowercase() }
        )
    }

    private fun Int.toReminderBucket(): ReminderBucket = when {
        this <= 0 -> ReminderBucket.DUE_TODAY
        this in 1..2 -> ReminderBucket.OVERDUE_1_TO_2_DAYS
        this in 3..6 -> ReminderBucket.OVERDUE_3_TO_6_DAYS
        this in 7..13 -> ReminderBucket.OVERDUE_1_WEEK
        this in 14..20 -> ReminderBucket.OVERDUE_2_WEEKS
        this in 21..29 -> ReminderBucket.OVERDUE_3_WEEKS
        else -> ReminderBucket.OVERDUE_1_MONTH_PLUS
    }

    private fun startOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
