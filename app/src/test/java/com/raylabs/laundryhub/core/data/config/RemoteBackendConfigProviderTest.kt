package com.raylabs.laundryhub.core.data.config

import com.raylabs.laundryhub.core.data.config.RemoteBackendConfigProvider.Companion.KEY_API_BASE_URL
import com.raylabs.laundryhub.core.data.config.RemoteBackendConfigProvider.Companion.KEY_API_CONFIG_VERSION
import com.raylabs.laundryhub.core.data.config.RemoteBackendConfigProvider.Companion.KEY_API_MAINTENANCE_ENABLED
import com.raylabs.laundryhub.core.data.config.RemoteBackendConfigProvider.Companion.KEY_API_MAINTENANCE_MESSAGE
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteBackendConfigProviderTest {

    private val endpointValidator = BackendEndpointValidator()

    @Test
    fun `refresh uses remote https base url when present`() = runTest {
        val source = FakeRemoteConfigSource(
            strings = mutableMapOf(KEY_API_BASE_URL to "https://new.example.com")
        )
        val provider = provider(source)

        val config = provider.refresh()

        assertEquals("https://new.example.com/api", config.baseUrl)
        assertEquals(
            listOf("https://new.example.com/api", FALLBACK_BASE_URL),
            provider.candidateBaseUrls()
        )
    }

    @Test
    fun `refresh falls back when remote base url is blank or unsafe`() = runTest {
        val source = FakeRemoteConfigSource(
            strings = mutableMapOf(KEY_API_BASE_URL to "http://unsafe.example.com/api")
        )
        val provider = provider(source)

        val config = provider.refresh()

        assertEquals(FALLBACK_BASE_URL, config.baseUrl)
        assertEquals(listOf(FALLBACK_BASE_URL), provider.candidateBaseUrls())
    }

    @Test
    fun `refresh exposes maintenance flag message and version`() = runTest {
        val source = FakeRemoteConfigSource(
            strings = mutableMapOf(
                KEY_API_BASE_URL to "https://new.example.com/api",
                KEY_API_MAINTENANCE_MESSAGE to "LaundryHub is being updated."
            ),
            booleans = mutableMapOf(KEY_API_MAINTENANCE_ENABLED to true),
            longs = mutableMapOf(KEY_API_CONFIG_VERSION to 7L)
        )
        val provider = provider(source)

        val config = provider.refresh()

        assertTrue(config.maintenanceEnabled)
        assertEquals("LaundryHub is being updated.", config.maintenanceMessage)
        assertEquals(7L, config.configVersion)
    }

    @Test
    fun `manual refresh forwards force flag to remote config`() = runTest {
        val source = FakeRemoteConfigSource()
        val provider = provider(source)

        provider.refresh(force = true)

        assertEquals(listOf(true), source.forceRequests)
    }

    @Test
    fun `activateBaseUrl only accepts normalized trusted candidates`() {
        val provider = provider(FakeRemoteConfigSource())

        provider.activateBaseUrl("https://backup.example.com")
        assertEquals("https://backup.example.com/api", provider.currentConfig().baseUrl)

        provider.activateBaseUrl("http://unsafe.example.com/api")
        assertEquals("https://backup.example.com/api", provider.currentConfig().baseUrl)
        assertFalse(provider.currentConfig().maintenanceEnabled)
        assertNull(provider.currentConfig().maintenanceMessage)
    }

    private fun provider(source: RemoteConfigSource): RemoteBackendConfigProvider {
        return RemoteBackendConfigProvider(
            remoteConfigSource = source,
            endpointValidator = endpointValidator,
            fallbackBaseUrl = FALLBACK_BASE_URL
        )
    }

    private class FakeRemoteConfigSource(
        private val strings: MutableMap<String, String> = mutableMapOf(),
        private val booleans: MutableMap<String, Boolean> = mutableMapOf(),
        private val longs: MutableMap<String, Long> = mutableMapOf()
    ) : RemoteConfigSource {
        val forceRequests = mutableListOf<Boolean>()

        override suspend fun refresh(force: Boolean): Boolean {
            forceRequests += force
            return true
        }

        override fun getString(key: String): String = strings[key].orEmpty()

        override fun getBoolean(key: String): Boolean = booleans[key] == true

        override fun getLong(key: String): Long = longs[key] ?: 0L
    }

    private companion object {
        const val FALLBACK_BASE_URL = "https://fallback.example.com/api"
    }
}
