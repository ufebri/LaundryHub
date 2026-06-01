package com.raylabs.laundryhub.core.domain.usecase.sheets.income

import com.raylabs.laundryhub.core.domain.model.sheets.FILTER
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ReadIncomeTransactionUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var useCase: ReadIncomeTransactionUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = ReadIncomeTransactionUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository read succeeds`() = runTest {
        val transactions = listOf(mock<TransactionData>())
        whenever(repository.readIncomeTransaction(FILTER.SHOW_ALL_DATA, null)).thenReturn(Resource.Success(transactions))

        val result = useCase()

        assertEquals(Resource.Success(transactions), result)
        verify(repository).readIncomeTransaction(FILTER.SHOW_ALL_DATA, null)
    }

    @Test
    fun `invoke returns error when repository read returns error`() = runTest {
        whenever(repository.readIncomeTransaction(FILTER.SHOW_ALL_DATA, null)).thenReturn(Resource.Error("Error"))

        val result = useCase()

        assertEquals(Resource.Error("Error"), result)
        verify(repository).readIncomeTransaction(FILTER.SHOW_ALL_DATA, null)
    }

    @Test
    fun `invoke retries and succeeds after exceptions are thrown`() = runTest {
        val transactions = listOf(mock<TransactionData>())
        var callCount = 0
        whenever(repository.readIncomeTransaction(FILTER.SHOW_ALL_DATA, null)).thenAnswer {
            callCount++
            if (callCount < 2) {
                throw RuntimeException("Temporary network issue")
            }
            Resource.Success(transactions)
        }

        val retriesInvoked = mutableListOf<Int>()
        val result = useCase(onRetry = { retriesInvoked.add(it) })

        assertEquals(Resource.Success(transactions), result)
        assertEquals(2, callCount)
        assertEquals(listOf(1), retriesInvoked)
    }

    @Test
    fun `invoke returns handleFailRetry after exhausting all retries on exception`() = runTest {
        whenever(repository.readIncomeTransaction(FILTER.SHOW_ALL_DATA, null)).thenThrow(RuntimeException("Persistent error"))

        val retriesInvoked = mutableListOf<Int>()
        val result = useCase(onRetry = { retriesInvoked.add(it) })

        assertEquals(UseCaseErrorHandling.handleFailRetry, result)
        assertEquals(listOf(1, 2), retriesInvoked)
        verify(repository, times(3)).readIncomeTransaction(FILTER.SHOW_ALL_DATA, null)
    }

    @Test
    fun `getPagingData returns paging data flow`() {
        val flow = useCase.getPagingData()
        assertNotNull(flow)
    }
}
