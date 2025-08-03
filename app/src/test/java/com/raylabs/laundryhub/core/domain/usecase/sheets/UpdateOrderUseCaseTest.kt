package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateOrderUseCaseTest {
    private lateinit var repository: GoogleSheetRepository
    private lateinit var useCase: UpdateOrderUseCase

    @Before
    fun setup() {
        repository = mock()
        useCase = UpdateOrderUseCase(repository)
    }

    @Test
    fun `invoke returns success when repository updateOrder is successful`() = runTest {
        val order = OrderData(
            orderId = "ORD1",
            name = "Alicia",
            phoneNumber = "0813",
            packageName = "Reguler",
            priceKg = "5500",
            totalPrice = "11000",
            paidStatus = "Paid",
            paymentMethod = "Cash",
            remark = "Edited",
            weight = "3",
            dueDate = "25/07/2025"
        )
        whenever(repository.updateOrder(order)).thenReturn(Resource.Success(true))

        val result = useCase(order = order)
        assertTrue(result is Resource.Success)
        assertEquals(true, (result as Resource.Success).data)
    }

    @Test
    fun `invoke returns error when repository updateOrder fails`() = runTest {
        val order = OrderData(
            orderId = "ORD1",
            name = "Alicia",
            phoneNumber = "0813",
            packageName = "Reguler",
            priceKg = "5500",
            totalPrice = "11000",
            paidStatus = "Paid",
            paymentMethod = "Cash",
            remark = "Edited",
            weight = "3",
            dueDate = "25/07/2025"
        )
        whenever(repository.updateOrder(order)).thenReturn(Resource.Error("Failed"))

        val result = useCase(order = order)
        assertTrue(result is Resource.Error)
        assertEquals("Failed", (result as Resource.Error).message)
    }

    @Test
    fun `invoke calls onRetry callback for each retry`() = runTest {
        val order = OrderData(
            orderId = "ORD1",
            name = "Alicia",
            phoneNumber = "0813",
            packageName = "Reguler",
            priceKg = "5500",
            totalPrice = "11000",
            paidStatus = "Paid",
            paymentMethod = "Cash",
            remark = "Edited",
            weight = "3",
            dueDate = "25/07/2025"
        )
        var retryCount = 0

        // THIS IS THE KEY FIX:
        whenever(repository.updateOrder(order)).thenThrow(RuntimeException("Fail"))

        val result = useCase(
            onRetry = { retryCount++ },
            order = order
        )
        // Karena retry default 3x, retryCount minimal 2
        assertTrue(result is Resource.Error)
        assertTrue(retryCount > 0)
    }
}