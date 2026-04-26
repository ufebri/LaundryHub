package com.raylabs.laundryhub.core.data.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleSheetServiceTest {

    private val authorizationManager: GoogleSheetsAuthorizationManager = mock()

    @Test
    fun `hasAuthorizedSheetsAccount delegates to authorization manager`() {
        whenever(authorizationManager.getSignedInEmail()).thenReturn("user@example.com")

        val service = GoogleSheetService(authorizationManager)

        assertTrue(service.hasAuthorizedSheetsAccount())
    }

    @Test
    fun `getAuthorizedAccountEmail delegates to authorization manager`() {
        whenever(authorizationManager.getSignedInEmail()).thenReturn("user@example.com")

        val service = GoogleSheetService(authorizationManager)

        assertEquals("user@example.com", service.getAuthorizedAccountEmail())
    }

    @Test
    fun `getSheetsService builds service when email and access token exist`() = runTest {
        whenever(authorizationManager.getSignedInEmail()).thenReturn("user@example.com")
        whenever(authorizationManager.getAccessToken()).thenReturn("token-123")

        val service = GoogleSheetService(authorizationManager)

        val sheets = service.getSheetsService()

        assertNotNull(sheets)
        assertEquals("LaundryHub App", sheets.applicationName)
    }

    @Test
    fun `getSheetsService throws clear error when no authorized account exists`() = runTest {
        whenever(authorizationManager.getSignedInEmail()).thenReturn(null)

        val service = GoogleSheetService(authorizationManager)

        val error = kotlin.runCatching { service.getSheetsService() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals(GoogleSheetService.MISSING_ACCESS_MESSAGE, error?.message)
        assertFalse(service.hasAuthorizedSheetsAccount())
    }

    @Test
    fun `getSheetsService throws clear error when access token is unavailable`() = runTest {
        whenever(authorizationManager.getSignedInEmail()).thenReturn("user@example.com")
        whenever(authorizationManager.getAccessToken()).thenReturn(null)

        val service = GoogleSheetService(authorizationManager)

        val error = kotlin.runCatching { service.getSheetsService() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals(
            "Google Sheets access token is unavailable for user@example.com. Reconnect Google Sheets access and try again.",
            error?.message
        )
    }

    @Test
    fun `hasSpreadsheetEditAccess throws clear error when no authorized account exists`() = runTest {
        whenever(authorizationManager.getSignedInEmail()).thenReturn(null)

        val service = GoogleSheetService(authorizationManager)

        val error = kotlin.runCatching {
            service.hasSpreadsheetEditAccess("sheet-123")
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals(GoogleSheetService.MISSING_ACCESS_MESSAGE, error?.message)
    }

    @Test
    fun `hasSpreadsheetEditAccess throws clear error when access token is unavailable`() = runTest {
        whenever(authorizationManager.getSignedInEmail()).thenReturn("user@example.com")
        whenever(authorizationManager.getAccessToken()).thenReturn("")

        val service = GoogleSheetService(authorizationManager)

        val error = kotlin.runCatching {
            service.hasSpreadsheetEditAccess("sheet-123")
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals(
            "Google Sheets access token is unavailable for user@example.com. Reconnect Google Sheets access and try again.",
            error?.message
        )
    }

    @Test
    fun `hasSpreadsheetEditAccess initiates request when token is available`() = runTest {
        whenever(authorizationManager.getSignedInEmail()).thenReturn("user@example.com")
        whenever(authorizationManager.getAccessToken()).thenReturn("valid-token")

        val service = GoogleSheetService(authorizationManager)

        val error = kotlin.runCatching {
            service.hasSpreadsheetEditAccess("sheet-123")
        }.exceptionOrNull()

        assertNotNull(error)
    }
}
