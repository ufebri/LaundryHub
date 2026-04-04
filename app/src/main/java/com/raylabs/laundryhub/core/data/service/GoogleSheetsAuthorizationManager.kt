package com.raylabs.laundryhub.core.data.service

import android.content.Intent
import android.content.IntentSender

interface GoogleSheetsAuthorizationManager {
    fun getSignedInEmail(): String?
    suspend fun hasSheetsAccess(): Boolean
    suspend fun getAccessToken(): String?
    suspend fun getAuthorizationIntentSender(): IntentSender?
    fun handleAuthorizationResult(data: Intent?): Boolean
}
