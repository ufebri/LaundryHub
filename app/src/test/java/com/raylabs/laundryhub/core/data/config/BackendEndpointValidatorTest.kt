package com.raylabs.laundryhub.core.data.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackendEndpointValidatorTest {

    private val validator = BackendEndpointValidator()

    @Test
    fun `normalizeFallback handles standard http and https URLs`() {
        assertEquals("https://example.com/api", validator.normalizeFallback("https://example.com"))
        assertEquals("http://example.com/api", validator.normalizeFallback("http://example.com/"))
        assertEquals("https://example.com/custom/path", validator.normalizeFallback("https://example.com/custom/path"))
    }

    @Test
    fun `normalizeFallback returns trimmed original for invalid URIs`() {
        assertEquals("invalid-url-shape", validator.normalizeFallback("  invalid-url-shape  "))
    }

    @Test
    fun `normalizeRemote requires https scheme`() {
        assertEquals("https://example.com/api", validator.normalizeRemote("https://example.com"))
        assertNull(validator.normalizeRemote("http://example.com"))
    }

    @Test
    fun `normalizeRemote returns null for blank or invalid authority`() {
        assertNull(validator.normalizeRemote("   "))
        assertNull(validator.normalizeRemote("https:///path"))
    }

    @Test
    fun `normalizeRemote returns null for queries or fragments`() {
        assertNull(validator.normalizeRemote("https://example.com/api?query=1"))
        assertNull(validator.normalizeRemote("https://example.com/api#fragment"))
    }

    @Test
    fun `normalizeRemote normalizes scheme casing`() {
        assertEquals("https://EXAMPLE.COM/api", validator.normalizeRemote("HTTPS://EXAMPLE.COM"))
    }
}
