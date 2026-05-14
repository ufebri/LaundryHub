package com.raylabs.laundryhub.core.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.raylabs.laundryhub.core.domain.model.fcm.DeviceTokenRequest
import com.raylabs.laundryhub.shared.network.HttpClientProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceTokenManager(private val baseUrl: String) {
    private val client: HttpClient = HttpClientProvider.createClient()

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

    private fun sendTokenToBackend(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.post("$baseUrl/api/notifications/token") {
                    contentType(ContentType.Application.Json)
                    setBody(DeviceTokenRequest(token))
                }
                Log.d("DeviceTokenManager", "Token sent to backend successfully.")
            } catch (e: Exception) {
                Log.e("DeviceTokenManager", "Failed to send token to backend: ${e.message}")
            }
        }
    }
}
