package com.raylabs.laundryhub.core.data.config

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorBackendHealthCheckerTest {

    private fun mockClient(status: HttpStatusCode, exception: Throwable? = null): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler {
                    if (exception != null) {
                        throw exception
                    }
                    respond(
                        content = "OK",
                        status = status,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            }
        }
    }

    @Test
    fun `isHealthy returns true for 200 OK`() = runBlocking {
        val client = mockClient(HttpStatusCode.OK)
        val healthChecker = KtorBackendHealthChecker(client)
        val isHealthyResult = healthChecker.isHealthy("https://backend.example.com")
        assertTrue(isHealthyResult)
    }

    @Test
    fun `isHealthy returns false for 500 Internal Server Error`() = runBlocking {
        val client = mockClient(HttpStatusCode.InternalServerError)
        val healthChecker = KtorBackendHealthChecker(client)
        val isHealthyResult = healthChecker.isHealthy("https://backend.example.com")
        assertFalse(isHealthyResult)
    }

    @Test
    fun `isHealthy returns false for network exception`() = runBlocking {
        val client = mockClient(HttpStatusCode.OK, exception = RuntimeException("Network error"))
        val healthChecker = KtorBackendHealthChecker(client)
        val isHealthyResult = healthChecker.isHealthy("https://backend.example.com")
        assertFalse(isHealthyResult)
    }

    @Test
    fun `isHealthy returns false for invalid or unreachable baseUrl`() = runBlocking {
        val healthChecker = KtorBackendHealthChecker()
        
        // This will fail instantly due to invalid URL or host lookup failure
        val isHealthyResult = healthChecker.isHealthy("https://invalid-unreachable-host.xyz")
        
        assertFalse(isHealthyResult)
    }

    @Test
    fun `isHealthy returns false for blank baseUrl`() = runBlocking {
        val healthChecker = KtorBackendHealthChecker()
        val isHealthyResult = healthChecker.isHealthy("   ")
        
        assertFalse(isHealthyResult)
    }
}
