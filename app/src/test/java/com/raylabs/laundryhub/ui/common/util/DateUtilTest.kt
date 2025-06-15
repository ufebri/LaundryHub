package com.raylabs.laundryhub.ui.common.util

import com.raylabs.laundryhub.ui.common.util.DateUtil.formatToLongDate
import com.raylabs.laundryhub.ui.common.util.DateUtil.getDueDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    @Test
    fun `given 6 hours duration, should return date plus 6 hours`() {
        val startDate = "01-06-2025 08:00"
        val expected = "01-06-2025 14:00"

        val result = getDueDate("6h", startDate)

        assertEquals(expected, result)
    }

    @Test
    fun `given 1 day duration, should return date plus 1 day`() {
        val startDate = "01-06-2025 08:00"
        val expected = "02-06-2025 08:00"

        val result = getDueDate("1d", startDate)

        assertEquals(expected, result)
    }

    @Test
    fun `given 3 days duration, should return date plus 3 days`() {
        val startDate = "01-06-2025 08:00"
        val expected = "04-06-2025 08:00"

        val result = getDueDate("3d", startDate)

        assertEquals(expected, result)
    }

    @Test
    fun `given invalid duration, should return start date unchanged`() {
        val startDate = "01-06-2025 08:00"
        val result = getDueDate("abc", startDate)

        assertEquals(startDate, result)
    }

    @Test
    fun `given malformed start date, should return original start date`() {
        val malformedDate = "bad-date"
        val result = getDueDate("1d", malformedDate)

        assertEquals(malformedDate, result)
    }

    @Test
    fun `format valid date to long format`() {
        val result = formatToLongDate("2025-06-15", "yyyy-MM-dd", "dd MMMM yyyy")
        val expected = if (Locale.getDefault().language == "id") {
            "15 Juni 2025"
        } else {
            "15 June 2025"
        }
        assertEquals(expected, result)
    }

    @Test
    fun `return original string when invalid date format`() {
        val result = formatToLongDate("invalid-date", "yyyy-MM-dd", "dd MMMM yyyy")
        assertEquals("invalid-date", result)
    }

    @Test
    fun `custom input and output format`() {
        val result = formatToLongDate("15/06/2025", "dd/MM/yyyy", "MMMM dd, yyyy")
        val expected = if (Locale.getDefault().language == "id") {
            "Juni 15, 2025"
        } else {
            "June 15, 2025"
        }
        assertEquals(expected, result)
    }
}