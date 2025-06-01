package com.raylabs.laundryhub.core.domain.usecase.auth

import com.raylabs.laundryhub.core.domain.repository.AuthRepository

class CheckUserLoggedInUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Boolean {
        return authRepository.isUserLoggedIn()
    }
}