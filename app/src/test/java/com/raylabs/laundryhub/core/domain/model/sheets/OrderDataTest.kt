package com.raylabs.laundryhub.core.domain.model.sheets

import com.raylabs.laundryhub.ui.common.util.DateUtil
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OrderDataTest {

    @Test
    fun `getSpreadSheetPaymentMethod should return correct codes`() {
        val cash = OrderData(
            orderId = "1", name = "", phoneNumber = "", packageName = "",
            priceKg = "", totalPrice = "", paidStatus = PAID_BY_CASH,
            paymentMethod = PAID_BY_CASH, remark = "", weight = "",
            orderDate = "01/01/2025", dueDate = "3d"
        )
        assertEquals("cash", cash.getSpreadSheetPaymentMethod)

        val qris = cash.copy(paymentMethod = PAID_BY_QRIS)
        assertEquals("qris", qris.getSpreadSheetPaymentMethod)

        val unpaid = cash.copy(paymentMethod = UNPAID)
        assertEquals("Unpaid", unpaid.getSpreadSheetPaymentMethod)
    }

    @Test
    fun `getSpreadSheetPaidStatus should return correct status`() {
        val cash = OrderData(
            orderId = "1", name = "", phoneNumber = "", packageName = "",
            priceKg = "", totalPrice = "", paidStatus = PAID_BY_CASH,
            paymentMethod = "", remark = "", weight = "",
            orderDate = "01/01/2025", dueDate = "3d"
        )
        assertEquals("lunas", cash.getSpreadSheetPaidStatus)

        val qris = cash.copy(paidStatus = PAID_BY_QRIS)
        assertEquals("lunas", qris.getSpreadSheetPaidStatus)

        val unpaid = cash.copy(paidStatus = UNPAID)
        assertEquals("belum", unpaid.getSpreadSheetPaidStatus)
    }

    @Test
    fun `getSpreadSheetDueDate should calculate correct due date from duration`() {
        val today = "01-01-2025 08:00"
        val expected = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).parse(today)
        val cal = Calendar.getInstance().apply {
            time = expected!!
            add(Calendar.DAY_OF_MONTH, 3)
        }
        val expectedDue = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)

        val data = OrderData(
            orderId = "1", name = "", phoneNumber = "", packageName = "",
            priceKg = "", totalPrice = "", paidStatus = "", paymentMethod = "",
            remark = "", weight = "",
            orderDate = "01/01/2025", dueDate = "3d"
        )

        assertEquals(expectedDue, data.getSpreadSheetDueDate)
    }

    @Test
    fun `getSpreadSheetDueDate returns blank when due date is blank`() {
        val data = sampleOrderData().copy(dueDate = "   ")

        assertEquals("", data.getSpreadSheetDueDate)
    }

    @Test
    fun `getSpreadSheetDueDate returns unchanged date when due date already uses slash format`() {
        val data = sampleOrderData().copy(dueDate = "04/01/2025")

        assertEquals("04/01/2025", data.getSpreadSheetDueDate)
    }

    @Test
    fun `getSpreadSheetDueDate returns unchanged date when due date already uses dash format`() {
        val data = sampleOrderData().copy(dueDate = "2025-01-04")

        assertEquals("2025-01-04", data.getSpreadSheetDueDate)
    }

    @Test
    fun `toSheetValues uses fallback current date when order date is blank`() {
        val data = sampleOrderData().copy(orderDate = "")

        val row = data.toSheetValues().single()
        assertEquals(DateUtil.getTodayDate(DateUtil.STANDARD_DATE_FORMATED), row[1])
    }

    @Test
    fun `toUpdateSheetValues uses existing date when order date is blank`() {
        val data = sampleOrderData().copy(orderDate = "")

        val row = data.toUpdateSheetValues(existingDate = "09/01/2025").single()
        assertEquals("09/01/2025", row[1])
    }

    @Test
    fun `toUpdateSheetValues prefers explicit order date when present`() {
        val data = sampleOrderData().copy(orderDate = "10/01/2025")

        val row = data.toUpdateSheetValues(existingDate = "09/01/2025").single()
        assertEquals("10/01/2025", row[1])
    }

    @Test
    fun `getDisplayPaymentMethod returns correct label`() {
        assertEquals("cash", getDisplayPaymentMethod(PAID_BY_CASH))
        assertEquals("qris", getDisplayPaymentMethod(PAID_BY_QRIS))
        assertEquals("Unpaid", getDisplayPaymentMethod(UNPAID))
        assertEquals("", getDisplayPaymentMethod("UNKNOWN"))
    }

    @Test
    fun `getDisplayPaidStatus returns correct label`() {
        assertEquals("lunas", getDisplayPaidStatus(PAID_BY_CASH))
        assertEquals("lunas", getDisplayPaidStatus(PAID_BY_QRIS))
        assertEquals("belum", getDisplayPaidStatus(UNPAID))
        assertEquals("", getDisplayPaidStatus("OTHER"))
    }

    private fun sampleOrderData(): OrderData {
        return OrderData(
            orderId = "1",
            name = "Alya",
            phoneNumber = "08123",
            packageName = "Regular",
            priceKg = "7000",
            totalPrice = "14000",
            paidStatus = PAID_BY_CASH,
            paymentMethod = PAID_BY_CASH,
            remark = "Handle with care",
            weight = "2",
            orderDate = "01/01/2025",
            dueDate = "3d"
        )
    }
}
