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

    @Test
    fun `handles invalid json with fallback to regex`() {
        val raw = "invalid_json_with_\\n_escaped_newlines"
        val clean = CredentialsNormalizer.cleanAndNormalizeServiceAccountJson(raw)
        assertEquals("invalid_json_with_\n_escaped_newlines", clean)
    }

    @Test
    fun `handles carriage returns and non-matching quotes`() {
        val raw1 = "\"non-matching-quote"
        val clean1 = CredentialsNormalizer.cleanAndNormalizeServiceAccountJson(raw1)
        assertEquals("\"non-matching-quote", clean1)

        val raw2 = "'non-matching-quote"
        val clean2 = CredentialsNormalizer.cleanAndNormalizeServiceAccountJson(raw2)
        assertEquals("'non-matching-quote", clean2)

        val rawWithCr = """
            {
              "type": "service_account",
              "private_key": "line1\\rline2\\\\rline3"
            }
        """.trimIndent()
        val cleanWithCr = CredentialsNormalizer.cleanAndNormalizeServiceAccountJson(rawWithCr)
        val parsed = Json.parseToJsonElement(cleanWithCr).jsonObject
        val privateKey = parsed["private_key"]?.jsonPrimitive?.content ?: ""
        println("DEBUG_CR: " + privateKey.replace("\r", "[CR]").replace("\n", "[LF]"))
        assertTrue(privateKey.contains("line1\rline2\rline3"))
    }

}


