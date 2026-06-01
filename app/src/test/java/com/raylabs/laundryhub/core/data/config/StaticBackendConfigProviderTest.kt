package com.raylabs.laundryhub.core.data.config

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StaticBackendConfigProviderTest {

    private val testBaseUrl = "https://laundry.example.com"
    private val provider = StaticBackendConfigProvider(testBaseUrl)

    @Test
    fun `refresh returns activeConfig`() = runTest {
        val config = provider.refresh()
        assertNotNull(config)
        assertEquals("https://laundry.example.com/api", config.baseUrl)
    }

    @Test
    fun `currentConfig returns activeConfig`() {
        val config = provider.currentConfig()
        assertNotNull(config)
        assertEquals("https://laundry.example.com/api", config.baseUrl)
    }

    @Test
    fun `candidateBaseUrls returns activeConfig baseUrl`() {
        val urls = provider.candidateBaseUrls()
        assertEquals(1, urls.size)
        assertEquals("https://laundry.example.com/api", urls[0])
    }

    @Test
    fun `activateBaseUrl updates activeConfig when valid`() {
        // Activate backup base URL
        provider.activateBaseUrl("https://backup.laundry.com")
        assertEquals("https://backup.laundry.com/api", provider.currentConfig().baseUrl)

        // Activate same fallback URL
        provider.activateBaseUrl("https://laundry.example.com/api")
        assertEquals("https://laundry.example.com/api", provider.currentConfig().baseUrl)
    }

    @Test
    fun `activateBaseUrl ignores invalid or unsafe URLs`() {
        // Activate unsafe HTTP URL (should be ignored by endpointValidator)
        provider.activateBaseUrl("http://unsafe.com/api")
        
        // baseUrl should remain the same
        assertEquals("https://laundry.example.com/api", provider.currentConfig().baseUrl)
    }
}
