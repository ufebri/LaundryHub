package com.raylabs.laundryhub.core.data.firebase

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class FirebaseDataSourceTest {

    private lateinit var fake: FirebaseAuthDataSource

    @Before
    fun setup() {
        fake = FakeFirebaseAuthDataSource()
    }

    @Test
    fun `sign in with google returns valid user`() = runTest {
        val result = fake.signInWithGoogle("some_token")
        assertNotNull(result)
        assertEquals("John", result?.displayName)
    }

    @Test
    fun `get current user returns valid user`() {
        val result = fake.getCurrentUser()
        assertNotNull(result)
        assertEquals("123", result?.uid)
    }

    @Test
    fun `sign out clears current user`() = runTest {
        fake.signOut()
        assertNull(fake.getCurrentUser())
    }

    @Test
    fun `is user logged in returns true`() {
        assertTrue(fake.isUserLoggedIn())
    }
}