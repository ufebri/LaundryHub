package com.raylabs.laundryhub.core.domain.usecase.user

import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UserUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var userUseCase: UserUseCase

    @Before
    fun setUp() {
        authRepository = mock()
        userUseCase = UserUseCase(authRepository)
    }

    @Test
    fun getCurrentUser_shouldReturnCurrentUserFromRepository() {
        val user = User("uid1", "Alicia", "alicia@mail.com", "https://photo")
        whenever(authRepository.getCurrentUser()).thenReturn(user)

        val result = userUseCase.getCurrentUser()
        assertEquals(user, result)
    }

    @Test
    fun getCurrentUser_shouldReturnNullWhenRepositoryReturnsNull() {
        whenever(authRepository.getCurrentUser()).thenReturn(null)

        val result = userUseCase.getCurrentUser()
        assertNull(result)
    }

    @Test
    fun signOut_callsRepositorySignOutAndReturnTrue() {
        runBlocking {
            whenever(authRepository.signOut()).thenReturn(true)

            val result = userUseCase.signOut()
            assertTrue(result)
            verify(authRepository).signOut()
        }
    }

    @Test
    fun signOut_returnsFalseIfRepositoryReturnsFalse() {
        runBlocking {
            whenever(authRepository.signOut()).thenReturn(false)

            val result = userUseCase.signOut()
            assertFalse(result)
            verify(authRepository).signOut()
        }
    }
}