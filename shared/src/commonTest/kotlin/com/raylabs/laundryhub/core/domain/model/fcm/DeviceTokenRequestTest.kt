package com.raylabs.laundryhub.core.domain.model.fcm

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceTokenRequestTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testInitialization() {
        val request = DeviceTokenRequest(token = "fcm_token_123")
        assertEquals("fcm_token_123", request.token)
    }

    @Test
    fun testSerialization() {
        val request = DeviceTokenRequest(token = "fcm_token_123")
        val serialized = json.encodeToString(request)
        val deserialized = json.decodeFromString<DeviceTokenRequest>(serialized)
        assertEquals(request, deserialized)
    }
}
