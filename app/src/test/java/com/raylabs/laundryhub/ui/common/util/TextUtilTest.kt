package com.raylabs.laundryhub.ui.common.util

import com.raylabs.laundryhub.ui.common.util.TextUtil.capitalizeFirstLetter
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
}