package com.raylabs.laundryhub.core.data.firebase

import com.raylabs.laundryhub.core.domain.model.auth.User

interface FirebaseAuthDataSource {
    suspend fun signInWithGoogle(idToken: String): User?
    fun isUserLoggedIn(): Boolean
    fun getCurrentUser(): User?
    suspend fun signOut(): Boolean
}