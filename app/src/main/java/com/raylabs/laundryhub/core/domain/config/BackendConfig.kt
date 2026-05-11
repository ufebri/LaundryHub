package com.raylabs.laundryhub.core.domain.config

data class BackendConfig(
    val baseUrl: String,
    val maintenanceEnabled: Boolean = false,
    val maintenanceMessage: String? = null,
    val configVersion: Long? = null
)

interface BackendConfigProvider {
    suspend fun refresh(force: Boolean = false): BackendConfig

    fun currentConfig(): BackendConfig

    fun candidateBaseUrls(): List<String>

    fun activateBaseUrl(baseUrl: String)

    suspend fun currentBaseUrl(): String = currentConfig().baseUrl
}
