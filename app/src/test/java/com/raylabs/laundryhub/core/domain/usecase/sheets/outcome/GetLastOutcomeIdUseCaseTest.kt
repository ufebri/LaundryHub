package com.raylabs.laundryhub.core.domain.usecase.sheets.outcome

import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GetLastOutcomeIdUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var getLastOutcomeIdUseCase: GetLastOutcomeIdUseCase

    @Before
    fun setUp() {
        repository = mock()
        getLastOutcomeIdUseCase = GetLastOutcomeIdUseCase(repository)
    }

    @Test
    fun `invoke returns success and outcome id when repository succeeds`() = runTest {
        whenever(repository.getLastOutcomeId()).thenReturn(Resource.Success("OUT100"))

        val result = getLastOutcomeIdUseCase()

        assertEquals(Resource.Success("OUT100"), result)
        verify(repository).getLastOutcomeId()
    }

    @Test
    fun `invoke returns error when repository returns error`() = runTest {
        whenever(repository.getLastOutcomeId()).thenReturn(Resource.Error("Not Found"))

        val result = getLastOutcomeIdUseCase()

        assertEquals(Resource.Error("Not Found"), result)
        verify(repository).getLastOutcomeId()
    }

    @Test
    fun `invoke retries and succeeds after exceptions are thrown`() = runTest {
        var callCount = 0
        whenever(repository.getLastOutcomeId()).thenAnswer {
            callCount++
            if (callCount < 2) {
                throw RuntimeException("Temporary network issue")
            }
            Resource.Success("OUT100")
        }

        val retriesInvoked = mutableListOf<Int>()
        val result = getLastOutcomeIdUseCase(
            onRetry = { retriesInvoked.add(it) }
        )

        assertEquals(Resource.Success("OUT100"), result)
        assertEquals(2, callCount)
        assertEquals(listOf(1), retriesInvoked)
    }

    @Test
    fun `invoke returns handleNotFoundID after exhausting all retries on exception`() = runTest {
        whenever(repository.getLastOutcomeId()).thenThrow(RuntimeException("Persistent error"))

        val retriesInvoked = mutableListOf<Int>()
        val result = getLastOutcomeIdUseCase(
            onRetry = { retriesInvoked.add(it) }
        )

        assertEquals(UseCaseErrorHandling.handleNotFoundID, result)
        assertEquals(listOf(1, 2), retriesInvoked)
        verify(repository, times(3)).getLastOutcomeId()
    }
}
