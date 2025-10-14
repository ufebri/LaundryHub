package com.raylabs.laundryhub.ui.home.state

import androidx.compose.ui.graphics.Color
import com.raylabs.laundryhub.core.domain.model.sheets.PAID
import com.raylabs.laundryhub.core.domain.model.sheets.TransactionData
import com.raylabs.laundryhub.core.domain.model.sheets.paidDescription
import com.raylabs.laundryhub.ui.theme.PurpleLaundryHub
import com.raylabs.laundryhub.ui.theme.RedLaundryHub
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionItemTest {

    @Test
    fun `toUI maps TransactionData to TransactionItem correctly`() {
        val trx = TransactionData(
            orderID = "ORD123",
            name = "Fina",
            date = "01/08/2025",
            totalPrice = "20000",
            packageType = "Reguler",
            paymentStatus = PAID,
            paymentMethod = "Cash",
            phoneNumber = "081234",
            remark = "",
            weight = "3",
            pricePerKg = "7000",
            dueDate = "02/08/2025"
        )

        val items = listOf(trx).toUI()
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("ORD123", item.id)
        assertEquals("Fina", item.name)
        assertEquals("20000,-", item.totalPrice)
        assertEquals(trx.paidDescription(), item.status)
        assertEquals(PurpleLaundryHub, item.statusColor)
        assertEquals("Reguler", item.packageDuration)
    }

    @Test
    fun `toUI sets Rp 0 if totalPrice empty`() {
        val trx = TransactionData(
            orderID = "ORD999",
            name = "Ardi",
            date = "01/08/2025",
            totalPrice = "",
            packageType = "Express",
            paymentStatus = "",
            paymentMethod = "",
            phoneNumber = "0821",
            remark = "",
            weight = "2",
            pricePerKg = "7000",
            dueDate = "02/08/2025"
        )

        val items = listOf(trx).toUI()
        assertEquals("Rp 0,-", items[0].totalPrice)
    }

    @Test
    fun `toColor returns RedLaundryHub for empty string`() {
        assertEquals(RedLaundryHub, "".toColor())
    }

    @Test
    fun `toColor returns PurpleLaundryHub for PAID`() {
        assertEquals(PurpleLaundryHub, PAID.toColor())
    }

    @Test
    fun `toColor returns Black for other strings`() {
        assertEquals(Color.Black, "UNPAID".toColor())
        assertEquals(Color.Black, "random".toColor())
        assertEquals(Color.Black, "Paid ".toColor()) // Case-sensitive
    }
}