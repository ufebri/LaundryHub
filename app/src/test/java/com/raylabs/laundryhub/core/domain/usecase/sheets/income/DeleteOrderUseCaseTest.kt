package com.raylabs.laundryhub.core.domain.usecase.sheets.income

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

class DeleteOrderUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var deleteOrderUseCase: DeleteOrderUseCase

    @Before
    fun setUp() {
        repository = mock()
        deleteOrderUseCase = DeleteOrderUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository delete succeeds`() = runTest {
        whenever(repository.deleteOrder("ORD123")).thenReturn(Resource.Success(true))

        val result = deleteOrderUseCase(orderId = "ORD123")

        assertEquals(Resource.Success(true), result)
        verify(repository).deleteOrder("ORD123")
    }

    @Test
    fun `invoke returns error when repository delete returns error`() = runTest {
        whenever(repository.deleteOrder("ORD123")).thenReturn(Resource.Error("Failed to delete"))

        val result = deleteOrderUseCase(orderId = "ORD123")

        assertEquals(Resource.Error("Failed to delete"), result)
        verify(repository).deleteOrder("ORD123")
    }

    @Test
    fun `invoke retries and succeeds after exceptions are thrown`() = runTest {
        var callCount = 0
        whenever(repository.deleteOrder("ORD123")).thenAnswer {
            callCount++
            if (callCount < 2) {
                throw RuntimeException("Temporary network issue")
            }
            Resource.Success(true)
        }

        val retriesInvoked = mutableListOf<Int>()
        val result = deleteOrderUseCase(
            onRetry = { retriesInvoked.add(it) },
            orderId = "ORD123"
        )

        assertEquals(Resource.Success(true), result)
        assertEquals(2, callCount)
        assertEquals(listOf(1), retriesInvoked)
    }

    @Test
    fun `invoke returns handleFailedSubmit after exhausting all retries on exception`() = runTest {
        whenever(repository.deleteOrder("ORD123")).thenThrow(RuntimeException("Persistent error"))

        val retriesInvoked = mutableListOf<Int>()
        val result = deleteOrderUseCase(
            onRetry = { retriesInvoked.add(it) },
            orderId = "ORD123"
        )

        assertEquals(UseCaseErrorHandling.handleFailedSubmit, result)
        assertEquals(listOf(1, 2), retriesInvoked)
        verify(repository, times(3)).deleteOrder("ORD123")
    }
}
