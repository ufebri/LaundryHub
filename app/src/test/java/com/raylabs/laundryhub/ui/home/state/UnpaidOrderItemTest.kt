package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnpaidOrderItemTest {

    @Test
    fun `toUi maps TransactionData to UnpaidOrderItem correctly`() {
        val trx = TransactionData(
            orderID = "ORD001",
            name = "Asep",
            date = "2025-08-04",
            totalPrice = "5000",
            packageType = "Reguler",
            paymentStatus = "Unpaid",
            paymentMethod = "Cash",
            phoneNumber = "081234",
            remark = "",
            weight = "2",
            pricePerKg = "2500",
            dueDate = "2025-08-06"
        )

        val result = listOf(trx).toUi()
        assertEquals(1, result.size)
        val item = result[0]
        assertEquals("ORD001", item.orderID)
        assertEquals("Asep", item.customerName)
        assertEquals("Reguler", item.packageType)
        assertEquals(trx.paidDescription(), item.nowStatus)
        assertEquals("2025-08-06", item.dueDate)
    }

    @Test
    fun `toUi returns empty list if input empty`() {
        val result = emptyList<TransactionData>().toUi()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toUi maps multiple TransactionData items`() {
        val trx1 = TransactionData(
            orderID = "A1",
            name = "Nia",
            date = "2025-08-04",
            totalPrice = "8000",
            packageType = "Express",
            paymentStatus = "Paid",
            paymentMethod = "QRIS",
            phoneNumber = "081235",
            remark = "",
            weight = "1",
            pricePerKg = "8000",
            dueDate = "2025-08-05"
        )
        val trx2 = TransactionData(
            orderID = "A2",
            name = "Budi",
            date = "2025-08-04",
            totalPrice = "10000",
            packageType = "Reguler",
            paymentStatus = "Unpaid",
            paymentMethod = "Cash",
            phoneNumber = "081236",
            remark = "",
            weight = "2",
            pricePerKg = "5000",
            dueDate = "2025-08-06"
        )

        val result = listOf(trx1, trx2).toUi()
        assertEquals(2, result.size)
        assertEquals("Nia", result[0].customerName)
        assertEquals("Budi", result[1].customerName)
    }
}