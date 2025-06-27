package com.raylabs.laundryhub.core.domain.model.sheets

import com.raylabs.laundryhub.ui.common.util.DateUtil
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

    @Test
    fun `toSheetRow returns 22 columns with correct values`() {
        val data = HistoryData(
            orderId = "ORD001",
            customerName = "John Doe",
            packageType = "Express",
            duration = "1d",
            orderDate = "2025-06-15",
            dueDate = "3d",
            status = "CREATED",
            paymentMethod = "Paid By Cash",
            paymentStatus = "Paid By Cash",
            totalPrice = "15000"
        )

        val row = data.toSheetRow()

        // Total kolom harus 22
        assertEquals(22, row.size)

        // Cek nilai-nilai awal
        assertEquals("ORD001", row[0])
        assertEquals("John Doe", row[1])
        assertEquals("Express", row[2])
        assertEquals("1d", row[3])
        assertEquals(DateUtil.getTodayDate("dd/MM/yyyy"), row[4])
        assertEquals(DateUtil.getDueDate("3d"), row[5])
        assertEquals("Pending", row[6]) // STATUS_ORDER_PENDING

        // Kolom kosong (kolom 7 sampai 18)
        for (i in 7..18) {
            assertEquals("", row[i])
        }

        // Kolom akhir
        assertEquals("", row[19])      // getDisplayPaymentMethod
        assertEquals("", row[20])      // getDisplayPaidStatus
        assertEquals("15000", row[21])     // totalPrice
    }
}