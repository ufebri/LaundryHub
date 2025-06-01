package com.raylabs.laundryhub.ui.common.util

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class DateUtilTest {

    @Test
    fun `getTodayDate returns today's date in yyyy-MM-dd format`() {
        val expectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val actualDate = DateUtil.getTodayDate()
        assertEquals(expectedDate, actualDate)
    }

    @Test
    fun `isToday returns true for today's date`() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        assertTrue(DateUtil.isToday(today))
    }

    @Test
    fun `isToday returns false for different date`() {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.time

        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterday)
        assertFalse(DateUtil.isToday(yesterdayStr))
    }

    @Test
    fun `parseDate returns correct Date object`() {
        val input = "2024-06-01"
        val parsedDate = DateUtil.parseDate(input)
        val expected = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(input)
        assertEquals(expected, parsedDate)
    }

    @Test
    fun `parseDate returns null for invalid input`() {
        val invalidInput = "invalid-date"
        val result = DateUtil.parseDate(invalidInput)
        assertNull(result)
    }
}