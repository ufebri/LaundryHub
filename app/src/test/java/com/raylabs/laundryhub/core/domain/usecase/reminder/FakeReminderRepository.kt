package com.raylabs.laundryhub.core.domain.usecase.reminder

import com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeReminderRepository(
    initialSettings: ReminderSettings = ReminderSettings(),
    initialLocalStates: Map<String, ReminderLocalState> = emptyMap()
) : ReminderRepository {

    private val settingsState = MutableStateFlow(initialSettings)
    private val localStatesState = MutableStateFlow(initialLocalStates)

    override val reminderSettings: Flow<ReminderSettings> = settingsState
    override val reminderLocalStates: Flow<Map<String, ReminderLocalState>> = localStatesState

    val setReminderEnabledCalls = mutableListOf<Boolean>()
    val setDailyNotificationEnabledCalls = mutableListOf<Boolean>()
    val setDailyNotificationTimeCalls = mutableListOf<Pair<Int, Int>>()
    val markCheckedCalls = mutableListOf<Pair<String, Long>>()
    val markAssumedPickedUpCalls = mutableListOf<Pair<String, Long>>()
    val dismissCalls = mutableListOf<Pair<String, Long>>()
    val snoozeCalls = mutableListOf<Pair<String, Long>>()

    fun updateSettings(settings: ReminderSettings) {
        settingsState.value = settings
    }

    fun currentSettings(): ReminderSettings = settingsState.value

    fun currentLocalStates(): Map<String, ReminderLocalState> = localStatesState.value

    override suspend fun setReminderEnabled(enabled: Boolean) {
        setReminderEnabledCalls += enabled
        settingsState.update { it.copy(isReminderEnabled = enabled) }
    }

    override suspend fun setDailyNotificationEnabled(enabled: Boolean) {
        setDailyNotificationEnabledCalls += enabled
        settingsState.update { it.copy(isDailyNotificationEnabled = enabled) }
    }

    override suspend fun setDailyNotificationTime(hourOfDay: Int, minute: Int) {
        setDailyNotificationTimeCalls += hourOfDay to minute
        settingsState.update {
            it.copy(
                notificationHour = hourOfDay.coerceIn(0, 23),
                notificationMinute = minute.coerceIn(0, 59)
            )
        }
    }

    override suspend fun markChecked(orderId: String, timestampMillis: Long) {
        markCheckedCalls += orderId to timestampMillis
        updateLocalState(orderId) {
            it.copy(
                checkedAtEpochMillis = timestampMillis,
                assumedPickedUpAtEpochMillis = null,
                dismissedAtEpochMillis = null,
                snoozedUntilEpochMillis = null
            )
        }
    }

    override suspend fun markAssumedPickedUp(orderId: String, timestampMillis: Long) {
        markAssumedPickedUpCalls += orderId to timestampMillis
        updateLocalState(orderId) {
            it.copy(
                checkedAtEpochMillis = null,
                assumedPickedUpAtEpochMillis = timestampMillis,
                dismissedAtEpochMillis = null,
                snoozedUntilEpochMillis = null
            )
        }
    }

    override suspend fun dismiss(orderId: String, timestampMillis: Long) {
        dismissCalls += orderId to timestampMillis
        updateLocalState(orderId) {
            it.copy(
                checkedAtEpochMillis = null,
                assumedPickedUpAtEpochMillis = null,
                dismissedAtEpochMillis = timestampMillis,
                snoozedUntilEpochMillis = null
            )
        }
    }

    override suspend fun snooze(orderId: String, untilEpochMillis: Long) {
        snoozeCalls += orderId to untilEpochMillis
        updateLocalState(orderId) {
            it.copy(
                checkedAtEpochMillis = null,
                assumedPickedUpAtEpochMillis = null,
                dismissedAtEpochMillis = null,
                snoozedUntilEpochMillis = untilEpochMillis
            )
        }
    }

    private fun updateLocalState(
        orderId: String,
        transform: (ReminderLocalState) -> ReminderLocalState
    ) {
        localStatesState.update { current ->
            current + (orderId to transform(current[orderId] ?: ReminderLocalState()))
        }
    }
}
