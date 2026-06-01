package com.raylabs.laundryhub.core.domain.usecase.sheets.income

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
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

class GetOrderUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var getOrderUseCase: GetOrderUseCase

    @Before
    fun setUp() {
        repository = mock()
        getOrderUseCase = GetOrderUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository fetch succeeds`() = runTest {
        val mockData: TransactionData = mock()
        whenever(repository.getOrderById("ORD123")).thenReturn(Resource.Success(mockData))

        val result = getOrderUseCase(orderID = "ORD123")

        assertEquals(Resource.Success(mockData), result)
        verify(repository).getOrderById("ORD123")
    }

    @Test
    fun `invoke returns error when repository fetch returns error`() = runTest {
        whenever(repository.getOrderById("ORD123")).thenReturn(Resource.Error("Failed to fetch"))

        val result = getOrderUseCase(orderID = "ORD123")

        assertEquals(Resource.Error("Failed to fetch"), result)
        verify(repository).getOrderById("ORD123")
    }

    @Test
    fun `invoke retries and succeeds after exceptions are thrown`() = runTest {
        val mockData: TransactionData = mock()
        var callCount = 0
        whenever(repository.getOrderById("ORD123")).thenAnswer {
            callCount++
            if (callCount < 2) {
                throw RuntimeException("Temporary network issue")
            }
            Resource.Success(mockData)
        }

        val retriesInvoked = mutableListOf<Int>()
        val result = getOrderUseCase(
            onRetry = { retriesInvoked.add(it) },
            orderID = "ORD123"
        )

        assertEquals(Resource.Success(mockData), result)
        assertEquals(2, callCount)
        assertEquals(listOf(1), retriesInvoked)
    }

    @Test
    fun `invoke returns handleFailRetry after exhausting all retries on exception`() = runTest {
        whenever(repository.getOrderById("ORD123")).thenThrow(RuntimeException("Persistent error"))

        val retriesInvoked = mutableListOf<Int>()
        val result = getOrderUseCase(
            onRetry = { retriesInvoked.add(it) },
            orderID = "ORD123"
        )

        assertEquals(UseCaseErrorHandling.handleFailRetry, result)
        assertEquals(listOf(1, 2), retriesInvoked)
        verify(repository, times(3)).getOrderById("ORD123")
    }
}
