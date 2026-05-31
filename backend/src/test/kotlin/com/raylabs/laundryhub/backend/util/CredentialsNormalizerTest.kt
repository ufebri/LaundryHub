package com.raylabs.laundryhub.backend.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CredentialsNormalizerTest {

    @Test
    fun `normalizes standard multi-line service account json with escaped newlines`() {
        val raw = """
            {
              "type": "service_account",
              "private_key": "-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC\n-----END PRIVATE KEY-----\n"
            }
        """.trimIndent()

        val clean = CredentialsNormalizer.cleanAndNormalizeServiceAccountJson(raw)
        
        val parsed = Json.parseToJsonElement(clean).jsonObject
        val privateKey = parsed["private_key"]?.jsonPrimitive?.content ?: ""
        
        assertTrue(privateKey.contains("-----BEGIN PRIVATE KEY-----\n"))
        assertTrue(privateKey.contains("\n-----END PRIVATE KEY-----\n"))
        // Verify no literal \n character sequence remains (only actual newlines)
        assertTrue(!privateKey.contains("\\n"))
    }

    @Test
    fun `normalizes double-escaped service account json`() {
        val raw = """
            {
              "type": "service_account",
              "private_key": "-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC\\n-----END PRIVATE KEY-----\\n"
            }
        """.trimIndent()

        val clean = CredentialsNormalizer.cleanAndNormalizeServiceAccountJson(raw)
        
        val parsed = Json.parseToJsonElement(clean).jsonObject
        val privateKey = parsed["private_key"]?.jsonPrimitive?.content ?: ""
        
        assertTrue(privateKey.contains("-----BEGIN PRIVATE KEY-----\n"))
        assertTrue(privateKey.contains("\n-----END PRIVATE KEY-----\n"))
        assertTrue(!privateKey.contains("\\n"))
    }

    @Test
    fun `normalizes quote-wrapped and escaped service account json`() {
        val raw = "\"{\\\"type\\\": \\\"service_account\\\", \\\"private_key\\\": \\\"-----BEGIN PRIVATE KEY-----\\\\nMIIEvg\\\\n-----END PRIVATE KEY-----\\\\n\\\"}\""

        val clean = CredentialsNormalizer.cleanAndNormalizeServiceAccountJson(raw)
        
        val parsed = Json.parseToJsonElement(clean).jsonObject
        val privateKey = parsed["private_key"]?.jsonPrimitive?.content ?: ""
        
        assertTrue(privateKey.contains("-----BEGIN PRIVATE KEY-----\n"))
        assertTrue(privateKey.contains("\n-----END PRIVATE KEY-----\n"))
        assertTrue(!privateKey.contains("\\n"))
        assertEquals("service_account", parsed["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun `normalizes service account json with single-quoted wrapping`() {
        val raw = "'{\"type\": \"service_account\", \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvg\\n-----END PRIVATE KEY-----\\n\"}'"

        val clean = CredentialsNormalizer.cleanAndNormalizeServiceAccountJson(raw)
        
        val parsed = Json.parseToJsonElement(clean).jsonObject
        val privateKey = parsed["private_key"]?.jsonPrimitive?.content ?: ""
        
        assertTrue(privateKey.contains("-----BEGIN PRIVATE KEY-----\n"))
        assertTrue(privateKey.contains("\n-----END PRIVATE KEY-----\n"))
        assertTrue(!privateKey.contains("\\n"))
    }
}
