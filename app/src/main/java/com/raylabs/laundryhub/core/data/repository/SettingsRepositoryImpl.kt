package com.raylabs.laundryhub.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.raylabs.laundryhub.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val showWhatsAppOption: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[SHOW_WHATSAPP_OPTION] ?: true }
        .distinctUntilChanged()

    override suspend fun setShowWhatsAppOption(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_WHATSAPP_OPTION] = enabled
        }
    }

    private companion object {
        val SHOW_WHATSAPP_OPTION = booleanPreferencesKey("show_whatsapp_option")
    }
}
