package com.raylabs.laundryhub.core.domain.usecase.auth

import com.raylabs.laundryhub.core.domain.repository.AuthRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CheckUserLoggedInUseCaseTest {
    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: CheckUserLoggedInUseCase

    @Before
    fun setUp() {
        authRepository = mock()
        useCase = CheckUserLoggedInUseCase(authRepository)
    }

    @Test
    fun `invoke returns true when user is logged in`() {
        whenever(authRepository.isUserLoggedIn()).thenReturn(true)
        assertTrue(useCase())
    }

    @Test
    fun `invoke returns false when user is not logged in`() {
        whenever(authRepository.isUserLoggedIn()).thenReturn(false)
        assertFalse(useCase())
    }
}

