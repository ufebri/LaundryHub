package com.raylabs.laundryhub.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderBucket
import com.raylabs.laundryhub.core.domain.usecase.reminder.EvaluateReminderCandidatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderLocalStatesUseCase
import com.raylabs.laundryhub.core.domain.usecase.reminder.ObserveReminderSettingsUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var observeReminderSettingsUseCase: ObserveReminderSettingsUseCase

    @Inject
    lateinit var observeReminderLocalStatesUseCase: ObserveReminderLocalStatesUseCase

    @Inject
    lateinit var reminderNotificationSheetReader: ReminderNotificationSheetReader

    @Inject
    lateinit var evaluateReminderCandidatesUseCase: EvaluateReminderCandidatesUseCase

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ReminderNotificationConfig.ACTION_DAILY_SUMMARY) return
        if (!context.hasReminderNotificationPermission()) return

        runBlocking {
            val settings = observeReminderSettingsUseCase().first()
            if (!settings.isReminderEnabled || !settings.isDailyNotificationEnabled) {
                ReminderNotificationPublisher.cancel(context)
                return@runBlocking
            }

            val transactions = reminderNotificationSheetReader.readTransactions()
            val localStates = observeReminderLocalStatesUseCase().first()
            val candidates = when (transactions) {
                is com.raylabs.laundryhub.ui.common.util.Resource.Success -> {
                    evaluateReminderCandidatesUseCase(transactions.data, localStates)
                }

                else -> emptyList()
            }

            if (candidates.isEmpty()) {
                ReminderNotificationPublisher.cancel(context)
                return@runBlocking
            }

            val topBucket = candidates.first().bucket.toNotificationLabel(context)
            val title = context.resources.getQuantityString(
                R.plurals.reminder_notification_title,
                candidates.size,
                candidates.size
            )
            val message = context.getString(R.string.reminder_notification_message, topBucket)
            ReminderNotificationPublisher.showSummary(
                context = context,
                title = title,
                message = message
            )
        }
    }

    private fun ReminderBucket.toNotificationLabel(context: Context): String = when (this) {
        ReminderBucket.DUE_TODAY -> context.getString(R.string.reminder_bucket_due_today)
        ReminderBucket.OVERDUE_1_TO_2_DAYS -> context.getString(R.string.reminder_bucket_overdue_1_2)
        ReminderBucket.OVERDUE_3_TO_6_DAYS -> context.getString(R.string.reminder_bucket_overdue_3_6)
        ReminderBucket.OVERDUE_1_WEEK -> context.getString(R.string.reminder_bucket_overdue_1_week)
        ReminderBucket.OVERDUE_2_WEEKS -> context.getString(R.string.reminder_bucket_overdue_2_weeks)
        ReminderBucket.OVERDUE_3_WEEKS -> context.getString(R.string.reminder_bucket_overdue_3_weeks)
        ReminderBucket.OVERDUE_1_MONTH_PLUS -> context.getString(R.string.reminder_bucket_overdue_1_month)
    }
}
