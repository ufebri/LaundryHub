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
            paymentMethod = PAID_BY_CASH, remark = "", weight = "", dueDate = "3d"
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
            paymentMethod = "", remark = "", weight = "", dueDate = "3d"
        )
        assertEquals("lunas", cash.getSpreadSheetPaidStatus)

        val qris = cash.copy(paidStatus = PAID_BY_QRIS)
        assertEquals("lunas", qris.getSpreadSheetPaidStatus)

        val unpaid = cash.copy(paidStatus = UNPAID)
        assertEquals("belum", unpaid.getSpreadSheetPaidStatus)
    }

    @Test
    fun `getSpreadSheetDueDate should calculate correct due date from duration`() {
        val today = DateUtil.getTodayDate("dd-MM-yyyy") + " 08:00"
        val expected = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).parse(today)
        val cal = Calendar.getInstance().apply {
            time = expected!!
            add(Calendar.DAY_OF_MONTH, 3)
        }
        val expectedDue = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(cal.time)

        val data = OrderData(
            orderId = "1", name = "", phoneNumber = "", packageName = "",
            priceKg = "", totalPrice = "", paidStatus = "", paymentMethod = "",
            remark = "", weight = "", dueDate = "3d"
        )

        assertEquals(expectedDue, data.getSpreadSheetDueDate)
    }
}