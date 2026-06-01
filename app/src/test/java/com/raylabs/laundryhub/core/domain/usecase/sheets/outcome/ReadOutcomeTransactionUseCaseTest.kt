package com.raylabs.laundryhub.core.domain.usecase.sheets.outcome

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReadOutcomeTransactionUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var readOutcomeTransactionUseCase: ReadOutcomeTransactionUseCase

    @Before
    fun setUp() {
        repository = mock()
        readOutcomeTransactionUseCase = ReadOutcomeTransactionUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository returns data`() = runTest {
        val data = listOf(OutcomeData("out-1", "2026-06-01", "Soap", "15000", "", "qris"))
        whenever(repository.readOutcomeTransaction()).thenReturn(Resource.Success(data))

        val result = readOutcomeTransactionUseCase()

        assertEquals(Resource.Success(data), result)
    }

    @Test
    fun `invoke returns error when repository fails after all attempts`() = runTest {
        whenever(repository.readOutcomeTransaction()).thenThrow(RuntimeException("Persistent error"))

        val result = readOutcomeTransactionUseCase()

        assertEquals(UseCaseErrorHandling.handleFailRetry, result)
    }

    @Test
    fun `getPagingData returns paging flow successfully`() = runTest {
        val flow = readOutcomeTransactionUseCase.getPagingData()
        assertNotNull(flow)
    }
}
