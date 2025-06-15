package com.raylabs.laundryhub.core.domain.model.sheets

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryDataTest {

    @Test
    fun `toHistoryData should map all fields correctly`() {
        // Given
        val inputMap = mapOf(
            "order_id" to "123",
            "customer_name" to "John Doe",
            "package" to "Express - 6H",
            "duration" to "6h",
            "order_date" to "01/01/2025",
            "due_date" to "01/01/2025 14:00",
            "status" to "Washing",

            "washing_date" to "01/01/2025",
            "washing_machine" to "M1",

            "drying_date" to "01/01/2025",
            "drying_machine" to "D1",

            "ironing_date" to "01/01/2025",
            "ironing_machine" to "I1",

            "folding_date" to "01/01/2025",
            "folding_station" to "F1",

            "packing_date" to "01/01/2025",
            "packing_station" to "P1",

            "ready_date" to "01/01/2025",
            "completed_date" to "01/01/2025",

            "payment_method" to "QRIS",
            "payment_status" to "Paid",
            "total_price" to "10000"
        )

        // When
        val result = inputMap.toHistoryData()

        // Then
        val expected = HistoryData(
            orderId = "123",
            customerName = "John Doe",
            packageType = "Express - 6H",
            duration = "6h",
            orderDate = "01/01/2025",
            dueDate = "01/01/2025 14:00",
            status = "Washing",

            washingDate = "01/01/2025",
            washingMachine = "M1",

            dryingDate = "01/01/2025",
            dryingMachine = "D1",

            ironingDate = "01/01/2025",
            ironingMachine = "I1",

            foldingDate = "01/01/2025",
            foldingStation = "F1",

            packingDate = "01/01/2025",
            packingStation = "P1",

            readyDate = "01/01/2025",
            completedDate = "01/01/2025",

            paymentMethod = "QRIS",
            paymentStatus = "Paid",
            totalPrice = "10000"
        )

        assertEquals(expected, result)
    }

    @Test
    fun `toHistoryData should return empty string when key is missing`() {
        // Given
        val inputMap = emptyMap<String, String>()

        // When
        val result = inputMap.toHistoryData()

        // Then: All fields should be empty
        val allFieldsEmpty = HistoryData(
            orderId = "",
            customerName = "",
            packageType = "",
            duration = "",
            orderDate = "",
            dueDate = "",
            status = "",

            washingDate = "",
            washingMachine = "",

            dryingDate = "",
            dryingMachine = "",

            ironingDate = "",
            ironingMachine = "",

            foldingDate = "",
            foldingStation = "",

            packingDate = "",
            packingStation = "",

            readyDate = "",
            completedDate = "",

            paymentMethod = "",
            paymentStatus = "",
            totalPrice = ""
        )

        assertEquals(allFieldsEmpty, result)
    }

    @Test
    fun `enum value SHOW_ALL_DATA should exist`() {
        val filter = HistoryFilter.valueOf("SHOW_ALL_DATA")
        assertEquals(HistoryFilter.SHOW_ALL_DATA, filter)
    }

    @Test
    fun `enum value SHOW_UNDONE_ORDER should exist`() {
        val filter = HistoryFilter.valueOf("SHOW_UNDONE_ORDER")
        assertEquals(HistoryFilter.SHOW_UNDONE_ORDER, filter)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid enum name should throw exception`() {
        HistoryFilter.valueOf("INVALID_FILTER")
    }
}