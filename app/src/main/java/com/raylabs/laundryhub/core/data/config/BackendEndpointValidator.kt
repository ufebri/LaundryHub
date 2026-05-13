package com.raylabs.laundryhub.core.data.config

import java.net.URI
import java.util.Locale

class BackendEndpointValidator {

    fun normalizeFallback(rawBaseUrl: String): String {
        return normalizeBaseUrlShape(rawBaseUrl, requireHttps = false)
            ?: rawBaseUrl.trim().trimEnd('/')
    }

    fun normalizeRemote(rawBaseUrl: String): String? {
        return normalizeBaseUrlShape(rawBaseUrl, requireHttps = true)
    }

    private fun normalizeBaseUrlShape(rawBaseUrl: String, requireHttps: Boolean): String? {
        val trimmed = rawBaseUrl.trim().trimEnd('/')
        if (trimmed.isBlank()) return null

        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.US) ?: return null
        val authority = uri.rawAuthority?.takeIf { it.isNotBlank() } ?: return null

        if (requireHttps && scheme != "https") return null
        if (!requireHttps && scheme !in setOf("http", "https")) return null
        if (!uri.rawQuery.isNullOrBlank() || !uri.rawFragment.isNullOrBlank()) return null

        val rawPath = uri.rawPath.orEmpty().trimEnd('/')
        return if (rawPath.isBlank() || rawPath == "/") {
            "$scheme://$authority/api"
        } else {
            "$scheme://$authority$rawPath"
        }
    }
}
