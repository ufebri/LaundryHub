package com.raylabs.laundryhub.core.data.firebase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private lateinit var repository: AuthRepositoryImpl
    private lateinit var fakeDataSource: FakeFirebaseAuthDataSource

    @Before
    fun setup() {
        fakeDataSource = FakeFirebaseAuthDataSource()
        repository = AuthRepositoryImpl(fakeDataSource)
    }

    @Test
    fun `sign in returns user`() = runTest {
        val user = repository.signInWithGoogle("dummyToken")
        assertNotNull(user)
        assertEquals("123", user?.uid)
    }

    @Test
    fun `get current user returns correct data`() {
        val user = repository.getCurrentUser()
        assertNotNull(user)
        assertEquals("123", user?.uid)
    }

    @Test
    fun `is user logged in returns true`() {
        assertTrue(repository.isUserLoggedIn())
    }

    @Test
    fun `sign out returns true`() = runTest {
        val result = repository.signOut()
        assertTrue(result)
    }
}