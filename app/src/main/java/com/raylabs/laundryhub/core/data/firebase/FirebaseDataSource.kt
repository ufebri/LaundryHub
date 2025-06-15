package com.raylabs.laundryhub.core.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.raylabs.laundryhub.core.domain.model.auth.User
import kotlinx.coroutines.tasks.await

class FirebaseDataSource(
    private val firebaseAuth: FirebaseAuth
) : FirebaseAuthDataSource {

    override suspend fun signInWithGoogle(idToken: String): User? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        val firebaseUser = authResult.user
        return firebaseUser?.let {
            User(
                uid = it.uid,
                displayName = it.displayName,
                email = it.email,
                urlPhoto = it.photoUrl.toString()
            )
        }
    }

    override fun isUserLoggedIn(): Boolean = (firebaseAuth.currentUser != null)

    override fun getCurrentUser(): User? {
        val currentUser = firebaseAuth.currentUser
        return currentUser?.let {
            User(
                uid = it.uid,
                displayName = it.displayName,
                email = it.email,
                urlPhoto = it.photoUrl.toString()
            )
        }
    }

    override suspend fun signOut(): Boolean {
        firebaseAuth.signOut()
        return true
    }

}
