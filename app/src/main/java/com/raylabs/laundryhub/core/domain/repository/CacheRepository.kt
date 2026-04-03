package com.raylabs.laundryhub.core.domain.repository

interface CacheRepository {
    suspend fun getCacheSizeBytes(): Long
    suspend fun clearCache(): Boolean
}
