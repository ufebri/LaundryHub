package com.raylabs.laundryhub.core.domain.usecase.auth

import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.repository.AuthRepository

class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): User? {
        return authRepository.signInWithGoogle(idToken)
    }
}