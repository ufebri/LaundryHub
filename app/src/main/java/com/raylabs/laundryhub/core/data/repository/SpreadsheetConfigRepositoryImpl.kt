package com.raylabs.laundryhub.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetConfigRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class SpreadsheetConfigRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val firebaseAuth: FirebaseAuth
) : SpreadsheetConfigRepository {

    private val authStateFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }

        trySend(firebaseAuth.currentUser?.uid)
        firebaseAuth.addAuthStateListener(listener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }.distinctUntilChanged()

    override val spreadsheetConfig: Flow<SpreadsheetConfig> = authStateFlow
        .flatMapLatest { userId ->
            if (userId.isNullOrBlank()) {
                flowOf(SpreadsheetConfig())
            } else {
                dataStore.data.map { preferences ->
                    SpreadsheetConfig(
                        spreadsheetId = preferences[spreadsheetIdKey(userId)],
                        spreadsheetName = preferences[spreadsheetNameKey(userId)],
                        spreadsheetUrl = preferences[spreadsheetUrlKey(userId)],
                        validationVersion = preferences[spreadsheetValidationVersionKey(userId)] ?: 0
                    )
                }
            }
        }
        .distinctUntilChanged()

    override suspend fun saveSpreadsheetConnection(
        spreadsheetId: String,
        spreadsheetName: String,
        spreadsheetUrl: String?
    ) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        dataStore.edit { preferences ->
            preferences[spreadsheetIdKey(userId)] = spreadsheetId
            preferences[spreadsheetNameKey(userId)] = spreadsheetName
            if (spreadsheetUrl.isNullOrBlank()) {
                preferences.remove(spreadsheetUrlKey(userId))
            } else {
                preferences[spreadsheetUrlKey(userId)] = spreadsheetUrl
            }
            preferences[spreadsheetValidationVersionKey(userId)] =
                SpreadsheetConfig.CURRENT_VALIDATION_VERSION
        }
    }

    override suspend fun clearSpreadsheetConnection() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        dataStore.edit { preferences ->
            preferences.remove(spreadsheetIdKey(userId))
            preferences.remove(spreadsheetNameKey(userId))
            preferences.remove(spreadsheetUrlKey(userId))
            preferences.remove(spreadsheetValidationVersionKey(userId))
        }
    }

    private fun spreadsheetIdKey(userId: String) = stringPreferencesKey("spreadsheet_id_$userId")

    private fun spreadsheetNameKey(userId: String) = stringPreferencesKey("spreadsheet_name_$userId")

    private fun spreadsheetUrlKey(userId: String) = stringPreferencesKey("spreadsheet_url_$userId")

    private fun spreadsheetValidationVersionKey(userId: String) =
        intPreferencesKey("spreadsheet_validation_version_$userId")
}
