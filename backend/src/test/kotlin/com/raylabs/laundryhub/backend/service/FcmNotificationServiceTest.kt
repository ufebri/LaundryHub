package com.raylabs.laundryhub.backend.service

import kotlin.test.Test
import kotlin.test.assertNotNull

class FcmNotificationServiceTest {

    @Test
    fun `service can be constructed and handles missing google service account json gracefully`() {
        // Constructing the service shouldn't throw any exceptions even if env vars are missing
        val service = FcmNotificationService()
        assertNotNull(service)
    }

    @Test
    fun `sendReminderNotification returns immediately if tokens list is empty`() {
        val service = FcmNotificationService()
        // Should not throw even with empty token list
        service.sendReminderNotification(emptyList(), "123", "John Doe", "Rp30.000")
    }
}
