package com.raylabs.laundryhub.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SpreadsheetConfigRepositoryImplTest {

    @Test
    fun `spreadsheet config is isolated per signed in user`() = runTest {
        val userA = mockUser("uid-a")
        val userB = mockUser("uid-b")
        val (repository, authController) = createRepository(
            scope = backgroundScope,
            initialUser = userA
        )

        repository.saveSpreadsheetConnection(
            spreadsheetId = "sheet-a",
            spreadsheetName = "Laundry A",
            spreadsheetUrl = "https://sheet-a"
        )
        assertEquals(
            SpreadsheetConfig(
                spreadsheetId = "sheet-a",
                spreadsheetName = "Laundry A",
                spreadsheetUrl = "https://sheet-a",
                validationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION
            ),
            repository.spreadsheetConfig.first()
        )

        authController.switchUser(userB)
        assertEquals(SpreadsheetConfig(), repository.spreadsheetConfig.first())

        repository.saveSpreadsheetConnection(
            spreadsheetId = "sheet-b",
            spreadsheetName = "Laundry B",
            spreadsheetUrl = "https://sheet-b"
        )
        assertEquals(
            SpreadsheetConfig(
                spreadsheetId = "sheet-b",
                spreadsheetName = "Laundry B",
                spreadsheetUrl = "https://sheet-b",
                validationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION
            ),
            repository.spreadsheetConfig.first()
        )

        authController.switchUser(userA)
        assertEquals(
            SpreadsheetConfig(
                spreadsheetId = "sheet-a",
                spreadsheetName = "Laundry A",
                spreadsheetUrl = "https://sheet-a",
                validationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION
            ),
            repository.spreadsheetConfig.first()
        )
    }

    @Test
    fun `spreadsheet config flow emits empty state when auth changes to account without setup`() =
        runTest {
            val userA = mockUser("uid-a")
            val userB = mockUser("uid-b")
            val (repository, authController) = createRepository(
                scope = backgroundScope,
                initialUser = userA
            )
            val observedStates = mutableListOf<SpreadsheetConfig>()

            val collectionJob = launch {
                repository.spreadsheetConfig
                    .take(3)
                    .toList(observedStates)
            }
            advanceUntilIdle()

            repository.saveSpreadsheetConnection(
                spreadsheetId = "sheet-a",
                spreadsheetName = "Laundry A",
                spreadsheetUrl = "https://sheet-a"
            )
            advanceUntilIdle()

            authController.switchUser(userB)
            advanceUntilIdle()

            assertEquals(
                listOf(
                    SpreadsheetConfig(),
                    SpreadsheetConfig(
                        spreadsheetId = "sheet-a",
                        spreadsheetName = "Laundry A",
                        spreadsheetUrl = "https://sheet-a",
                        validationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION
                    ),
                    SpreadsheetConfig()
                ),
                observedStates
            )

            collectionJob.cancel()
        }

    @Test
    fun `clearSpreadsheetConnection only clears active user spreadsheet`() = runTest {
        val userA = mockUser("uid-a")
        val userB = mockUser("uid-b")
        val (repository, authController) = createRepository(
            scope = backgroundScope,
            initialUser = userA
        )

        repository.saveSpreadsheetConnection(
            spreadsheetId = "sheet-a",
            spreadsheetName = "Laundry A",
            spreadsheetUrl = "https://sheet-a"
        )

        authController.switchUser(userB)
        repository.saveSpreadsheetConnection(
            spreadsheetId = "sheet-b",
            spreadsheetName = "Laundry B",
            spreadsheetUrl = "https://sheet-b"
        )
        repository.clearSpreadsheetConnection()

        assertEquals(SpreadsheetConfig(), repository.spreadsheetConfig.first())

        authController.switchUser(userA)
        assertEquals(
            SpreadsheetConfig(
                spreadsheetId = "sheet-a",
                spreadsheetName = "Laundry A",
                spreadsheetUrl = "https://sheet-a",
                validationVersion = SpreadsheetConfig.CURRENT_VALIDATION_VERSION
            ),
            repository.spreadsheetConfig.first()
        )
    }

    @Test
    fun `save and clear spreadsheet connection are no-ops when there is no signed in user`() = runTest {
        val (repository, _) = createRepository(
            scope = backgroundScope,
            initialUser = null
        )

        repository.saveSpreadsheetConnection(
            spreadsheetId = "sheet-a",
            spreadsheetName = "Laundry A",
            spreadsheetUrl = "https://sheet-a"
        )
        repository.clearSpreadsheetConnection()

        assertEquals(SpreadsheetConfig(), repository.spreadsheetConfig.first())
    }

    private fun createRepository(
        scope: CoroutineScope,
        initialUser: FirebaseUser?
    ): Pair<SpreadsheetConfigRepositoryImpl, AuthController> {
        val firebaseAuth: FirebaseAuth = mock()
        val authController = AuthController(firebaseAuth, initialUser)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = {
                File.createTempFile("spreadsheet-config", ".preferences_pb")
            }
        )

        whenever(firebaseAuth.currentUser).thenAnswer { authController.currentUser }
        doAnswer { invocation ->
            authController.listener = invocation.getArgument(0)
            null
        }.whenever(firebaseAuth).addAuthStateListener(any())
        doNothing().whenever(firebaseAuth).removeAuthStateListener(any())

        return SpreadsheetConfigRepositoryImpl(dataStore, firebaseAuth) to authController
    }

    private fun mockUser(uid: String): FirebaseUser = mock<FirebaseUser>().also { user ->
        whenever(user.uid).thenReturn(uid)
    }

    private data class AuthController(
        val firebaseAuth: FirebaseAuth,
        var currentUser: FirebaseUser?
    ) {
        lateinit var listener: FirebaseAuth.AuthStateListener

        fun switchUser(nextUser: FirebaseUser?) {
            currentUser = nextUser
            if (::listener.isInitialized) {
                listener.onAuthStateChanged(firebaseAuth)
            }
        }
    }
}
