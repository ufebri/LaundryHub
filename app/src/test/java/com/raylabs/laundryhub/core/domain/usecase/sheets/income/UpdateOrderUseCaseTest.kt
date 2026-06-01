package com.raylabs.laundryhub.core.domain.usecase.sheets.income

import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
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

class UpdateOrderUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var updateOrderUseCase: UpdateOrderUseCase
    private val mockOrder: OrderData = mock()

    @Before
    fun setUp() {
        repository = mock()
        updateOrderUseCase = UpdateOrderUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository update succeeds`() = runTest {
        whenever(repository.updateOrder(mockOrder)).thenReturn(Resource.Success(true))

        val result = updateOrderUseCase(order = mockOrder)

        assertEquals(Resource.Success(true), result)
        verify(repository).updateOrder(mockOrder)
    }

    @Test
    fun `invoke returns error when repository update returns error`() = runTest {
        whenever(repository.updateOrder(mockOrder)).thenReturn(Resource.Error("Failed to update"))

        val result = updateOrderUseCase(order = mockOrder)

        assertEquals(Resource.Error("Failed to update"), result)
        verify(repository).updateOrder(mockOrder)
    }

    @Test
    fun `invoke retries and succeeds after exceptions are thrown`() = runTest {
        var callCount = 0
        whenever(repository.updateOrder(mockOrder)).thenAnswer {
            callCount++
            if (callCount < 2) {
                throw RuntimeException("Temporary network issue")
            }
            Resource.Success(true)
        }

        val retriesInvoked = mutableListOf<Int>()
        val result = updateOrderUseCase(
            onRetry = { retriesInvoked.add(it) },
            order = mockOrder
        )

        assertEquals(Resource.Success(true), result)
        assertEquals(2, callCount)
        assertEquals(listOf(1), retriesInvoked)
    }

    @Test
    fun `invoke returns handleFailedSubmit after exhausting all retries on exception`() = runTest {
        whenever(repository.updateOrder(mockOrder)).thenThrow(RuntimeException("Persistent error"))

        val retriesInvoked = mutableListOf<Int>()
        val result = updateOrderUseCase(
            onRetry = { retriesInvoked.add(it) },
            order = mockOrder
        )

        assertEquals(UseCaseErrorHandling.handleFailedSubmit, result)
        assertEquals(listOf(1, 2), retriesInvoked)
        verify(repository, times(3)).updateOrder(mockOrder)
    }
}
