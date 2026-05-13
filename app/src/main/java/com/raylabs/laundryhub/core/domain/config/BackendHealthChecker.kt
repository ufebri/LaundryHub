package com.raylabs.laundryhub.core.domain.config

interface BackendHealthChecker {
    suspend fun isHealthy(baseUrl: String): Boolean
}
