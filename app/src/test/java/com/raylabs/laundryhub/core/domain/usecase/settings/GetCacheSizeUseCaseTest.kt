package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.repository.CacheRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetCacheSizeUseCaseTest {

    private lateinit var cacheRepository: CacheRepository
    private lateinit var getCacheSizeUseCase: GetCacheSizeUseCase

    @Before
    fun setUp() {
        cacheRepository = mock()
        getCacheSizeUseCase = GetCacheSizeUseCase(cacheRepository)
    }

    @Test
    fun `invoke delegates cache size lookup to repository`() = runTest {
        whenever(cacheRepository.getCacheSizeBytes()).thenReturn(2048L)

        val result = getCacheSizeUseCase()

        assertEquals(2048L, result)
        verify(cacheRepository).getCacheSizeBytes()
    }
}
