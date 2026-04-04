package com.raylabs.laundryhub.core.data.service

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.raylabs.laundryhub.BuildConfig
import javax.inject.Inject

class GoogleCredentialAuthManagerImpl @Inject constructor(
    private val credentialManager: CredentialManager
) : GoogleCredentialAuthManager {

    override suspend fun signIn(activityContext: Context): GoogleCredentialAuthResult {
        Log.d(
            TAG,
            "Starting Credential Manager Google sign-in package=${activityContext.packageName} webClientIdSuffix=${BuildConfig.WEB_CLIENT_ID.takeLast(12)}"
        )
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetSignInWithGoogleOption.Builder(
                    serverClientId = BuildConfig.WEB_CLIENT_ID
                ).build()
            )
            .build()

        val response = credentialManager.getCredential(
            context = activityContext,
            request = request
        )

        val credential = response.credential
        Log.d(TAG, "Credential Manager returned credentialType=${credential.type}")
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            Log.e(TAG, "Unsupported credential response type=${credential.type}")
            throw IllegalStateException("Unsupported Google credential response.")
        }

        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        Log.d(TAG, "Google sign-in completed for email=${googleCredential.id}")
        return GoogleCredentialAuthResult(
            idToken = googleCredential.idToken,
            email = googleCredential.id
        )
    }

    override suspend fun clearCredentialState() {
        Log.d(TAG, "Clearing Credential Manager state")
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }

    private companion object {
        const val TAG = "GoogleSignIn"
    }
}
