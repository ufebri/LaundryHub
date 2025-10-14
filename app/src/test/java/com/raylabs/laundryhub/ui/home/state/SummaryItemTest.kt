package com.raylabs.laundryhub.ui.home.state

import androidx.compose.ui.graphics.Color
import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryItemTest {

    @Test
    fun `toReadyToPick maps SpreadsheetData correctly`() {
        val data = SpreadsheetData(key = READY_TO_PICKUP, value = "5")
        val summary = data.toReadyToPick()
        assertEquals("Ready To Pick", summary.title)
        assertEquals("5 Orders", summary.body)
        assertEquals("Send message →", summary.footer)
        assertEquals(Color(0xFFFEF7FF), summary.backgroundColor)
        assertEquals(Color.Black, summary.textColor)
    }

    @Test
    fun `toReadyToPick returns empty footer when value is 0`() {
        val data = SpreadsheetData(key = READY_TO_PICKUP, value = "0")
        val summary = data.toReadyToPick()
        assertEquals("", summary.footer)
    }

    @Test
    fun `toNewMember maps zero member correctly`() {
        val data = SpreadsheetData(key = NEW_MEMBER, value = "0")
        val summary = data.toNewMember()
        assertEquals("New Member", summary.title)
        assertEquals("Good Days", summary.body)
        assertEquals("", summary.footer)
        assertEquals(Color(0xFFF9DEDC), summary.backgroundColor)
        assertEquals(Color.Black, summary.textColor)
    }

    @Test
    fun `toNewMember maps with member value correctly`() {
        val data = SpreadsheetData(key = NEW_MEMBER, value = "7")
        val summary = data.toNewMember()
        assertEquals("7 Members", summary.body)
        assertEquals("Last 7 days", summary.footer)
    }

    @Test
    fun `toMonthlyTarget maps percent and status correctly`() {
        val dataList = listOf(
            SpreadsheetData(key = MONTHLY_TARGET_PERCENTAGE, value = "75%"),
            SpreadsheetData(key = MONTHLY_TARGET_STATUS, value = "On Track")
        )
        val summary = dataList.toMonthlyTarget()
        assertEquals("Monthly Target", summary.title)
        assertEquals("75%", summary.body)
        assertEquals("On Track", summary.footer)
        assertEquals(Color(0xFFB3261E), summary.backgroundColor)
    }

    @Test
    fun `toCashOnHand maps value correctly`() {
        val data = SpreadsheetData(key = CASH_ON_HAND, value = "Rp 3.000.000")
        val summary = data.toCashOnHand()
        assertEquals("Cash On Hand", summary.title)
        assertEquals("Rp 3.000.000", summary.body)
        assertEquals("", summary.footer)
        assertEquals(Color(0xFF6750A4), summary.backgroundColor)
    }

    @Test
    fun `toUI maps spreadsheet list to correct order and types`() {
        val dataList = listOf(
            SpreadsheetData(key = READY_TO_PICKUP, value = "3"),
            SpreadsheetData(key = NEW_MEMBER, value = "2"),
            SpreadsheetData(key = MONTHLY_TARGET_PERCENTAGE, value = "90%"),
            SpreadsheetData(key = MONTHLY_TARGET_STATUS, value = "Above Target"),
            SpreadsheetData(key = CASH_ON_HAND, value = "Rp 1.500.000")
        )
        val summaryList = dataList.toUI()
        // Harus urut: ReadyToPick, NewMember, MonthlyTarget, CashOnHand
        assertEquals(4, summaryList.size)
        assertEquals("Ready To Pick", summaryList[0].title)
        assertEquals("New Member", summaryList[1].title)
        assertEquals("Monthly Target", summaryList[2].title)
        assertEquals("Cash On Hand", summaryList[3].title)
    }
}