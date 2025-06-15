package com.raylabs.laundryhub.core.data.firebase

import com.raylabs.laundryhub.core.domain.model.auth.User
import com.raylabs.laundryhub.core.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val firebaseDataSource: FirebaseAuthDataSource
) : AuthRepository {

    override suspend fun signInWithGoogle(idToken: String): User? =
        firebaseDataSource.signInWithGoogle(idToken)

    override fun isUserLoggedIn(): Boolean = firebaseDataSource.isUserLoggedIn()

    override fun getCurrentUser(): User? = firebaseDataSource.getCurrentUser()

    override suspend fun signOut(): Boolean = firebaseDataSource.signOut()

}
