package com.raylabs.laundryhub.backend.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.raylabs.laundryhub.backend.util.CredentialsNormalizer
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream

class FcmNotificationService(private val env: Map<String, String> = System.getenv()) {

    private val logger = LoggerFactory.getLogger(FcmNotificationService::class.java)
    private var isInitialized = false

    init {
        initializeFirebase()
    }

    private fun initializeFirebase() {
        if (FirebaseApp.getApps().isNotEmpty()) {
            isInitialized = true
            return
        }

        try {
            val rawJson = env["GOOGLE_SERVICE_ACCOUNT_JSON"]
            if (rawJson.isNullOrBlank()) {
                logger.warn("GOOGLE_SERVICE_ACCOUNT_JSON not found. FCM Push Notifications will be disabled.")
                return
            }
            
            val cleanJson = CredentialsNormalizer.cleanAndNormalizeServiceAccountJson(rawJson)

            val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(cleanJson.toByteArray()))
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build()

            FirebaseApp.initializeApp(options)
            isInitialized = true
            logger.info("Firebase Admin SDK initialized successfully.")
        } catch (e: Exception) {
            logger.error("Failed to initialize Firebase Admin SDK: ${e.message}")
        }
    }

    fun sendReminderNotification(tokens: List<String>, orderId: String, name: String, amount: String) {
        if (!isInitialized || tokens.isEmpty()) return

        val title = "Order Reminder"
        val body = "Unpaid order for $name ($amount). Tap to view."
        
        val data = mapOf(
            "orderId" to orderId
        )

        sendMulticastNotification(tokens, title, body, "REMINDER", data)
    }

    private fun sendMulticastNotification(tokens: List<String>, title: String, body: String, type: String, data: Map<String, String>) {
        try {
            tokens.forEach { token ->
                val messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(
                        Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build()
                    )
                    .putData("type", type)
                
                data.forEach { (key, value) -> messageBuilder.putData(key, value) }

                FirebaseMessaging.getInstance().sendAsync(messageBuilder.build())
            }
            logger.info("FCM push notifications ($type) sent to ${tokens.size} devices.")
        } catch (e: Exception) {
            logger.error("Failed to send FCM notification: ${e.message}")
        }
    }
}
