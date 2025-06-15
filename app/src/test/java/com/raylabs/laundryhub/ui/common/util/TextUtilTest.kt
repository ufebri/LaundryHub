package com.raylabs.laundryhub.ui.common.util

import com.raylabs.laundryhub.ui.common.util.TextUtil.capitalizeFirstLetter
import com.raylabs.laundryhub.ui.common.util.TextUtil.toRupiahFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class TextUtilTest {

    @Test
    fun `capitalizeFirstLetter should capitalize lowercase first character`() {
        val input = "laundry"
        val expected = "Laundry"
        val result = input.capitalizeFirstLetter()
        assertEquals(expected, result)
    }

    @Test
    fun `capitalizeFirstLetter should not change already capitalized input`() {
        val input = "Laundry"
        val expected = "Laundry"
        val result = input.capitalizeFirstLetter()
        assertEquals(expected, result)
    }

    @Test
    fun `capitalizeFirstLetter should handle empty string`() {
        val input = ""
        val expected = ""
        val result = input.capitalizeFirstLetter()
        assertEquals(expected, result)
    }

    @Test
    fun `capitalizeFirstLetter should handle single character`() {
        val input = "l"
        val expected = "L"
        val result = input.capitalizeFirstLetter()
        assertEquals(expected, result)
    }

    @Test
    fun `capitalizeFirstLetter should handle input with mixed case`() {
        val input = "lAUNdry"
        val expected = "Laundry"
        val result = input.capitalizeFirstLetter()
        assertEquals(expected, result)
    }

    @Test
    fun `should format plain number to Indonesian rupiah format`() {
        val input = "5000"
        val expected = "5.000"
        val result = input.toRupiahFormat()
        assertEquals(expected, result)
    }

    @Test
    fun `should return empty string for non-numeric input`() {
        val input = "abc"
        val result = input.toRupiahFormat()
        assertEquals("", result)
    }

    @Test
    fun `should return empty string for empty input`() {
        val input = ""
        val result = input.toRupiahFormat()
        assertEquals("", result)
    }

    @Test
    fun `should format large number with proper grouping`() {
        val input = "1500000"
        val expected = "1.500.000"
        val result = input.toRupiahFormat()
        assertEquals(expected, result)
    }
}