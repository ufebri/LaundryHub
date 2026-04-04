package com.raylabs.laundryhub.core.data.service

import android.accounts.Account
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class GoogleSheetsAccountProviderImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : GoogleSheetsAccountProvider {

    override fun getAuthorizedAccount(): Account? {
        val email = firebaseAuth.currentUser?.email ?: return null
        return Account(email, "com.google")
    }

    override fun getSignedInEmail(): String? = firebaseAuth.currentUser?.email
}
