package com.raylabs.laundryhub.backend.util

import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import com.raylabs.laundryhub.core.domain.model.sheets.OrderData
import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncSignatureUtilsTest {

    @Test
    fun `normalizedSyncText trims and lowercases`() {
        assertEquals("test", "  TeSt  ".normalizedSyncText())
    }

    @Test
    fun `normalizedSyncNumberText extracts digits or falls back to lowercase`() {
        assertEquals("12345", " Rp. 12.345 ".normalizedSyncNumberText())
        assertEquals("none", " none ".normalizedSyncNumberText())
    }

    @Test
    fun `normalizedSyncPhoneText works like normalizedSyncNumberText`() {
        assertEquals("620812345678", " +62-0812-3456-78 ".normalizedSyncPhoneText())
    }

    @Test
    fun `normalizedSyncStatusText normalizes status keys`() {
        assertEquals("paid", " Lunas ".normalizedSyncStatusText())
        assertEquals("paid", " paid ".normalizedSyncStatusText())
        assertEquals("unpaid", " Belum ".normalizedSyncStatusText())
        assertEquals("unpaid", " unpaid ".normalizedSyncStatusText())
        assertEquals("unpaid", "   ".normalizedSyncStatusText())
        assertEquals("custom", " CUSTOM ".normalizedSyncStatusText())
    }

    @Test
    fun `order syncVerificationSignature matches expected formatting`() {
        val order = OrderData(
            orderId = "123",
            name = "John Doe",
            phoneNumber = "+62-812",
            packageName = "Express",
            priceKg = "10000",
            totalPrice = "20000",
            paidStatus = "Lunas",
            paymentMethod = "Cash",
            remark = "Fast",
            weight = "2.0",
            orderDate = "10/05/2026",
            dueDate = "11/05/2026"
        )
        val signature = order.syncVerificationSignature()
        val parts = signature.split("\u001F")
        
        assertEquals(12, parts.size)
        assertEquals("123", parts[0])
        assertEquals("10/05/2026", parts[1])
        assertEquals("john doe", parts[2])
        assertEquals("20", parts[3]) // extracts digits from 2.0 -> 20
        assertEquals("10000", parts[4])
        assertEquals("20000", parts[5])
        assertEquals("paid", parts[6])
        assertEquals("express", parts[7])
        assertEquals("fast", parts[8])
        assertEquals("cash", parts[9])
        assertEquals("62812", parts[10])
        assertEquals("11/05/2026", parts[11])
    }

    @Test
    fun `outcome syncVerificationSignature matches expected formatting`() {
        val outcome = OutcomeData(
            id = "OUT-01",
            date = "09/05/2026",
            purpose = "Soap",
            price = "50,000",
            remark = "Urgent",
            payment = "Cash"
        )
        val signature = outcome.syncVerificationSignature()
        val parts = signature.split("\u001F")
        
        assertEquals(6, parts.size)
        assertEquals("OUT-01", parts[0])
        assertEquals("09/05/2026", parts[1])
        assertEquals("soap", parts[2])
        assertEquals("50000", parts[3])
        assertEquals("urgent", parts[4])
        assertEquals("cash", parts[5])
    }

    @Test
    fun `package syncVerificationSignature matches expected formatting`() {
        val packageData = PackageData(
            name = "Dry Clean",
            price = "15000",
            duration = "12",
            unit = "Hours"
        )
        val signature = packageData.syncVerificationSignature()
        val parts = signature.split("\u001F")
        
        assertEquals(4, parts.size)
        assertEquals("dry clean", parts[0])
        assertEquals("15000", parts[1])
        assertEquals("12", parts[2])
        assertEquals("hours", parts[3])
    }

    @Test
    fun `gross syncVerificationSignature matches expected formatting`() {
        val gross = GrossData(
            month = "May 2026",
            totalNominal = "5,000,000",
            orderCount = "150",
            tax = "50000"
        )
        val signature = gross.syncVerificationSignature()
        val parts = signature.split("\u001F")
        
        assertEquals(4, parts.size)
        assertEquals("may 2026", parts[0])
        assertEquals("5000000", parts[1])
        assertEquals("150", parts[2])
        assertEquals("50000", parts[3])
    }

    @Test
    fun `spreadsheet syncVerificationSignature matches expected formatting`() {
        val spreadsheet = SpreadsheetData(
            key = "SpreadsheetKey",
            value = "Value123"
        )
        val signature = spreadsheet.syncVerificationSignature()
        val parts = signature.split("\u001F")
        
        assertEquals(2, parts.size)
        assertEquals("spreadsheetkey", parts[0])
        assertEquals("123", parts[1])
    }
}
