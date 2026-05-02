package com.raylabs.laundryhub.core.data.service

import android.content.Intent
import android.content.IntentSender
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GoogleSheetsAuthorizationManagerImpl @Inject constructor(
    private val authorizationClient: AuthorizationClient,
    private val googleSheetsAccountProvider: GoogleSheetsAccountProvider
) : GoogleSheetsAuthorizationManager {

    companion object {
        private const val TAG = "GoogleSheetsAuth"
    }

    override fun getSignedInEmail(): String? = googleSheetsAccountProvider.getSignedInEmail()

    override suspend fun hasSheetsAccess(): Boolean {
        // Karena sekarang menggunakan Backend Ktor + Service Account, 
        // klien Android tidak perlu lagi mengecek izin spreadsheet secara manual.
        return true 
    }

    override suspend fun getAccessToken(): String? {
        // Access token tidak lagi dikelola di sisi klien untuk operasi spreadsheet.
        return ""
    }

    override suspend fun getAuthorizationIntentSender(): IntentSender? {
        // Langsung return null karena tidak butuh scope tambahan lagi.
        return null
    }

    override fun handleAuthorizationResult(data: Intent?): Boolean {
        return true
    }

    private suspend fun authorizeCurrentAccount(): AuthorizationResult? {
        val signedInEmail = getSignedInEmail() ?: return null
        val request = AuthorizationRequest.builder()
            .build() // Tanpa Scopes tambahan

        return runCatching {
            Log.d(TAG, "authorizeCurrentAccount started for email=$signedInEmail")
            authorizationClient.authorize(request).await()
        }.onFailure { throwable ->
            logAuthorizationFailure("authorizeCurrentAccount email=$signedInEmail", throwable)
        }.getOrThrow()
    }

    private fun List<String>.hasRequiredSpreadsheetScopes(): Boolean = true

    private fun logAuthorizationFailure(prefix: String, throwable: Throwable) {
        if (throwable is ApiException) {
            Log.e(
                TAG,
                "$prefix failed with statusCode=${throwable.statusCode} message=${throwable.message}",
                throwable
            )
        } else {
            Log.e(TAG, "$prefix failed: ${throwable.message}", throwable)
        }
    }
}
