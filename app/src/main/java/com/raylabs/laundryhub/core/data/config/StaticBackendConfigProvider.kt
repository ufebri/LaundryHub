package com.raylabs.laundryhub.core.data.config

import com.raylabs.laundryhub.core.domain.config.BackendConfig
import com.raylabs.laundryhub.core.domain.config.BackendConfigProvider

class StaticBackendConfigProvider(
    baseUrl: String,
    private val endpointValidator: BackendEndpointValidator = BackendEndpointValidator()
) : BackendConfigProvider {

    private val fallbackBaseUrl = endpointValidator.normalizeFallback(baseUrl)
    private var activeConfig = BackendConfig(baseUrl = fallbackBaseUrl)

    override suspend fun refresh(force: Boolean): BackendConfig = activeConfig

    override fun currentConfig(): BackendConfig = activeConfig

    override fun candidateBaseUrls(): List<String> = listOf(activeConfig.baseUrl)

    override fun activateBaseUrl(baseUrl: String) {
        val normalized = if (baseUrl == fallbackBaseUrl) {
            fallbackBaseUrl
        } else {
            endpointValidator.normalizeRemote(baseUrl) ?: return
        }
        activeConfig = activeConfig.copy(baseUrl = normalized)
    }
}
