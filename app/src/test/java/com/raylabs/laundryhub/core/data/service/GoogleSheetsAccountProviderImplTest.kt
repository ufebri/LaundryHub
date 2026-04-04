package com.raylabs.laundryhub.core.data.service

import com.google.firebase.auth.FirebaseAuth
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GoogleSheetsAccountProviderImplTest {

    @Test
    fun `getAuthorizedAccount returns null when no signed in email exists`() {
        val firebaseAuth: FirebaseAuth = mock()
        whenever(firebaseAuth.currentUser).thenReturn(null)

        val provider = GoogleSheetsAccountProviderImpl(firebaseAuth)

        assertNull(provider.getAuthorizedAccount())
        assertNull(provider.getSignedInEmail())
    }

    @Test
    fun `getAuthorizedAccount returns null when signed in user email is missing`() {
        val signedInUser = mock<com.google.firebase.auth.FirebaseUser> {
            on { email } doReturn null
        }
        val firebaseAuth: FirebaseAuth = mock {
            on { currentUser } doReturn signedInUser
        }

        val provider = GoogleSheetsAccountProviderImpl(firebaseAuth)

        assertNull(provider.getAuthorizedAccount())
        assertNull(provider.getSignedInEmail())
    }
}
