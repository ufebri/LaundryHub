package com.raylabs.laundryhub.core.data.service

import com.google.android.gms.auth.api.identity.AuthorizationClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleSheetsAuthorizationManagerImplTest {

    private val authorizationClient: AuthorizationClient = mock()
    private val googleSheetsAccountProvider: GoogleSheetsAccountProvider = mock()
    private lateinit var manager: GoogleSheetsAuthorizationManagerImpl

    @Before
    fun setUp() {
        manager = GoogleSheetsAuthorizationManagerImpl(authorizationClient, googleSheetsAccountProvider)
    }

    @Test
    fun `getSignedInEmail returns email from provider`() {
        val expectedEmail = "test@example.com"
        whenever(googleSheetsAccountProvider.getSignedInEmail()).thenReturn(expectedEmail)

        val actualEmail = manager.getSignedInEmail()

        assertEquals(expectedEmail, actualEmail)
    }

    @Test
    fun `hasSheetsAccess now always returns true in KMP architecture`() = runTest {
        assertTrue(manager.hasSheetsAccess())
    }

    @Test
    fun `getAccessToken now returns empty string as tokens are handled by backend`() = runTest {
        assertEquals("", manager.getAccessToken())
    }
}
