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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SubmitOrderUseCaseTest {
    private lateinit var repository: GoogleSheetRepository
    private lateinit var useCase: SubmitOrderUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = SubmitOrderUseCase(repository)
    }

    @Test
    fun `returns success when repository returns success`() = runTest {
        val order = OrderData(
            orderId = "1",
            name = "A",
            phoneNumber = "0812",
            packageName = "Reguler",
            priceKg = "5000",
            totalPrice = "5000",
            paidStatus = "Paid",
            paymentMethod = "Cash",
            remark = "-",
            weight = "1",
            orderDate = "21/06/2025",
            dueDate = "24/06/2025"
        )
        whenever(repository.addOrder(order)).thenReturn(Resource.Success(true))
        val result = useCase.invoke(order = order)
        assertTrue(result is Resource.Success)
        assertEquals(true, (result as Resource.Success).data)
    }

    @Test
    fun `returns error when repository returns null`() = runTest {
        val order = OrderData(
            orderId = "1",
            name = "A",
            phoneNumber = "0812",
            packageName = "Reguler",
            priceKg = "5000",
            totalPrice = "5000",
            paidStatus = "Paid",
            paymentMethod = "Cash",
            remark = "-",
            weight = "1",
            orderDate = "21/06/2025",
            dueDate = "24/06/2025"
        )
        whenever(repository.addOrder(order)).thenReturn(null)
        val result = useCase.invoke(order = order)
        assertTrue(result is Resource.Error)
        assertEquals("Failed to submit data", (result as Resource.Error).message)
    }

    @Test
    fun `returns error when repository returns error`() = runTest {
        val order = OrderData(
            orderId = "1",
            name = "A",
            phoneNumber = "0812",
            packageName = "Reguler",
            priceKg = "5000",
            totalPrice = "5000",
            paidStatus = "Paid",
            paymentMethod = "Cash",
            remark = "-",
            weight = "1",
            orderDate = "21/06/2025",
            dueDate = "24/06/2025"
        )
        whenever(repository.addOrder(order)).thenReturn(Resource.Error("fail"))
        val result = useCase.invoke(order = order)
        assertTrue(result is Resource.Error)
        assertEquals("fail", (result as Resource.Error).message)
    }
}
