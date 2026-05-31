package com.raylabs.laundryhub.backend.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory

object CredentialsNormalizer {
    private val logger = LoggerFactory.getLogger(CredentialsNormalizer::class.java)

    /**
     * Safely cleans and normalizes a Google Service Account credentials JSON string.
     * It handles:
     * 1. Surrounding double or single quotes added by cloud dashboard env var interfaces.
     * 2. Escaped quotes (\") in case the JSON was stringified and nested.
     * 3. Normalizes all forms of escaped newlines (\\n, \n, literal newlines) inside the private_key value
     *    to standard single-escaped newlines expected by standard JSON parsers, resulting in proper
     *    RSA signing PEM keys.
     */
    fun cleanAndNormalizeServiceAccountJson(rawJson: String): String {
        var trimmed = rawJson.trim()
        
        // Remove surrounding quotes if wrapped
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length - 1)
        }
        if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
            trimmed = trimmed.substring(1, trimmed.length - 1)
        }

        // Unescape double quotes if the whole JSON string was escaped
        if (trimmed.contains("\\\"")) {
            trimmed = trimmed.replace("\\\"", "\"")
        }

        try {
            // Parse with kotlinx.serialization lenient Json parser
            val jsonElement = Json { isLenient = true; ignoreUnknownKeys = true }.parseToJsonElement(trimmed)
            val jsonObject = jsonElement.jsonObject

            // Rebuild the JSON object, specifically cleaning up the private_key field
            val rebuilt = buildJsonObject {
                jsonObject.forEach { (key, value) ->
                    if (key == "private_key") {
                        val rawKey = value.jsonPrimitive.content
                        // Standardize the private key string in memory:
                        // Convert literal "\\n", "\n", or any other escaped variations to actual newlines (\n)
                        val cleanKey = rawKey
                            .replace("\\\\n", "\n")
                            .replace("\\n", "\n")
                            .replace("\\\\r", "\r")
                            .replace("\\r", "\r")
                        put(key, cleanKey)
                    } else {
                        put(key, value)
                    }
                }
            }
            return rebuilt.toString()
        } catch (e: Exception) {
            logger.warn("Failed to parse GOOGLE_SERVICE_ACCOUNT_JSON as JSON, falling back to simple regex replacement: {}", e.message)
            // Fallback to simple regex if JSON parsing fails
            var fallback = trimmed
            fallback = fallback.replace("\\\\n", "\n")
            fallback = fallback.replace("\\n", "\n")
            return fallback
        }
    }
}
