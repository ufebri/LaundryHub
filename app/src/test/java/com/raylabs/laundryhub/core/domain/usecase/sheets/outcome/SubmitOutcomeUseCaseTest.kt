package com.raylabs.laundryhub.core.domain.usecase.sheets.outcome

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SubmitOutcomeUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var submitOutcomeUseCase: SubmitOutcomeUseCase
    private val mockOutcome: OutcomeData = mock()

    @Before
    fun setUp() {
        repository = mock()
        submitOutcomeUseCase = SubmitOutcomeUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository add succeeds`() = runTest {
        whenever(repository.addOutcome(mockOutcome)).thenReturn(Resource.Success("Success"))

        val result = submitOutcomeUseCase(order = mockOutcome)

        assertEquals(Resource.Success("Success"), result)
        verify(repository).addOutcome(mockOutcome)
    }

    @Test
    fun `invoke returns error when repository add returns error`() = runTest {
        whenever(repository.addOutcome(mockOutcome)).thenReturn(Resource.Error("Failed to add"))

        val result = submitOutcomeUseCase(order = mockOutcome)

        assertEquals(Resource.Error("Failed to add"), result)
        verify(repository).addOutcome(mockOutcome)
    }

    @Test
    fun `invoke retries and succeeds after exceptions are thrown`() = runTest {
        var callCount = 0
        whenever(repository.addOutcome(mockOutcome)).thenAnswer {
            callCount++
            if (callCount < 2) {
                throw RuntimeException("Temporary network issue")
            }
            Resource.Success("Success")
        }

        val retriesInvoked = mutableListOf<Int>()
        val result = submitOutcomeUseCase(
            onRetry = { retriesInvoked.add(it) },
            order = mockOutcome
        )

        assertEquals(Resource.Success("Success"), result)
        assertEquals(2, callCount)
        assertEquals(listOf(1), retriesInvoked)
    }

    @Test
    fun `invoke returns handleFailedSubmit after exhausting all retries on exception`() = runTest {
        whenever(repository.addOutcome(mockOutcome)).thenThrow(RuntimeException("Persistent error"))

        val retriesInvoked = mutableListOf<Int>()
        val result = submitOutcomeUseCase(
            onRetry = { retriesInvoked.add(it) },
            order = mockOutcome
        )

        assertEquals(UseCaseErrorHandling.handleFailedSubmit, result)
        assertEquals(listOf(1, 2), retriesInvoked)
        verify(repository, times(3)).addOutcome(mockOutcome)
    }
}
