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
        
        // Assert properties to cover compiler-generated getters
        assertEquals("SUCCESS", deserialized.status)
        assertEquals("Order created", deserialized.message)
        assertEquals("ord-1", deserialized.orderId)
    }

    @Test
    fun testOrderDataSerialization() {
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
        val serialized = json.encodeToString(order)
        val deserialized = json.decodeFromString<OrderData>(serialized)
        assertEquals(order, deserialized)

        // Read all property getters explicitly to cover compiler-generated accessor bytecodes
        assertEquals("ord-1", deserialized.orderId)
        assertEquals("Ray", deserialized.name)
        assertEquals("0812", deserialized.phoneNumber)
        assertEquals("Express", deserialized.packageName)
        assertEquals("10000", deserialized.priceKg)
        assertEquals("20000", deserialized.totalPrice)
        assertEquals("Paid by Cash", deserialized.paidStatus)
        assertEquals("Cash", deserialized.paymentMethod)
        assertEquals("None", deserialized.remark)
        assertEquals("2.0", deserialized.weight)
        assertEquals("2026-06-01", deserialized.orderDate)
        assertEquals("2026-06-03", deserialized.dueDate)
        assertEquals("cash", deserialized.getSpreadSheetPaymentMethod)
        assertEquals("lunas", deserialized.getSpreadSheetPaidStatus)
    }

    @Test
    fun testOrderDataPaidDescription() {
        val orderUnpaid = OrderData(
            orderId = "ord-1",
            name = "Ray",
            phoneNumber = "0812",
            packageName = "Express",
            priceKg = "10000",
            totalPrice = "20000",
            paidStatus = "belum",
            paymentMethod = "Unpaid",
            remark = "None",
            weight = "2.0",
            orderDate = "2026-06-01",
            dueDate = "2026-06-03"
        )
        assertEquals("Unpaid", orderUnpaid.paidDescription())

        val orderCash = orderUnpaid.copy(paidStatus = "lunas", paymentMethod = "cash")
        assertEquals(PAID_BY_CASH, orderCash.paidDescription())

        val orderQris = orderUnpaid.copy(paidStatus = "paid", paymentMethod = "qris")
        assertEquals(PAID_BY_QRIS, orderQris.paidDescription())

        val orderPaidOther = orderUnpaid.copy(paidStatus = "lunas", paymentMethod = "OTHER")
        assertEquals("Paid", orderPaidOther.paidDescription())
    }

    @Test
    fun testOrderDataBlankDateFallbacks() {
        val order = OrderData(
            orderId = "ord-1",
            name = "Ray",
            phoneNumber = "0812",
            packageName = "Express",
            priceKg = "10000",
            totalPrice = "20000",
            paidStatus = "belum",
            paymentMethod = "Unpaid",
            remark = "None",
            weight = "2.0",
            orderDate = "",
            dueDate = " 2 Hari "
        )
        // Sanitized orderDate fallback to today's date in getSpreadSheetDueDate
        assertTrue(order.getSpreadSheetDueDate.isNotBlank())

        // Blank orderDate in toSheetValues
        val sheetValues = order.toSheetValues()
        assertTrue(sheetValues[0][1].isNotBlank())

        // Not blank orderDate in toUpdateSheetValues
        val orderWithDate = order.copy(orderDate = "2026-06-01")
        val updateValuesWithDate = orderWithDate.toUpdateSheetValues("2026-05-30")
        assertEquals("2026-06-01", updateValuesWithDate[0][1])
    }

    @Test
    fun testPaymentMethodLists() {
        assertTrue(paymentMethodList.contains(UNPAID))
        assertTrue(paymentMethodOutcomeList.contains(PAID_BY_PERSONAL))
    }
}
