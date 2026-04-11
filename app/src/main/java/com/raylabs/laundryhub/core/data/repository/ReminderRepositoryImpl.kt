package com.raylabs.laundryhub.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import com.raylabs.laundryhub.core.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReminderRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val firebaseAuth: FirebaseAuth,
    private val gson: Gson
) : ReminderRepository {

    override val reminderSettings: Flow<ReminderSettings> = dataStore.data
        .map { preferences ->
            val key = reminderSettingsKey()
            parseSettings(preferences[key])
        }
        .distinctUntilChanged()

    override val reminderLocalStates: Flow<Map<String, ReminderLocalState>> = dataStore.data
        .map { preferences ->
            val key = reminderLocalStatesKey()
            parseLocalStates(preferences[key])
        }
        .distinctUntilChanged()

    override suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            val key = reminderSettingsKey()
            val current = parseSettings(preferences[key])
            preferences[key] = gson.toJson(current.copy(isReminderEnabled = enabled))
        }
    }

    override suspend fun setDailyNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            val key = reminderSettingsKey()
            val current = parseSettings(preferences[key])
            preferences[key] = gson.toJson(current.copy(isDailyNotificationEnabled = enabled))
        }
    }

    override suspend fun setDailyNotificationTime(hourOfDay: Int, minute: Int) {
        dataStore.edit { preferences ->
            val key = reminderSettingsKey()
            val current = parseSettings(preferences[key])
            preferences[key] = gson.toJson(
                current.copy(
                    notificationHour = hourOfDay.coerceIn(0, 23),
                    notificationMinute = minute.coerceIn(0, 59)
                )
            )
        }
    }

    override suspend fun markChecked(orderId: String, timestampMillis: Long) {
        updateLocalState(orderId) { current ->
            current.copy(
                checkedAtEpochMillis = timestampMillis,
                assumedPickedUpAtEpochMillis = null,
                dismissedAtEpochMillis = null,
                snoozedUntilEpochMillis = null
            )
        }
    }

    override suspend fun markAssumedPickedUp(orderId: String, timestampMillis: Long) {
        updateLocalState(orderId) { current ->
            current.copy(
                checkedAtEpochMillis = null,
                assumedPickedUpAtEpochMillis = timestampMillis,
                dismissedAtEpochMillis = null,
                snoozedUntilEpochMillis = null
            )
        }
    }

    override suspend fun dismiss(orderId: String, timestampMillis: Long) {
        updateLocalState(orderId) { current ->
            current.copy(
                checkedAtEpochMillis = null,
                assumedPickedUpAtEpochMillis = null,
                dismissedAtEpochMillis = timestampMillis,
                snoozedUntilEpochMillis = null
            )
        }
    }

    override suspend fun snooze(orderId: String, untilEpochMillis: Long) {
        updateLocalState(orderId) { current ->
            current.copy(
                checkedAtEpochMillis = null,
                assumedPickedUpAtEpochMillis = null,
                dismissedAtEpochMillis = null,
                snoozedUntilEpochMillis = untilEpochMillis
            )
        }
    }

    private suspend fun updateLocalState(
        orderId: String,
        transform: (ReminderLocalState) -> ReminderLocalState
    ) {
        dataStore.edit { preferences ->
            val key = reminderLocalStatesKey()
            val current = parseLocalStates(preferences[key]).toMutableMap()
            current[orderId] = transform(current[orderId] ?: ReminderLocalState())
            preferences[key] = gson.toJson(current)
        }
    }

    private fun reminderSettingsKey(): Preferences.Key<String> {
        return stringPreferencesKey("reminder_settings_${currentUserId()}")
    }

    private fun reminderLocalStatesKey(): Preferences.Key<String> {
        return stringPreferencesKey("reminder_local_states_${currentUserId()}")
    }

    private fun currentUserId(): String = firebaseAuth.currentUser?.uid ?: "guest"

    private fun parseSettings(raw: String?): ReminderSettings {
        if (raw.isNullOrBlank()) return ReminderSettings()
        return runCatching { gson.fromJson(raw, ReminderSettings::class.java) }
            .getOrDefault(ReminderSettings())
    }

    private fun parseLocalStates(raw: String?): Map<String, ReminderLocalState> {
        if (raw.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, ReminderLocalState>>() {}.type
        return runCatching { gson.fromJson<Map<String, ReminderLocalState>>(raw, type) }
            .getOrDefault(emptyMap())
    }
}
