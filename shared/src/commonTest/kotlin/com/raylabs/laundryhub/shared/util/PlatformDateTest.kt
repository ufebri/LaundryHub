package com.raylabs.laundryhub.shared.util

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformDateTest {

    @Test
    fun testGetTodayDate() {
        val date = PlatformDate.getTodayDate("dd/MM/yyyy")
        assertTrue(date.matches(Regex("\\d{2}/\\d{2}/\\d{4}")))
    }

    @Test
    fun testIsToday() {
        val today = PlatformDate.getTodayDate()
        assertTrue(PlatformDate.isToday(today, PlatformDate.STANDARD_DATE_FORMAT))
    }
}
