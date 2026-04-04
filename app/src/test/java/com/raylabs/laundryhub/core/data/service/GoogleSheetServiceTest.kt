package com.raylabs.laundryhub.core.data.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `getSheetsService throws clear error when no authorized account exists`() = runTest {
        whenever(authorizationManager.getSignedInEmail()).thenReturn(null)

        val service = GoogleSheetService(authorizationManager)

        val error = kotlin.runCatching { service.getSheetsService() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals(GoogleSheetService.MISSING_ACCESS_MESSAGE, error?.message)
        assertFalse(service.hasAuthorizedSheetsAccount())
    }
}
