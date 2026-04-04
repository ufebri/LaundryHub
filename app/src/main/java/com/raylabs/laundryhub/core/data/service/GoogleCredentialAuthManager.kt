package com.raylabs.laundryhub.core.data.service

import android.content.Context

interface GoogleCredentialAuthManager {
    suspend fun signIn(activityContext: Context): GoogleCredentialAuthResult
    suspend fun clearCredentialState()
}
