package com.raylabs.laundryhub.core.domain.model.sheets

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testPaymentMethodMappers() {
        assertEquals(CASH, getDisplayPaymentMethod(PAID_BY_CASH))
        assertEquals(CASH, getDisplayPaymentMethod(CASH))
        assertEquals(QRIS, getDisplayPaymentMethod(PAID_BY_QRIS))
        assertEquals(QRIS, getDisplayPaymentMethod(QRIS))
        assertEquals(UNPAID, getDisplayPaymentMethod(UNPAID))
        assertEquals(UNPAID, getDisplayPaymentMethod(UNPAID_ID))
        assertEquals("", getDisplayPaymentMethod("INVALID"))
    }

    @Test
    fun testPaidStatusMappers() {
        assertEquals(PAID, getDisplayPaidStatus(PAID_BY_CASH))
        assertEquals(PAID, getDisplayPaidStatus(PAID_BY_QRIS))
        assertEquals(PAID, getDisplayPaidStatus(PAID))
        assertEquals(PAID, getDisplayPaidStatus("paid"))
        assertEquals(UNPAID_ID, getDisplayPaidStatus(UNPAID))
        assertEquals(UNPAID_ID, getDisplayPaidStatus(UNPAID_ID))
        assertEquals(UNPAID_ID, getDisplayPaidStatus("unpaid"))
        assertEquals("", getDisplayPaidStatus("INVALID"))
    }

    @Test
    fun testIsPaidAndUnpaidHelpers() {
        assertTrue(isPaidStatusValue(PAID))
        assertTrue(isPaidStatusValue("paid"))
        assertFalse(isPaidStatusValue(UNPAID))

        assertTrue(isUnpaidStatusValue(UNPAID))
        assertTrue(isUnpaidStatusValue("unpaid"))
        assertFalse(isUnpaidStatusValue(PAID))

        // Blank treatment
        assertFalse(isUnpaidStatusValue("", treatBlankAsUnpaid = false))
        assertTrue(isUnpaidStatusValue("", treatBlankAsUnpaid = true))
    }

    @Test
    fun testOrderDataSpreadsheetDueDate() {
        val order = OrderData(
            orderId = "ord-1",
            name = "Ray",
            phoneNumber = "0812",
            packageName = "Express",
            priceKg = "10000",
            totalPrice = "20000",
            paidStatus = "Paid by Cash",
            paymentMethod = "Cash",
            remark = "None",
            weight = "2.0",
            orderDate = "2026-06-01",
            dueDate = " 2 Hari "
        )

        // Trimmed check
        assertEquals("ord-1", order.orderId)
        assertEquals("Ray", order.name)

        // Blank check
        val orderBlankDue = order.copy(dueDate = " ")
        assertEquals("", orderBlankDue.getSpreadSheetDueDate)

        // Containing dash/slash checks
        val orderWithDateDue = order.copy(dueDate = "2026-06-03")
        assertEquals("2026-06-03", orderWithDateDue.getSpreadSheetDueDate)

        val orderWithSlashDue = order.copy(dueDate = "2026/06/03")
        assertEquals("2026/06/03", orderWithSlashDue.getSpreadSheetDueDate)
    }

    @Test
    fun testOrderDataSheetValues() {
        val order = OrderData(
            orderId = "ord-1",
            name = "Ray",
            phoneNumber = "0812",
            packageName = "Express",
            priceKg = "10000",
            totalPrice = "20000",
            paidStatus = "Paid by Cash",
            paymentMethod = "Cash",
            remark = "None",
            weight = "2.0",
            orderDate = "2026-06-01",
            dueDate = "2026-06-03"
        )

        val sheetValues = order.toSheetValues()
        assertEquals(1, sheetValues.size)
        assertEquals("ord-1", sheetValues[0][0])
        assertEquals("2026-06-01", sheetValues[0][1])
        assertEquals("Ray", sheetValues[0][2])
        assertEquals("2.0", sheetValues[0][3])
        assertEquals("10000", sheetValues[0][4])
        assertEquals("20000", sheetValues[0][5])
        assertEquals(PAID, sheetValues[0][6])
        assertEquals("Express", sheetValues[0][7])
        assertEquals("None", sheetValues[0][8])
        assertEquals(CASH, sheetValues[0][9])
        assertEquals("0812", sheetValues[0][10])
        assertEquals("2026-06-03", sheetValues[0][11])
    }

    @Test
    fun testOrderDataUpdateSheetValues() {
        val order = OrderData(
            orderId = "ord-1",
            name = "Ray",
            phoneNumber = "0812",
            packageName = "Express",
            priceKg = "10000",
            totalPrice = "20000",
            paidStatus = "Paid by Cash",
            paymentMethod = "Cash",
            remark = "None",
            weight = "2.0",
            orderDate = "",
            dueDate = "2026-06-03"
        )

        val updateValues = order.toUpdateSheetValues("2026-05-30")
        assertEquals("2026-05-30", updateValues[0][1])

        val updateValuesFallback = order.toUpdateSheetValues("")
        assertTrue(updateValuesFallback[0][1].isNotBlank())
    }

    @Test
    fun testCreateOrderResponseSerialization() {
        val response = CreateOrderResponse("SUCCESS", "Order created", "ord-1")
        val serialized = json.encodeToString(response)
        val deserialized = json.decodeFromString<CreateOrderResponse>(serialized)
        assertEquals(response, deserialized)
    }
}
