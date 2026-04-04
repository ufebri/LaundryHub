package com.raylabs.laundryhub.core.data.service

import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleCredentialAuthResultTest {

    @Test
    fun `result keeps id token and email`() {
        val result = GoogleCredentialAuthResult(
            idToken = "token-123",
            email = "owner@laundryhub.com"
        )

        assertEquals("token-123", result.idToken)
        assertEquals("owner@laundryhub.com", result.email)
    }
}
