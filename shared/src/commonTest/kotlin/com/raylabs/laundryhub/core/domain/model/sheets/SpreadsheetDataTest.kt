package com.raylabs.laundryhub.core.domain.model.sheets

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SpreadsheetDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testToSheetValues() {
        val data = SpreadsheetData("sheet_id", "14E1xk")
        val sheetValues = data.toSheetValues()
        assertEquals(1, sheetValues.size)
        assertEquals("sheet_id", sheetValues[0][0])
        assertEquals("14E1xk", sheetValues[0][1])
    }

    @Test
    fun testSerialization() {
        val data = SpreadsheetData("key", "value")
        val serialized = json.encodeToString(data)
        val deserialized = json.decodeFromString<SpreadsheetData>(serialized)
        assertEquals(data, deserialized)
    }
}
