package com.raylabs.laundryhub.core.data.config

import com.raylabs.laundryhub.core.domain.config.BackendHealthChecker
import com.raylabs.laundryhub.shared.network.HttpClientProvider
import io.ktor.client.request.get
import kotlinx.coroutines.withTimeout

class KtorBackendHealthChecker(
    private val client: io.ktor.client.HttpClient = HttpClientProvider.createClient(enableLogging = false)
) : BackendHealthChecker {

    override suspend fun isHealthy(baseUrl: String): Boolean {
        return runCatching {
            withTimeout(HEALTH_TIMEOUT_MILLIS) {
                val healthUrl = "${baseUrl.trimEnd('/')}/health"
                client.get(healthUrl).status.value in HTTP_SUCCESS_RANGE
            }
        }.getOrDefault(false)
    }

    private companion object {
        const val HEALTH_TIMEOUT_MILLIS = 5_000L
        val HTTP_SUCCESS_RANGE = 200..299
    }
}
