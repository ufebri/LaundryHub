package com.raylabs.laundryhub.core.data.service

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleCredentialAuthManagerImplTest {

    private val credentialManager: CredentialManager = mock()
    private val context: Context = mock()

    @Test
    fun `signIn throws clear error when credential is not google id token`() = runTest {
        val credential: CustomCredential = mock()
        val response: GetCredentialResponse = mock()
        whenever(credential.type).thenReturn("unsupported-credential")
        whenever(response.credential).thenReturn(credential)
        whenever(
            credentialManager.getCredential(
                eq(context),
                any<GetCredentialRequest>()
            )
        ).thenReturn(response)

        val manager = GoogleCredentialAuthManagerImpl(credentialManager)

        val error = kotlin.runCatching { manager.signIn(context) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("Unsupported Google credential response.", error?.message)
    }

    @Test
    fun `clearCredentialState delegates to credential manager`() = runTest {
        val manager = GoogleCredentialAuthManagerImpl(credentialManager)

        manager.clearCredentialState()

        verify(credentialManager).clearCredentialState(any<ClearCredentialStateRequest>())
    }
}
