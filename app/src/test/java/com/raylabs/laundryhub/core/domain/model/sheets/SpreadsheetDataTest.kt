package com.raylabs.laundryhub.core.domain.model.sheets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SpreadsheetDataTest {

    @Test
    fun `SpreadsheetData should hold key and value correctly`() {
        val data = SpreadsheetData(key = "foo", value = "bar")

        assertEquals("foo", data.key)
        assertEquals("bar", data.value)
    }

    @Test
    fun `SpreadsheetData equality should work for same values`() {
        val a = SpreadsheetData("key1", "value1")
        val b = SpreadsheetData("key1", "value1")

        assertEquals(a, b)
    }

    @Test
    fun `SpreadsheetData equality should fail for different values`() {
        val a = SpreadsheetData("key1", "value1")
        val b = SpreadsheetData("key2", "value2")

        assertNotEquals(a, b)
    }

    @Test
    fun `SpreadsheetData toString should return expected format`() {
        val data = SpreadsheetData("example", "123")
        val expected = "SpreadsheetData(key=example, value=123)"
        assertEquals(expected, data.toString())
    }
}