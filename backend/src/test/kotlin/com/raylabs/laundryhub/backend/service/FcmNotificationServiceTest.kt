package com.raylabs.laundryhub.backend.service

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.ByteArrayInputStream
import java.security.KeyPairGenerator
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FcmNotificationServiceTest {

    private fun generateValidPkcs8Key(): String {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val privateKeyBytes = kpg.generateKeyPair().private.encoded
        val base64 = Base64.getEncoder().encodeToString(privateKeyBytes)
        val pemContent = base64.chunked(64).joinToString("\n")
        return "-----BEGIN PRIVATE KEY-----\n$pemContent\n-----END PRIVATE KEY-----\n"
    }

    private fun initDefaultFirebaseApp() {
        if (FirebaseApp.getApps().isEmpty()) {
            val key = generateValidPkcs8Key().replace("\n", "\\n")
            val dummyJson = """
            {
              "type": "service_account",
              "project_id": "test-project",
              "private_key_id": "123",
              "private_key": "$key",
              "client_email": "test@test-project.iam.gserviceaccount.com",
              "client_id": "123",
              "auth_uri": "https://accounts.google.com/o/oauth2/auth",
              "token_uri": "https://oauth2.googleapis.com/token",
              "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
              "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/test%40test-project.iam.gserviceaccount.com"
            }
            """.trimIndent()
            val options = FirebaseOptions.builder()
                .setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(ByteArrayInputStream(dummyJson.toByteArray())))
                .setProjectId("test-project")
                .build()
            FirebaseApp.initializeApp(options)
        }
    }

    @Test
    fun `service can be constructed and handles missing google service account json gracefully`() {
        val service = FcmNotificationService()
        assertNotNull(service)
    }

    @Test
    fun `sendReminderNotification returns immediately if tokens list is empty`() {
        val service = FcmNotificationService()
        service.sendReminderNotification(emptyList(), "123", "John Doe", "Rp30.000")
    }

    @Test
    fun `sendReminderNotification executes fully when firebase is initialized`() {
        initDefaultFirebaseApp()
        val service = FcmNotificationService()
        
        val isInitField = FcmNotificationService::class.java.getDeclaredField("isInitialized")
        isInitField.isAccessible = true
        val isInitialized = isInitField.get(service) as Boolean
        assertTrue(isInitialized)

        service.sendReminderNotification(listOf("token-1", "token-2"), "ORD-123", "John Doe", "Rp50.000")
    }
}
