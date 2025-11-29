package com.raylabs.laundryhub.ui.outcome.state

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.ui.common.util.DateUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryItemTest {

    @Test
    fun `toEntryItemUI maps fields and formats date`() {
        val data = OutcomeData(
            id = "10",
            date = "01/01/2025",
            purpose = "Supplies",
            price = "1000",
            remark = "-",
            payment = "cash"
        )

        val item = data.toEntryItemUI()

        assertEquals("10", item.id)
        assertEquals("Supplies", item.name)
        assertEquals(DateUtil.formatToLongDate("01/01/2025", DateUtil.STANDARD_DATE_FORMATED), item.date)
        assertEquals("1000", item.price)
        assertEquals("Paid by Cash", item.paymentStatus)
        assertEquals(TypeCard.OUTCOME, item.typeCard)
    }

    @Test
    fun `toDateListUiItems groups by date adds header and sorts by id desc`() {
        val list = listOf(
            OutcomeData("1", "02/02/2025", "A", "100", "", "cash"),
            OutcomeData("3", "02/02/2025", "C", "300", "", "cash"),
            OutcomeData("2", "01/02/2025", "B", "200", "", "cash")
        )

        val result = list.toDateListUiItems()

        // Expect headers for two dates, entries sorted desc by id within date
        assertTrue(result[0] is DateListItemUI.Header && (result[0] as DateListItemUI.Header).date == "02/02/2025")
        assertTrue(result[1] is DateListItemUI.Entry && (result[1] as DateListItemUI.Entry).item.id == "3")
        assertTrue(result[2] is DateListItemUI.Entry && (result[2] as DateListItemUI.Entry).item.id == "1")
        assertTrue(result[3] is DateListItemUI.Header && (result[3] as DateListItemUI.Header).date == "01/02/2025")
        assertTrue(result[4] is DateListItemUI.Entry && (result[4] as DateListItemUI.Entry).item.id == "2")
    }
}
