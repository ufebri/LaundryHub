package com.raylabs.laundryhub.backend.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class BackendDateParserTest {

    @Test
    fun `parseSupportedLaundryDate returns null for null or blank input`() {
        assertNull(parseSupportedLaundryDate(null))
        assertNull(parseSupportedLaundryDate("   "))
        assertNull(parseSupportedLaundryDate(""))
    }

    @Test
    fun `parseSupportedLaundryDate parses standard formats successfully`() {
        // dd/MM/yyyy
        val date1 = parseSupportedLaundryDate("01/06/2026")
        assertNotNull(date1)
        
        // yyyy-MM-dd
        val date2 = parseSupportedLaundryDate("2026-06-01")
        assertNotNull(date2)

        // dd-MM-yyyy
        val date3 = parseSupportedLaundryDate("01-06-2026")
        assertNotNull(date3)
    }

    @Test
    fun `parseSupportedLaundryDate parses date with times successfully`() {
        // dd/MM/yyyy HH:mm (parsed successfully via dd/MM/yyyy prefix)
        val date1 = parseSupportedLaundryDate("01/06/2026 15:30")
        assertNotNull(date1)

        val cal = Calendar.getInstance().apply { time = date1!! }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH))
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `parseSupportedLaundryDate parses word formats successfully`() {
        // dd MMM yyyy (English)
        val date1 = parseSupportedLaundryDate("01 Jun 2026")
        assertNotNull(date1)

        // dd MMMM yyyy (Indonesian)
        val date2 = parseSupportedLaundryDate("01 Juni 2026")
        assertNotNull(date2)
    }

    @Test
    fun `parseSupportedLaundryDate returns null for invalid formats`() {
        assertNull(parseSupportedLaundryDate("invalid-date-format"))
        assertNull(parseSupportedLaundryDate("32/12/2026")) // Invalid day
        assertNull(parseSupportedLaundryDate("12/13/2026")) // Invalid month
    }
}
