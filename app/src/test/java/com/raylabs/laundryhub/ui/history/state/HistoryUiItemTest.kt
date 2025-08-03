package com.raylabs.laundryhub.ui.history.state

import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.isPaidData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryUiItemTest {

    @Test
    fun `toUiItem maps TransactionData correctly`() {
        val trx = TransactionData(
            orderID = "123",
            name = "Rama",
            date = "01/08/2025",
            totalPrice = "10000",
            packageType = "Reguler",
            paymentStatus = "Paid",
            paymentMethod = "Cash",
            phoneNumber = "081234",
            remark = "",
            weight = "2",
            pricePerKg = "5000",
            dueDate = "01/08/2025"
        )
        val ui = trx.toUiItem()
        assertEquals("123", ui.orderId)
        assertEquals("Rama", ui.name)
        assertEquals("01/08/2025", ui.formattedDate)
        assertEquals("10000", ui.totalPrice)
        assertEquals("Reguler", ui.packageType)
        // Asumsi paidDescription dan isPaidData default-mu sesuai status Paid
        assertEquals(trx.paidDescription(), ui.paymentStatus)
        assertEquals(trx.isPaidData(), ui.isPaid)
    }

    @Test
    fun `toUiItems groups by date and sorts entries by orderId descending`() {
        val trx1 = TransactionData(
            orderID = "10",
            name = "A",
            date = "01/08/2025",
            totalPrice = "10000",
            packageType = "Reguler",
            paymentStatus = "Paid",
            paymentMethod = "Cash",
            phoneNumber = "0812",
            remark = "",
            weight = "2",
            pricePerKg = "5000",
            dueDate = "01/08/2025"
        )
        val trx2 = TransactionData(
            orderID = "20",
            name = "B",
            date = "01/08/2025",
            totalPrice = "20000",
            packageType = "Express",
            paymentStatus = "Unpaid",
            paymentMethod = "Cash",
            phoneNumber = "0813",
            remark = "",
            weight = "3",
            pricePerKg = "7000",
            dueDate = "02/08/2025"
        )
        val trx3 = TransactionData(
            orderID = "5",
            name = "C",
            date = "02/08/2025",
            totalPrice = "15000",
            packageType = "Reguler",
            paymentStatus = "Paid",
            paymentMethod = "Cash",
            phoneNumber = "0814",
            remark = "",
            weight = "2",
            pricePerKg = "7500",
            dueDate = "02/08/2025"
        )

        val result = listOf(trx1, trx2, trx3).toUiItems()

        // Harus ada 2 header (2 tanggal)
        val headers = result.filterIsInstance<HistoryUiItem.Header>()
        assertEquals(2, headers.size)
        assertTrue(headers.any { it.date == "01/08/2025" })
        assertTrue(headers.any { it.date == "02/08/2025" })

        // Cek Entry urutannya benar (orderId descending di group tanggal)
        val entriesFor0108 = result.filterIsInstance<HistoryUiItem.Entry>()
            .filter { it.item.formattedDate == "01/08/2025" }
        assertEquals(2, entriesFor0108.size)
        assertEquals("20", entriesFor0108[0].item.orderId) // orderId 20 harus di depan (descending)
        assertEquals("10", entriesFor0108[1].item.orderId)
    }
}