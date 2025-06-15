package com.raylabs.laundryhub.core.data.firebase

import com.raylabs.laundryhub.core.domain.model.auth.User

class FakeFirebaseAuthDataSource : FirebaseAuthDataSource {
    var user: User? = User("123", "John", "john@example.com", "http://image.com")

    override suspend fun signInWithGoogle(idToken: String): User? = user

    override fun isUserLoggedIn(): Boolean = true

    override fun getCurrentUser(): User? = user

    override suspend fun signOut(): Boolean {
        user = null
        return true
    }
}