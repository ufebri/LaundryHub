package com.raylabs.laundryhub.core.data.service

import android.content.Intent
import android.content.IntentSender
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GoogleSheetsAuthorizationManagerImpl @Inject constructor(
    private val authorizationClient: AuthorizationClient,
    private val googleSheetsAccountProvider: GoogleSheetsAccountProvider
) : GoogleSheetsAuthorizationManager {

    @Volatile
    private var cachedEmail: String? = null

    @Volatile
    private var cachedAccessToken: String? = null

    override fun getSignedInEmail(): String? = googleSheetsAccountProvider.getSignedInEmail()

    override suspend fun hasSheetsAccess(): Boolean {
        val signedInEmail = getSignedInEmail()
        if (signedInEmail.isNullOrBlank()) {
            clearAuthorizationCache()
            Log.d(TAG, "hasSheetsAccess skipped because there is no signed-in Google email")
            return false
        }

        invalidateCacheIfAccountChanged(signedInEmail)
        cachedAccessToken?.let {
            Log.d(TAG, "hasSheetsAccess satisfied from cached token for email=$signedInEmail")
            return true
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
            clearAuthorizationCache()
            Log.w(TAG, "getAccessToken called without signed-in email")
            return null
        }

        invalidateCacheIfAccountChanged(signedInEmail)
        cachedAccessToken?.let {
            Log.d(TAG, "getAccessToken returned cached token for email=$signedInEmail")
            return it
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
            clearAuthorizationCache()
            Log.w(TAG, "getAuthorizationIntentSender called without signed-in email")
            return null
        }

        invalidateCacheIfAccountChanged(signedInEmail)
        if (cachedAccessToken != null) {
            Log.d(TAG, "getAuthorizationIntentSender skipped because token is already cached for email=$signedInEmail")
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
                    val granted = !result.accessToken.isNullOrBlank() &&
                        result.grantedScopes.hasRequiredSpreadsheetScopes()
                    if (granted) {
                        cacheAuthorization(
                            signedInEmail = getSignedInEmail(),
                            accessToken = result.accessToken
                        )
                    }
                    granted
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
                    Scope(SheetsScopes.SPREADSHEETS),
                    Scope(GoogleSheetService.DRIVE_METADATA_READONLY_SCOPE)
                )
            )
            .build()

        return runCatching {
            Log.d(TAG, "authorizeCurrentAccount started for email=$signedInEmail")
            authorizationClient.authorize(request).await()
        }.onSuccess { result ->
            if (!result.hasResolution() && result.grantedScopes.hasRequiredSpreadsheetScopes()) {
                cacheAuthorization(
                    signedInEmail = signedInEmail,
                    accessToken = result.accessToken
                )
            }
        }.onFailure { throwable ->
            logAuthorizationFailure("authorizeCurrentAccount email=$signedInEmail", throwable)
        }.getOrThrow()
    }

    private fun invalidateCacheIfAccountChanged(signedInEmail: String) {
        if (cachedEmail != null && cachedEmail != signedInEmail) {
            Log.d(TAG, "Clearing cached Google Sheets token because signed-in email changed from $cachedEmail to $signedInEmail")
            clearAuthorizationCache()
        }
    }

    private fun cacheAuthorization(signedInEmail: String?, accessToken: String?) {
        val normalizedEmail = signedInEmail?.takeIf { it.isNotBlank() } ?: return
        val normalizedToken = accessToken?.takeIf { it.isNotBlank() } ?: return
        cachedEmail = normalizedEmail
        cachedAccessToken = normalizedToken
        Log.d(TAG, "Cached Google Sheets token for email=$normalizedEmail")
    }

    private fun clearAuthorizationCache() {
        cachedEmail = null
        cachedAccessToken = null
    }

    private fun List<String>.hasRequiredSpreadsheetScopes(): Boolean =
        contains(SheetsScopes.SPREADSHEETS) &&
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

    private companion object {
        const val TAG = "SheetsAuth"
    }
}
