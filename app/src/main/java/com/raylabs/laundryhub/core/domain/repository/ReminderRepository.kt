package com.raylabs.laundryhub.core.domain.repository

import com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    val reminderSettings: Flow<ReminderSettings>
    val reminderLocalStates: Flow<Map<String, ReminderLocalState>>

    suspend fun setReminderEnabled(enabled: Boolean)
    suspend fun setDailyNotificationEnabled(enabled: Boolean)
    suspend fun setDailyNotificationTime(hourOfDay: Int, minute: Int)
    suspend fun markChecked(orderId: String, timestampMillis: Long)
    suspend fun markAssumedPickedUp(orderId: String, timestampMillis: Long)
    suspend fun dismiss(orderId: String, timestampMillis: Long)
    suspend fun snooze(orderId: String, untilEpochMillis: Long)
}
