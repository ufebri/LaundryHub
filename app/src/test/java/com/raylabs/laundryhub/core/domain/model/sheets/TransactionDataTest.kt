package com.raylabs.laundryhub.core.domain.model.sheets

import com.raylabs.laundryhub.ui.common.util.DateUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionDataTest {

    private fun today(): String = DateUtil.getTodayDate("yyyy-MM-dd")

    @Test
    fun `toIncomeList parses all fields correctly`() {
        val map = mapOf(
            "orderID" to "ORD123",
            "Date" to today(),
            "Name" to "Alice",
            "Weight" to "5",
            "Price/kg" to "5000",
            "Total Price" to "25000",
            "(lunas/belum)" to "Paid",
            "Package" to "Express",
            "remark" to "Urgent",
            "payment" to "QRIS",
            "phoneNumber" to "08123456789",
            "orderStatus" to "DONE",
            "station" to "Dryer",
            "due date" to "3d"
        )

        val result = map.toIncomeList()

        assertEquals("ORD123", result.orderID)
        assertEquals("Alice", result.name)
        assertEquals("QRIS", result.paymentMethod)
        assertEquals("Urgent", result.remark)
    }

    @Test
    fun `getAllIncomeData returns true only if name and totalPrice are present`() {
        val data = TransactionData(
            orderID = "", date = "", name = "Bob", weight = "", pricePerKg = "",
            totalPrice = "10000", paymentStatus = "", packageType = "", remark = "",
            paymentMethod = "", phoneNumber = "", orderStatus = "", station = "", dueDate = ""
        )
        assertTrue(data.getAllIncomeData())

        val incomplete = data.copy(name = "", totalPrice = "")
        assertFalse(incomplete.getAllIncomeData())
    }

    @Test
    fun `getTodayIncomeData returns true for today's date`() {
        val data = TransactionData(
            orderID = "", date = today(), name = "", weight = "", pricePerKg = "",
            totalPrice = "", paymentStatus = "", packageType = "", remark = "",
            paymentMethod = "", phoneNumber = "", orderStatus = "", station = "", dueDate = ""
        )
        assertTrue(data.getTodayIncomeData())
    }

    @Test
    fun `filterRangeDateData returns true when in range`() {
        val data = TransactionData(
            orderID = "", date = "2024-06-15", name = "", weight = "", pricePerKg = "",
            totalPrice = "", paymentStatus = "", packageType = "", remark = "",
            paymentMethod = "", phoneNumber = "", orderStatus = "", station = "", dueDate = ""
        )
        val range = RangeDate("2024-06-01", "2024-06-30")
        assertTrue(data.filterRangeDateData(range))

        val outOfRange = RangeDate("2024-07-01", "2024-07-31")
        assertFalse(data.filterRangeDateData(outOfRange))
    }

    @Test
    fun `isUnpaidData detects unpaid correctly`() {
        val unpaid = TransactionData("", "", "", "", "", "", "Unpaid", "", "", "", "", "", "", "")
        val empty = unpaid.copy(paymentStatus = "")
        assertTrue(unpaid.isUnpaidData())
        assertTrue(empty.isUnpaidData())
    }

    @Test
    fun `isPaidData detects paid correctly`() {
        val paid = TransactionData("", "", "", "", "", "", PAID, "", "", "", "", "", "", "")
        assertTrue(paid.isPaidData())

        val notPaid = paid.copy(paymentStatus = "Unpaid")
        assertFalse(notPaid.isPaidData())
    }

    @Test
    fun `isQRISData and isCashData detect payment method correctly`() {
        val qris = TransactionData("", "", "", "", "", "", "", "", "", "QRIS", "", "", "", "")
        val cash = qris.copy(paymentMethod = "Cash")

        assertTrue(qris.isQRISData())
        assertFalse(qris.isCashData())

        assertTrue(cash.isCashData())
        assertFalse(cash.isQRISData())
    }

    @Test
    fun `paidDescription returns correct string`() {
        val paidQris =
            TransactionData("", "", "", "", "", "", PAID, "", "", QRIS, "", "", "", "")
        assertEquals("Paid by Qris", paidQris.paidDescription())

        val unpaid = paidQris.copy(paymentStatus = "Unpaid")
        assertEquals("Unpaid", unpaid.paidDescription())
    }
}