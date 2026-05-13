package com.raylabs.laundryhub.core.data.config

import com.raylabs.laundryhub.core.domain.config.BackendConfig
import com.raylabs.laundryhub.core.domain.config.BackendConfigProvider

class RemoteBackendConfigProvider(
    private val remoteConfigSource: RemoteConfigSource,
    private val endpointValidator: BackendEndpointValidator,
    fallbackBaseUrl: String
) : BackendConfigProvider {

    private val fallbackBaseUrl = endpointValidator.normalizeFallback(fallbackBaseUrl)

    @Volatile
    private var activeConfig = BackendConfig(baseUrl = this.fallbackBaseUrl)

    override suspend fun refresh(force: Boolean): BackendConfig {
        runCatching { remoteConfigSource.refresh(force) }

        val remoteBaseUrl = endpointValidator.normalizeRemote(
            remoteConfigSource.getString(KEY_API_BASE_URL)
        )
        val maintenanceMessage = remoteConfigSource
            .getString(KEY_API_MAINTENANCE_MESSAGE)
            .trim()
            .takeIf { it.isNotBlank() }
        val configVersion = remoteConfigSource
            .getLong(KEY_API_CONFIG_VERSION)
            .takeIf { it > 0L }

        activeConfig = BackendConfig(
            baseUrl = remoteBaseUrl ?: fallbackBaseUrl,
            maintenanceEnabled = remoteConfigSource.getBoolean(KEY_API_MAINTENANCE_ENABLED),
            maintenanceMessage = maintenanceMessage,
            configVersion = configVersion
        )

        return activeConfig
    }

    override fun currentConfig(): BackendConfig = activeConfig

    override fun candidateBaseUrls(): List<String> {
        return listOf(activeConfig.baseUrl, fallbackBaseUrl).distinct()
    }

    override fun activateBaseUrl(baseUrl: String) {
        val normalized = if (baseUrl == fallbackBaseUrl) {
            fallbackBaseUrl
        } else {
            endpointValidator.normalizeRemote(baseUrl) ?: return
        }

        activeConfig = activeConfig.copy(baseUrl = normalized)
    }

    companion object {
        const val KEY_API_BASE_URL = "api_base_url"
        const val KEY_API_MAINTENANCE_ENABLED = "api_maintenance_enabled"
        const val KEY_API_MAINTENANCE_MESSAGE = "api_maintenance_message"
        const val KEY_API_CONFIG_VERSION = "api_config_version"
    }
}
