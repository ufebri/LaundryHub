package com.raylabs.laundryhub.core.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpreadsheetIdParserTest {

    @Test
    fun `normalize returns spreadsheet id from full google sheets url`() {
        val input = "https://docs.google.com/spreadsheets/d/1AbCdEfGhIjKlMnOpQrStUvWxYz1234567890/edit#gid=0"

        val result = SpreadsheetIdParser.normalize(input)

        assertEquals("1AbCdEfGhIjKlMnOpQrStUvWxYz1234567890", result)
    }

    @Test
    fun `normalize returns raw spreadsheet id when it is valid`() {
        val input = "1AbCdEfGhIjKlMnOpQrStUvWxYz1234567890"

        val result = SpreadsheetIdParser.normalize(input)

        assertEquals(input, result)
    }

    @Test
    fun `normalize returns null when input is blank or invalid`() {
        assertNull(SpreadsheetIdParser.normalize(""))
        assertNull(SpreadsheetIdParser.normalize("not-a-sheet"))
    }
}
