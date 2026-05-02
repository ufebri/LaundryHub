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
        private const val SPREADSHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"
        private const val TAG = "GoogleSheetsAuth"
    }

    override fun getSignedInEmail(): String? = googleSheetsAccountProvider.getSignedInEmail()

    override suspend fun hasSheetsAccess(): Boolean {
        val signedInEmail = getSignedInEmail()
        if (signedInEmail.isNullOrBlank()) {
            Log.d(TAG, "hasSheetsAccess skipped because there is no signed-in Google email")
            return false
        }

        val result = authorizeCurrentAccount() ?: return false
        val hasAccessToken = !result.accessToken.isNullOrBlank()
        Log.d(
            TAG,
            "hasSheetsAccess email=$signedInEmail hasResolution=${result.hasResolution()} accessTokenPresent=$hasAccessToken grantedScopes=${result.grantedScopes}"
        )
        return !result.hasResolution() &&
            hasAccessToken &&
            result.grantedScopes.hasRequiredSpreadsheetScopes()
    }

    override suspend fun getAccessToken(): String? {
        val signedInEmail = getSignedInEmail()
        if (signedInEmail.isNullOrBlank()) {
            Log.w(TAG, "getAccessToken called without signed-in email")
            return null
        }

        val result = authorizeCurrentAccount() ?: return null
        val accessToken = result.accessToken?.takeIf { it.isNotBlank() }
        Log.d(
            TAG,
            "getAccessToken email=$signedInEmail hasResolution=${result.hasResolution()} accessTokenPresent=${accessToken != null} grantedScopes=${result.grantedScopes}"
        )
        return if (result.hasResolution()) null else accessToken
    }

    override suspend fun getAuthorizationIntentSender(): IntentSender? {
        val signedInEmail = getSignedInEmail()
        if (signedInEmail.isNullOrBlank()) {
            Log.w(TAG, "getAuthorizationIntentSender called without signed-in email")
            return null
        }

        val result = authorizeCurrentAccount() ?: return null
        Log.d(
            TAG,
            "getAuthorizationIntentSender email=$signedInEmail hasResolution=${result.hasResolution()} grantedScopes=${result.grantedScopes}"
        )
        return if (result.hasResolution()) {
            result.pendingIntent?.intentSender
        } else {
            null
        }
    }

    override fun handleAuthorizationResult(data: Intent?): Boolean {
        if (data == null) {
            Log.w(TAG, "handleAuthorizationResult received null intent data")
            return false
        }

        return runCatching {
            authorizationClient.getAuthorizationResultFromIntent(data)
                .let { result ->
                    !result.accessToken.isNullOrBlank() &&
                        result.grantedScopes.hasRequiredSpreadsheetScopes()
                }
        }.onSuccess { granted ->
            Log.d(TAG, "handleAuthorizationResult granted=$granted")
        }.onFailure { throwable ->
            logAuthorizationFailure("handleAuthorizationResult", throwable)
        }.getOrDefault(false)
    }

    private suspend fun authorizeCurrentAccount(): AuthorizationResult? {
        val signedInEmail = getSignedInEmail() ?: return null
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope(SPREADSHEETS_SCOPE),
                    Scope(GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE)
                )
            )
            .build()

        return runCatching {
            Log.d(TAG, "authorizeCurrentAccount started for email=$signedInEmail")
            authorizationClient.authorize(request).await()
        }.onFailure { throwable ->
            logAuthorizationFailure("authorizeCurrentAccount email=$signedInEmail", throwable)
        }.getOrThrow()
    }

    private fun List<String>.hasRequiredSpreadsheetScopes(): Boolean =
        contains(SPREADSHEETS_SCOPE) &&
            contains(GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE)

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
