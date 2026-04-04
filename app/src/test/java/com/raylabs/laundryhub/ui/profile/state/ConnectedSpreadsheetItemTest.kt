package com.raylabs.laundryhub.ui.profile.state

import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectedSpreadsheetItemTest {

    @Test
    fun `shortSpreadsheetId returns original id when already short`() {
        val item = ConnectedSpreadsheetItem(
            spreadsheetId = "sheet-123456",
            spreadsheetName = "Laundry A",
            spreadsheetUrl = null
        )

        assertEquals("sheet-123456", item.shortSpreadsheetId)
    }

    @Test
    fun `shortSpreadsheetId truncates long id`() {
        val item = ConnectedSpreadsheetItem(
            spreadsheetId = "1AbCdEfGhIjKlMnOpQrStUvWxYz1234567890",
            spreadsheetName = "Laundry A",
            spreadsheetUrl = null
        )

        assertEquals("1AbCdEfG...567890", item.shortSpreadsheetId)
    }

    @Test
    fun `toUi returns null when spreadsheet id is missing`() {
        assertNull(SpreadsheetConfig().toUi())
    }

    @Test
    fun `toUi uses fallback spreadsheet name when name is missing`() {
        val item = SpreadsheetConfig(
            spreadsheetId = "sheet-123",
            spreadsheetUrl = "https://docs.google.com/spreadsheets/d/sheet-123/edit"
        ).toUi()

        assertEquals("sheet-123", item?.spreadsheetId)
        assertEquals("LaundryHub Spreadsheet", item?.spreadsheetName)
    }
}
