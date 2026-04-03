package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.repository.CacheRepository
import javax.inject.Inject

class ClearCacheUseCase @Inject constructor(
    private val cacheRepository: CacheRepository
) {
    suspend operator fun invoke(): Boolean = cacheRepository.clearCache()
}
