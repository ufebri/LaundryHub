package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetLastOrderIdUseCaseTest {
    private lateinit var repository: GoogleSheetRepository
    private lateinit var useCase: GetLastOrderIdUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = GetLastOrderIdUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository returns success`() = runTest {
        whenever(repository.getLastOrderId()).thenReturn(Resource.Success("123"))
        val result = useCase.invoke()
        assertTrue(result is Resource.Success)
        assertEquals("123", (result as Resource.Success).data)
    }

    @Test
    fun `invoke returns error when repository returns null`() = runTest {
        // Simulate retry returns null
        val useCase = object : GetLastOrderIdUseCase(repository) {
            override suspend fun invoke(onRetry: ((Int) -> Unit)?): Resource<String> {
                return super.invoke(onRetry = onRetry)
            }
        }
        val result = useCase.invoke { }
        // Since retry returns null, should return Resource.Error
        assertTrue(result is Resource.Error)
    }
}

