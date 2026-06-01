package com.raylabs.laundryhub.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlatformDateTest {

    @Test
    fun testGetTodayDate() {
        val date = PlatformDate.getTodayDate("dd/MM/yyyy")
        assertTrue(date.matches(Regex("\\d{2}/\\d{2}/\\d{4}")))
        
        // Default format
        val defaultDate = PlatformDate.getTodayDate()
        assertTrue(defaultDate.matches(Regex("\\d{2}/\\d{2}/\\d{4}")))
    }

    @Test
    fun testIsToday() {
        val today = PlatformDate.getTodayDate()
        assertTrue(PlatformDate.isToday(today, PlatformDate.STANDARD_DATE_FORMAT))
    }

    @Test
    fun testGetDueDateHours() {
        val startDate = "01-06-2026 08:00"
        val result = PlatformDate.getDueDate("3h", startDate)
        assertEquals("01/06/2026", result)
    }

    @Test
    fun testGetDueDateDays() {
        val startDate = "01-06-2026 08:00"
        val result = PlatformDate.getDueDate("2d", startDate)
        assertEquals("03/06/2026", result)
    }

    @Test
    fun testGetDueDateInvalidDuration() {
        val startDate = "01-06-2026 08:00"
        
        // Non-h/d duration returns startDate unchanged
        assertEquals(startDate, PlatformDate.getDueDate("2w", startDate))
        
        // Invalid number returns startDate
        assertEquals(startDate, PlatformDate.getDueDate("invalidh", startDate))
    }

    @Test
    fun testGetDueDateParseException() {
        // Bad date format triggers catch and returns startDate
        assertEquals("invalid-date", PlatformDate.getDueDate("2d", "invalid-date"))
    }

    @Test
    fun testParseDate() {
        val dateStr = "2026-06-01"
        val millis = PlatformDate.parseDate(dateStr, "yyyy-MM-dd")
        assertNotNull(millis)

        // Invalid format triggers exception and returns null
        assertNull(PlatformDate.parseDate("invalid", "yyyy-MM-dd"))
    }

    @Test
    fun testFormatToLongDate() {
        val dateStr = "2026-06-01"
        val formatted = PlatformDate.formatToLongDate(dateStr, "yyyy-MM-dd", "dd MMMM yyyy")
        assertTrue(formatted.contains("Juni") || formatted.contains("June"))

        // Invalid input returns original string
        assertEquals("invalid", PlatformDate.formatToLongDate("invalid", "yyyy-MM-dd", "dd MMMM yyyy"))
    }
}
