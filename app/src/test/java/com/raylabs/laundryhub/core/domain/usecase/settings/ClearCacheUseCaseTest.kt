package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.repository.CacheRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ClearCacheUseCaseTest {

    private lateinit var cacheRepository: CacheRepository
    private lateinit var clearCacheUseCase: ClearCacheUseCase

    @Before
    fun setUp() {
        cacheRepository = mock()
        clearCacheUseCase = ClearCacheUseCase(cacheRepository)
    }

    @Test
    fun `invoke delegates cache clearing to repository`() = runTest {
        whenever(cacheRepository.clearCache()).thenReturn(true)

        val result = clearCacheUseCase()

        assertTrue(result)
        verify(cacheRepository).clearCache()
    }
}
