package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetHistoryUseCaseTest {
    private lateinit var repository: GoogleSheetRepository
    private lateinit var getOrderUseCase: GetOrderUseCase

    @Before
    fun setUp() {
        repository = Mockito.mock(GoogleSheetRepository::class.java)
        getOrderUseCase = GetOrderUseCase(repository)
    }

    @Test
    fun `invoke returns Resource_Error when repository returns null`() = runTest {
        val orderId = "123"
        whenever(repository.getOrderById(orderId)).thenReturn(null)

        val result = getOrderUseCase(orderID = orderId)
        assertTrue(result is Resource.Error)
        assertEquals("Failed after 3 attempts.", (result as Resource.Error).message)
    }

    @Test
    fun `invoke calls onRetry callback`() = runTest {
        val orderId = "123"
        var retryCount = 0
        whenever(repository.getOrderById(orderId)).thenThrow(RuntimeException())
        val onRetry: (Int) -> Unit = { retryCount++ }

        getOrderUseCase(onRetry = onRetry, orderID = orderId)
        assertTrue(retryCount > 0)
    }
}
