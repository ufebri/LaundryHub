package com.raylabs.laundryhub.core.domain.repository

import com.raylabs.laundryhub.core.domain.model.auth.User

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): User?
    fun isUserLoggedIn(): Boolean
    fun getCurrentUser(): User?
    suspend fun signOut(): Boolean
}