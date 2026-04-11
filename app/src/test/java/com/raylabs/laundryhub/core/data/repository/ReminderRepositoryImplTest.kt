package com.raylabs.laundryhub.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.gson.Gson
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderLocalState
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderRepositoryImplTest {

    @Test
    fun `reminder settings are isolated per signed in user`() = runTest {
        val userA = mockUser("uid-a")
        val userB = mockUser("uid-b")
        val (repository, authController, _) = createRepository(
            scope = backgroundScope,
            initialUser = userA
        )

        repository.setReminderEnabled(true)
        repository.setDailyNotificationEnabled(true)
        assertEquals(
            ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true
            ),
            repository.reminderSettings.first()
        )

        authController.currentUser = userB
        assertEquals(ReminderSettings(), repository.reminderSettings.first())

        repository.setDailyNotificationEnabled(true)
        assertEquals(
            ReminderSettings(
                isReminderEnabled = false,
                isDailyNotificationEnabled = true
            ),
            repository.reminderSettings.first()
        )

        authController.currentUser = userA
        assertEquals(
            ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true
            ),
            repository.reminderSettings.first()
        )
    }

    @Test
    fun `local state actions overwrite the reminder state for the active user only`() = runTest {
        val userA = mockUser("uid-a")
        val userB = mockUser("uid-b")
        val (repository, authController, _) = createRepository(
            scope = backgroundScope,
            initialUser = userA
        )

        repository.markChecked(orderId = "A-1", timestampMillis = 11L)
        assertEquals(
            ReminderLocalState(checkedAtEpochMillis = 11L),
            repository.reminderLocalStates.first()["A-1"]
        )

        repository.snooze(orderId = "A-1", untilEpochMillis = 22L)
        assertEquals(
            ReminderLocalState(snoozedUntilEpochMillis = 22L),
            repository.reminderLocalStates.first()["A-1"]
        )

        repository.markAssumedPickedUp(orderId = "A-1", timestampMillis = 33L)
        assertEquals(
            ReminderLocalState(assumedPickedUpAtEpochMillis = 33L),
            repository.reminderLocalStates.first()["A-1"]
        )

        repository.dismiss(orderId = "A-1", timestampMillis = 44L)
        assertEquals(
            ReminderLocalState(dismissedAtEpochMillis = 44L),
            repository.reminderLocalStates.first()["A-1"]
        )

        authController.currentUser = userB
        assertTrue(repository.reminderLocalStates.first().isEmpty())

        repository.markChecked(orderId = "B-1", timestampMillis = 55L)
        assertEquals(
            mapOf("B-1" to ReminderLocalState(checkedAtEpochMillis = 55L)),
            repository.reminderLocalStates.first()
        )

        authController.currentUser = userA
        assertEquals(
            mapOf("A-1" to ReminderLocalState(dismissedAtEpochMillis = 44L)),
            repository.reminderLocalStates.first()
        )
    }

    @Test
    fun `invalid datastore payload falls back to default reminder values`() = runTest {
        val user = mockUser("uid-a")
        val (repository, _, dataStore) = createRepository(
            scope = backgroundScope,
            initialUser = user
        )

        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("reminder_settings_uid-a")] = "{invalid"
            preferences[stringPreferencesKey("reminder_local_states_uid-a")] = "{invalid"
        }

        assertEquals(ReminderSettings(), repository.reminderSettings.first())
        assertTrue(repository.reminderLocalStates.first().isEmpty())
    }

    @Test
    fun `daily notification time is clamped before persisting`() = runTest {
        val user = mockUser("uid-a")
        val (repository, _, _) = createRepository(
            scope = backgroundScope,
            initialUser = user
        )

        repository.setDailyNotificationTime(hourOfDay = 99, minute = -5)

        assertEquals(
            ReminderSettings(
                notificationHour = 23,
                notificationMinute = 0
            ),
            repository.reminderSettings.first()
        )
    }

    @Test
    fun `guest reminder state uses isolated guest keys when no user is signed in`() = runTest {
        val (repository, _, _) = createRepository(
            scope = backgroundScope,
            initialUser = null
        )

        repository.setReminderEnabled(true)
        repository.markChecked(orderId = "guest-order", timestampMillis = 99L)

        assertEquals(
            ReminderSettings(isReminderEnabled = true),
            repository.reminderSettings.first()
        )
        assertEquals(
            mapOf("guest-order" to ReminderLocalState(checkedAtEpochMillis = 99L)),
            repository.reminderLocalStates.first()
        )
    }

    private fun createRepository(
        scope: CoroutineScope,
        initialUser: FirebaseUser?
    ): Triple<ReminderRepositoryImpl, AuthController, DataStore<Preferences>> {
        val firebaseAuth: FirebaseAuth = mock()
        val authController = AuthController(initialUser)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File.createTempFile("reminder-repo", ".preferences_pb") }
        )

        whenever(firebaseAuth.currentUser).thenAnswer { authController.currentUser }

        return Triple(
            ReminderRepositoryImpl(
                dataStore = dataStore,
                firebaseAuth = firebaseAuth,
                gson = Gson()
            ),
            authController,
            dataStore
        )
    }

    private fun mockUser(uid: String): FirebaseUser = mock<FirebaseUser>().also { user ->
        whenever(user.uid).thenReturn(uid)
    }

    private data class AuthController(
        var currentUser: FirebaseUser?
    )
}
