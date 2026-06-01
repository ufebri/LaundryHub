package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ReadSpreadsheetDataUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var readSpreadsheetDataUseCase: ReadSpreadsheetDataUseCase

    @Before
    fun setUp() {
        repository = mock()
        readSpreadsheetDataUseCase = ReadSpreadsheetDataUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository read succeeds`() = runTest {
        val summary = listOf(SpreadsheetData(key = "Total Orders", value = "50"))
        whenever(repository.readSummaryTransaction()).thenReturn(Resource.Success(summary))

        val result = readSpreadsheetDataUseCase()

        assertEquals(Resource.Success(summary), result)
        verify(repository).readSummaryTransaction()
    }

    @Test
    fun `invoke returns error when repository read returns error`() = runTest {
        whenever(repository.readSummaryTransaction()).thenReturn(Resource.Error("Failed to read"))

        val result = readSpreadsheetDataUseCase()

        assertEquals(Resource.Error("Failed to read"), result)
        verify(repository).readSummaryTransaction()
    }

    @Test
    fun `invoke retries and succeeds after exceptions are thrown`() = runTest {
        val summary = listOf(SpreadsheetData(key = "Total Orders", value = "50"))
        var callCount = 0
        whenever(repository.readSummaryTransaction()).thenAnswer {
            callCount++
            if (callCount < 2) {
                throw RuntimeException("Network error")
            }
            Resource.Success(summary)
        }

        val retriesInvoked = mutableListOf<Int>()
        val result = readSpreadsheetDataUseCase(onRetry = { retriesInvoked.add(it) })

        assertEquals(Resource.Success(summary), result)
        assertEquals(2, callCount)
        assertEquals(listOf(1), retriesInvoked)
    }

    @Test
    fun `invoke returns error after exhausting all retries on exception`() = runTest {
        whenever(repository.readSummaryTransaction()).thenThrow(RuntimeException("Fatal error"))

        val retriesInvoked = mutableListOf<Int>()
        val result = readSpreadsheetDataUseCase(onRetry = { retriesInvoked.add(it) })

        assertEquals(Resource.Error("Failed after 3 attempts."), result)
        assertEquals(listOf(1, 2), retriesInvoked)
        verify(repository, times(3)).readSummaryTransaction()
    }
}
