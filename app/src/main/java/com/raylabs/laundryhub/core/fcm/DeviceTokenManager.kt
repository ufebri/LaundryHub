package com.raylabs.laundryhub.core.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.raylabs.laundryhub.core.domain.config.BackendConfigProvider
import com.raylabs.laundryhub.core.domain.model.fcm.DeviceTokenRequest
import com.raylabs.laundryhub.shared.network.HttpClientProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceTokenManager @Inject constructor(
    private val backendConfigProvider: BackendConfigProvider
) {
    private var client: HttpClient = HttpClientProvider.createClient()

    internal constructor(
        client: HttpClient,
        backendConfigProvider: BackendConfigProvider
    ) : this(backendConfigProvider) {
        this.client = client
    }

    fun fetchAndRegisterToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("DeviceTokenManager", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("DeviceTokenManager", "FCM Token: $token")
            sendTokenToBackend(token)
        }
    }

    fun sendTokenToBackend(token: String, refreshBackendConfig: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            sendTokenToBackendNow(token, refreshBackendConfig)
        }
    }

    internal suspend fun sendTokenToBackendNow(token: String, refreshBackendConfig: Boolean = false): Boolean {
        val sanitizedToken = token.trim()
        if (sanitizedToken.isBlank()) {
            Log.w("DeviceTokenManager", "Skipping blank FCM token registration.")
            return false
        }

        return try {
            val response: HttpResponse = client.post(tokenEndpoint(refreshBackendConfig)) {
                contentType(ContentType.Application.Json)
                setBody(DeviceTokenRequest(sanitizedToken))
            }
            if (response.status.value in 200..299) {
                Log.d("DeviceTokenManager", "Token sent to backend successfully.")
                true
            } else {
                val body = runCatching { response.bodyAsText() }.getOrDefault("")
                Log.e("DeviceTokenManager", "Failed to register token: HTTP ${response.status.value} $body")
                false
            }
        } catch (e: Exception) {
            Log.e("DeviceTokenManager", "Failed to send token to backend: ${e.message}")
            false
        }
    }

    private suspend fun tokenEndpoint(refreshBackendConfig: Boolean): String {
        if (refreshBackendConfig) {
            backendConfigProvider.refresh(force = false)
        }
        return "${backendConfigProvider.currentConfig().baseUrl.trimEnd('/')}/notifications/token"
    }
}
