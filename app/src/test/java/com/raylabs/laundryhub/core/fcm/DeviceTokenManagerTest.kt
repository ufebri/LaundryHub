package com.raylabs.laundryhub.core.fcm

import com.raylabs.laundryhub.core.domain.config.BackendConfig
import com.raylabs.laundryhub.core.domain.config.BackendConfigProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceTokenManagerTest {

    @Test
    fun `sendTokenToBackendNow posts to active api root without duplicating api path`() = runTest {
        val requestedUrls = mutableListOf<String>()
        val manager = DeviceTokenManager(
            client = mockClient { url ->
                requestedUrls += url
                HttpStatusCode.OK
            },
            backendConfigProvider = MutableBackendConfigProvider("https://backend.example.test/api")
        )

        val success = manager.sendTokenToBackendNow(" fcm-token ")

        assertTrue(success)
        assertEquals(listOf("https://backend.example.test/api/notifications/token"), requestedUrls)
    }

    @Test
    fun `sendTokenToBackendNow returns false for blank token without network call`() = runTest {
        val requestedUrls = mutableListOf<String>()
        val manager = DeviceTokenManager(
            client = mockClient { url ->
                requestedUrls += url
                HttpStatusCode.OK
            },
            backendConfigProvider = MutableBackendConfigProvider("https://backend.example.test/api")
        )

        val success = manager.sendTokenToBackendNow("   ")

        assertFalse(success)
        assertEquals(emptyList<String>(), requestedUrls)
    }

    @Test
    fun `sendTokenToBackendNow returns false when backend rejects token`() = runTest {
        val manager = DeviceTokenManager(
            client = mockClient { _ -> HttpStatusCode.BadRequest },
            backendConfigProvider = MutableBackendConfigProvider("https://backend.example.test/api")
        )

        val success = manager.sendTokenToBackendNow("fcm-token")

        assertFalse(success)
    }

    private fun mockClient(handler: (String) -> HttpStatusCode): HttpClient {
        return HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            engine {
                addHandler { request ->
                    respond(
                        content = """{"status":"ok"}""",
                        status = handler(request.url.toString()),
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }
    }

    private class MutableBackendConfigProvider(
        private var baseUrl: String
    ) : BackendConfigProvider {
        override suspend fun refresh(force: Boolean): BackendConfig = currentConfig()

        override fun currentConfig(): BackendConfig = BackendConfig(baseUrl = baseUrl)

        override fun candidateBaseUrls(): List<String> = listOf(baseUrl)

        override fun activateBaseUrl(baseUrl: String) {
            this.baseUrl = baseUrl
        }
    }
}
