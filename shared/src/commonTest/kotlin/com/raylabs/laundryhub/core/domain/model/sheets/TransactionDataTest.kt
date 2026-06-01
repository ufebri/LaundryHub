package com.raylabs.laundryhub.core.domain.model.sheets

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sampleTx = TransactionData(
        orderID = "ord-1",
        date = "2026-06-01",
        name = "Ray",
        weight = "2.0",
        pricePerKg = "10000",
        totalPrice = "20000",
        paymentStatus = PAID,
        packageType = "Express",
        remark = "None",
        paymentMethod = CASH,
        phoneNumber = "0812",
        dueDate = "2026-06-03"
    )

    @Test
    fun testToIncomeListMapping() {
        val map = mapOf(
            "orderID" to "ord-99",
            "Date" to "2026-06-02",
            "Name" to "John",
            "Weight" to "3.0",
            "Price/kg" to "12000",
            "Total Price" to "36000",
            "(lunas/belum)" to UNPAID_ID,
            "Package" to "Regular",
            "remark" to "Iron only",
            "payment" to QRIS,
            "phoneNumber" to "0853",
            "due date" to "2026-06-04"
        )

        val tx = map.toIncomeList()
        assertEquals("ord-99", tx.orderID)
        assertEquals("2026-06-02", tx.date)
        assertEquals("John", tx.name)
        assertEquals("3.0", tx.weight)
        assertEquals("12000", tx.pricePerKg)
        assertEquals("36000", tx.totalPrice)
        assertEquals(UNPAID_ID, tx.paymentStatus)
        assertEquals("Regular", tx.packageType)
        assertEquals("Iron only", tx.remark)
        assertEquals(QRIS, tx.paymentMethod)
        assertEquals("0853", tx.phoneNumber)
        assertEquals("2026-06-04", tx.dueDate)
    }

    @Test
    fun testIncomeFiltersAndGetters() {
        assertTrue(sampleTx.getAllIncomeData())
        assertFalse(sampleTx.copy(name = "").getAllIncomeData())
        assertFalse(sampleTx.copy(totalPrice = "").getAllIncomeData())

        // isPaid/isUnpaid
        assertTrue(sampleTx.isPaidData())
        assertFalse(sampleTx.isUnpaidData())

        val unpaidTx = sampleTx.copy(paymentStatus = UNPAID_ID)
        assertFalse(unpaidTx.isPaidData())
        assertTrue(unpaidTx.isUnpaidData())

        // paymentMethod
        assertTrue(sampleTx.isCashData())
        assertFalse(sampleTx.isQRISData())

        val qrTx = sampleTx.copy(paymentMethod = QRIS)
        assertFalse(qrTx.isCashData())
        assertTrue(qrTx.isQRISData())
    }

    @Test
    fun testPaidDescription() {
        assertEquals(PAID_BY_CASH, sampleTx.paidDescription())
        assertEquals(PAID_BY_QRIS, sampleTx.copy(paymentMethod = QRIS).paidDescription())
        assertEquals("Paid", sampleTx.copy(paymentMethod = "OTHER").paidDescription())
        assertEquals("Unpaid", sampleTx.copy(paymentStatus = UNPAID_ID).paidDescription())
    }

    @Test
    fun testFilterRangeDateData() {
        val range = RangeDate("2026-05-30", "2026-06-05")
        assertTrue(sampleTx.filterRangeDateData(range))

        val outsideRange = RangeDate("2026-06-02", "2026-06-05")
        assertFalse(sampleTx.filterRangeDateData(outsideRange))

        // Null range dates trigger default bounds
        assertTrue(sampleTx.filterRangeDateData(RangeDate(null, null)))
        
        // Invalid date format in transaction returns false
        assertFalse(sampleTx.copy(date = "invalid").filterRangeDateData(range))
    }

    @Test
    fun testSerialization() {
        val serialized = json.encodeToString(sampleTx)
        val deserialized = json.decodeFromString<TransactionData>(serialized)
        assertEquals(sampleTx, deserialized)

        val range = RangeDate("2026-05-30", "2026-06-05")
        val serializedRange = json.encodeToString(range)
        val deserializedRange = json.decodeFromString<RangeDate>(serializedRange)
        assertEquals(range, deserializedRange)
    }

    @Test
    fun testEnums() {
        assertEquals(FILTER.SHOW_ALL_DATA, FILTER.valueOf("SHOW_ALL_DATA"))
    }
}
