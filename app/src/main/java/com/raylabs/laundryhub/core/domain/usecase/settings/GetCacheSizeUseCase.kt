package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.repository.CacheRepository
import javax.inject.Inject

class GetCacheSizeUseCase @Inject constructor(
    private val cacheRepository: CacheRepository
) {
    suspend operator fun invoke(): Long = cacheRepository.getCacheSizeBytes()
}
