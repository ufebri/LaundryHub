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

class SubmitOrderUseCaseTest {

    private lateinit var repository: LaundryRepository
    private lateinit var submitOrderUseCase: SubmitOrderUseCase
    private val mockOrder: OrderData = mock()

    @Before
    fun setUp() {
        repository = mock()
        submitOrderUseCase = SubmitOrderUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository add succeeds`() = runTest {
        whenever(repository.addOrder(mockOrder)).thenReturn(Resource.Success("Success"))

        val result = submitOrderUseCase(order = mockOrder)

        assertEquals(Resource.Success("Success"), result)
        verify(repository).addOrder(mockOrder)
    }

    @Test
    fun `invoke returns error when repository add returns error`() = runTest {
        whenever(repository.addOrder(mockOrder)).thenReturn(Resource.Error("Failed to add"))

        val result = submitOrderUseCase(order = mockOrder)

        assertEquals(Resource.Error("Failed to add"), result)
        verify(repository).addOrder(mockOrder)
    }

    @Test
    fun `invoke retries and succeeds after exceptions are thrown`() = runTest {
        var callCount = 0
        whenever(repository.addOrder(mockOrder)).thenAnswer {
            callCount++
            if (callCount < 2) {
                throw RuntimeException("Temporary network issue")
            }
            Resource.Success("Success")
        }

        val retriesInvoked = mutableListOf<Int>()
        val result = submitOrderUseCase(
            onRetry = { retriesInvoked.add(it) },
            order = mockOrder
        )

        assertEquals(Resource.Success("Success"), result)
        assertEquals(2, callCount)
        assertEquals(listOf(1), retriesInvoked)
    }

    @Test
    fun `invoke returns handleFailedSubmit after exhausting all retries on exception`() = runTest {
        whenever(repository.addOrder(mockOrder)).thenThrow(RuntimeException("Persistent error"))

        val retriesInvoked = mutableListOf<Int>()
        val result = submitOrderUseCase(
            onRetry = { retriesInvoked.add(it) },
            order = mockOrder
        )

        assertEquals(UseCaseErrorHandling.handleFailedSubmit, result)
        assertEquals(listOf(1, 2), retriesInvoked)
        verify(repository, times(3)).addOrder(mockOrder)
    }
}
