package com.raylabs.laundryhub.core.domain.usecase.auth

import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SignInWithGoogleUseCaseTest {
    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: SignInWithGoogleUseCase

    @Before
    fun setUp() {
        authRepository = mock()
        useCase = SignInWithGoogleUseCase(authRepository)
    }

    @Test
    fun `invoke returns user when signInWithGoogle returns user`() = runTest {
        val user = User("uid123", "Test User", "test@email.com", "url")
        whenever(authRepository.signInWithGoogle("token")).thenReturn(user)
        val result = useCase.invoke("token")
        assertEquals(user, result)
    }

    @Test
    fun `invoke returns null when signInWithGoogle returns null`() = runTest {
        whenever(authRepository.signInWithGoogle("token")).thenReturn(null)
        val result = useCase.invoke("token")
        assertNull(result)
    }
}

