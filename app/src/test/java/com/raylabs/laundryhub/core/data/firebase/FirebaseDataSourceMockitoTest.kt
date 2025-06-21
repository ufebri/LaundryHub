package com.raylabs.laundryhub.core.data.firebase

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseDataSourceMockitoTest {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var dataSource: FirebaseDataSource
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var authResult: AuthResult
    private lateinit var task: Task<AuthResult>

    @Before
    fun setup() {
        firebaseAuth = mock()
        firebaseUser = mock()
        authResult = mock()
        task = mock()
        dataSource = FirebaseDataSource(firebaseAuth)
    }

    @Test
    fun `isUserLoggedIn returns true if user exists`() {
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        assertTrue(dataSource.isUserLoggedIn())
    }

    @Test
    fun `isUserLoggedIn returns false if user is null`() {
        whenever(firebaseAuth.currentUser).thenReturn(null)
        assertFalse(dataSource.isUserLoggedIn())
    }

    @Test
    fun `getCurrentUser returns User if firebaseUser exists`() {
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        whenever(firebaseUser.uid).thenReturn("uid123")
        whenever(firebaseUser.displayName).thenReturn("Test User")
        whenever(firebaseUser.email).thenReturn("test@email.com")
        whenever(firebaseUser.photoUrl).thenReturn(null)
        val user = dataSource.getCurrentUser()
        assertNotNull(user)
        assertEquals("uid123", user?.uid)
        assertEquals("Test User", user?.displayName)
        assertEquals("test@email.com", user?.email)
        assertEquals("null", user?.urlPhoto) // photoUrl.toString() on null returns "null"
    }

    @Test
    fun `getCurrentUser returns null if firebaseUser is null`() {
        whenever(firebaseAuth.currentUser).thenReturn(null)
        val user = dataSource.getCurrentUser()
        assertNull(user)
    }

    @Test
    fun `signOut calls firebaseAuth signOut`() = runTest {
        // No exception means success
        val result = dataSource.signOut()
        assertTrue(result)
        Mockito.verify(firebaseAuth).signOut()
    }
}
