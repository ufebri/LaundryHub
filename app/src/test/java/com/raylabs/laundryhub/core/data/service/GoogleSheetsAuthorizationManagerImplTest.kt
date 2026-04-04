package com.raylabs.laundryhub.core.data.service

import android.content.Intent
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
    fun `hasSheetsAccess returns false when no account is available`() = runTest {
        whenever(accountProvider.getSignedInEmail()).thenReturn(null)

        val manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, accountProvider)

        assertFalse(manager.hasSheetsAccess())
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
    fun `handleAuthorizationResult returns true when sheets scope is granted`() {
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
    fun `getAccessToken reuses cached token after first authorization`() = runTest {
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

        assertEquals("token", manager.getAccessToken())
        assertEquals("token", manager.getAccessToken())
        verify(authorizationClient, times(1)).authorize(any())
    }
}
