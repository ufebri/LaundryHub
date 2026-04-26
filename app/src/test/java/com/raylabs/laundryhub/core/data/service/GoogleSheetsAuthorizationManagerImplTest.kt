package com.raylabs.laundryhub.core.data.service

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.tasks.Tasks
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleSheetsAuthorizationManagerImplTest {

    private val authorizationClient: AuthorizationClient = mock()
    private val accountProvider: GoogleSheetsAccountProvider = mock()

    @Test
    fun `getSignedInEmail delegates to account provider`() {
        whenever(accountProvider.getSignedInEmail()).thenReturn("owner@laundryhub.com")

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertEquals("owner@laundryhub.com", manager.getSignedInEmail())
    }

    @Test
    fun `hasSheetsAccess returns false when no account is available`() = runTest {
        whenever(accountProvider.getSignedInEmail()).thenReturn(null)

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertFalse(manager.hasSheetsAccess())
    }

    @Test
    fun `getAccessToken returns null when no account is available`() = runTest {
        whenever(accountProvider.getSignedInEmail()).thenReturn(null)

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertEquals(null, manager.getAccessToken())
    }

    @Test
    fun `getAuthorizationIntentSender returns null when no account is available`() = runTest {
        whenever(accountProvider.getSignedInEmail()).thenReturn(null)

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertEquals(null, manager.getAuthorizationIntentSender())
    }

    @Test
    fun `hasSheetsAccess returns true when authorization result grants sheets scope`() = runTest {
        whenever(accountProvider.getSignedInEmail()).thenReturn("owner@laundryhub.com")
        val result: AuthorizationResult = mock()
        whenever(result.hasResolution()).thenReturn(false)
        whenever(result.accessToken).thenReturn("token")
        whenever(result.grantedScopes).thenReturn(
            listOf(
                SheetsScopes.SPREADSHEETS,
                GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE
            )
        )
        whenever(authorizationClient.authorize(any())).thenReturn(Tasks.forResult(result))

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertTrue(manager.hasSheetsAccess())
    }

    @Test
    fun `hasSheetsAccess returns false when authorization still requires resolution`() = runTest {
        whenever(accountProvider.getSignedInEmail()).thenReturn("owner@laundryhub.com")
        val result: AuthorizationResult = mock()
        whenever(result.hasResolution()).thenReturn(true)
        whenever(result.accessToken).thenReturn(null)
        whenever(result.grantedScopes).thenReturn(
            listOf(
                SheetsScopes.SPREADSHEETS,
                GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE
            )
        )
        whenever(authorizationClient.authorize(any())).thenReturn(Tasks.forResult(result))

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertFalse(manager.hasSheetsAccess())
    }

    @Test
    fun `handleAuthorizationResult returns true when sheets scope is granted`() {
        whenever(accountProvider.getSignedInEmail()).thenReturn("owner@laundryhub.com")
        val result: AuthorizationResult = mock()
        whenever(result.accessToken).thenReturn("token")
        whenever(result.grantedScopes).thenReturn(
            listOf(
                SheetsScopes.SPREADSHEETS,
                GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE
            )
        )
        whenever(authorizationClient.getAuthorizationResultFromIntent(any())).thenReturn(result)

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertTrue(manager.handleAuthorizationResult(mock<Intent>()))
    }

    @Test
    fun `handleAuthorizationResult returns false when intent data is missing`() {
        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertFalse(manager.handleAuthorizationResult(null))
    }

    @Test
    fun `handleAuthorizationResult returns false when required scopes are incomplete`() {
        val result: AuthorizationResult = mock()
        whenever(result.accessToken).thenReturn("token")
        whenever(result.grantedScopes).thenReturn(listOf(SheetsScopes.SPREADSHEETS))
        whenever(authorizationClient.getAuthorizationResultFromIntent(any())).thenReturn(result)

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertFalse(manager.handleAuthorizationResult(mock<Intent>()))
    }

    @Test
    fun `handleAuthorizationResult returns false when authorization client throws`() {
        whenever(authorizationClient.getAuthorizationResultFromIntent(any())).thenThrow(
            IllegalStateException("result missing")
        )

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertFalse(manager.handleAuthorizationResult(mock<Intent>()))
    }

    @Test
    fun `hasSheetsAccess returns false when drive metadata scope is missing`() = runTest {
        whenever(accountProvider.getSignedInEmail()).thenReturn("owner@laundryhub.com")
        val result: AuthorizationResult = mock()
        whenever(result.hasResolution()).thenReturn(false)
        whenever(result.accessToken).thenReturn("token")
        whenever(result.grantedScopes).thenReturn(listOf(SheetsScopes.SPREADSHEETS))
        whenever(authorizationClient.authorize(any())).thenReturn(Tasks.forResult(result))

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertFalse(manager.hasSheetsAccess())
    }

    @Test
    fun `hasSheetsAccess returns false when access token is missing`() = runTest {
        whenever(accountProvider.getSignedInEmail()).thenReturn("owner@laundryhub.com")
        val result: AuthorizationResult = mock()
        whenever(result.hasResolution()).thenReturn(false)
        whenever(result.accessToken).thenReturn(null)
        whenever(result.grantedScopes).thenReturn(
            listOf(
                SheetsScopes.SPREADSHEETS,
                GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE
            )
        )
        whenever(authorizationClient.authorize(any())).thenReturn(Tasks.forResult(result))

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertFalse(manager.hasSheetsAccess())
    }

    @Test
    fun `getAccessToken requests a fresh authorization result each time`() = runTest {
        whenever(accountProvider.getSignedInEmail()).thenReturn("owner@laundryhub.com")
        val firstResult: AuthorizationResult = mock()
        whenever(firstResult.hasResolution()).thenReturn(false)
        whenever(firstResult.accessToken).thenReturn("token-1")
        whenever(firstResult.grantedScopes).thenReturn(
            listOf(
                SheetsScopes.SPREADSHEETS,
                GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE
            )
        )
        val secondResult: AuthorizationResult = mock()
        whenever(secondResult.hasResolution()).thenReturn(false)
        whenever(secondResult.accessToken).thenReturn("token-2")
        whenever(secondResult.grantedScopes).thenReturn(
            listOf(
                SheetsScopes.SPREADSHEETS,
                GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE
            )
        )
        whenever(authorizationClient.authorize(any())).thenReturn(
            Tasks.forResult(firstResult),
            Tasks.forResult(secondResult)
        )

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertEquals("token-1", manager.getAccessToken())
        assertEquals("token-2", manager.getAccessToken())
        verify(authorizationClient, times(2)).authorize(any())
    }

    @Test
    fun `hasSheetsAccess rechecks authorization instead of trusting an old cached token`() = runTest {
        whenever(accountProvider.getSignedInEmail()).thenReturn("owner@laundryhub.com")
        val firstResult: AuthorizationResult = mock()
        whenever(firstResult.hasResolution()).thenReturn(false)
        whenever(firstResult.accessToken).thenReturn("token-1")
        whenever(firstResult.grantedScopes).thenReturn(
            listOf(
                SheetsScopes.SPREADSHEETS,
                GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE
            )
        )
        val secondResult: AuthorizationResult = mock()
        whenever(secondResult.hasResolution()).thenReturn(false)
        whenever(secondResult.accessToken).thenReturn("token-2")
        whenever(secondResult.grantedScopes).thenReturn(
            listOf(
                SheetsScopes.SPREADSHEETS,
                GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE
            )
        )
        whenever(authorizationClient.authorize(any())).thenReturn(
            Tasks.forResult(firstResult),
            Tasks.forResult(secondResult)
        )

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertTrue(manager.hasSheetsAccess())
        assertTrue(manager.hasSheetsAccess())
        verify(authorizationClient, times(2)).authorize(any())
    }

    @Test
    fun `getAccessToken returns null when authorization requires resolution`() = runTest {
        whenever(accountProvider.getSignedInEmail()).thenReturn("owner@laundryhub.com")
        val result: AuthorizationResult = mock()
        whenever(result.hasResolution()).thenReturn(true)
        whenever(result.accessToken).thenReturn(null)
        whenever(result.grantedScopes).thenReturn(
            listOf(
                SheetsScopes.SPREADSHEETS,
                GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE
            )
        )
        whenever(authorizationClient.authorize(any())).thenReturn(Tasks.forResult(result))

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertEquals(null, manager.getAccessToken())
    }

    @Test
    fun `getAuthorizationIntentSender returns sender when authorization needs resolution`() = runTest {
        whenever(accountProvider.getSignedInEmail()).thenReturn("owner@laundryhub.com")
        val result: AuthorizationResult = mock()
        val pendingIntent: PendingIntent = mock()
        val intentSender: IntentSender = mock()
        whenever(result.hasResolution()).thenReturn(true)
        whenever(result.pendingIntent).thenReturn(pendingIntent)
        whenever(pendingIntent.intentSender).thenReturn(intentSender)
        whenever(result.grantedScopes).thenReturn(
            listOf(
                SheetsScopes.SPREADSHEETS,
                GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE
            )
        )
        whenever(authorizationClient.authorize(any())).thenReturn(Tasks.forResult(result))

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertEquals(intentSender, manager.getAuthorizationIntentSender())
    }

    @Test
    fun `getAuthorizationIntentSender rechecks authorization even after access was previously granted`() = runTest {
        whenever(accountProvider.getSignedInEmail()).thenReturn("owner@laundryhub.com")
        val grantedResult: AuthorizationResult = mock()
        whenever(grantedResult.hasResolution()).thenReturn(false)
        whenever(grantedResult.accessToken).thenReturn("token")
        whenever(grantedResult.grantedScopes).thenReturn(
            listOf(
                SheetsScopes.SPREADSHEETS,
                GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE
            )
        )
        val pendingIntent: PendingIntent = mock()
        val intentSender: IntentSender = mock()
        val resolutionResult: AuthorizationResult = mock()
        whenever(resolutionResult.hasResolution()).thenReturn(true)
        whenever(resolutionResult.pendingIntent).thenReturn(pendingIntent)
        whenever(pendingIntent.intentSender).thenReturn(intentSender)
        whenever(resolutionResult.grantedScopes).thenReturn(
            listOf(
                SheetsScopes.SPREADSHEETS,
                GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE
            )
        )
        whenever(authorizationClient.authorize(any())).thenReturn(
            Tasks.forResult(grantedResult),
            Tasks.forResult(resolutionResult)
        )

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertTrue(manager.hasSheetsAccess())
        assertEquals(intentSender, manager.getAuthorizationIntentSender())
        verify(authorizationClient, times(2)).authorize(any())
    }
}
