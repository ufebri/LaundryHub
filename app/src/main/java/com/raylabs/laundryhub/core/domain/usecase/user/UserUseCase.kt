package com.raylabs.laundryhub.core.domain.usecase.user

import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.repository.AuthRepository

class UserUseCase(private val authRepository: AuthRepository) {

    // Function to get current user
    fun getCurrentUser(): User? = authRepository.getCurrentUser()

    // Function to sign out
    suspend fun signOut(): Boolean = authRepository.signOut()

}