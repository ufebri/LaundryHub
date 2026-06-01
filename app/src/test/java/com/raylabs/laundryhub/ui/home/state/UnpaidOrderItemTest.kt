package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import org.junit.Assert.assertEquals
import org.junit.Test

class UnpaidOrderItemTest {

    @Test
    fun `UnpaidOrderItem properties initialize correctly`() {
        val item = UnpaidOrderItem(
            orderID = "ORD-1",
            customerName = "Emy",
            packageType = "Regular",
            nowStatus = "Unpaid",
            dueDate = "2026-06-05",
            orderDate = "2026-06-01",
            syncStatus = SyncStatus.PENDING
        )

        assertEquals("ORD-1", item.orderID)
        assertEquals("Emy", item.customerName)
        assertEquals("Regular", item.packageType)
        assertEquals("Unpaid", item.nowStatus)
        assertEquals("2026-06-05", item.dueDate)
        assertEquals("2026-06-01", item.orderDate)
        assertEquals(SyncStatus.PENDING, item.syncStatus)
    }

    @Test
    fun `List toUi maps TransactionData to UnpaidOrderItem`() {
        val transaction = TransactionData(
            orderID = "123",
            name = "John",
            packageType = "Express",
            dueDate = "2026-06-10",
            date = "2026-06-01",
            pricePerKg = "25000",
            totalPrice = "50000",
            paymentStatus = "belum",
            weight = "2",
            remark = "",
            paymentMethod = "Cash",
            phoneNumber = "0812345678"
        )

        val uiList = listOf(transaction).toUi()
        assertEquals(1, uiList.size)
        val uiItem = uiList[0]

        assertEquals("123", uiItem.orderID)
        assertEquals("John", uiItem.customerName)
        assertEquals("Express", uiItem.packageType)
        assertEquals("Unpaid", uiItem.nowStatus) // Maps "belum" to "Unpaid" via paidDescription()
        assertEquals("2026-06-10", uiItem.dueDate)
        assertEquals("2026-06-01", uiItem.orderDate)
        assertEquals(SyncStatus.SYNCED, uiItem.syncStatus)
    }
}
