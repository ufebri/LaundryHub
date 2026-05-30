package com.raylabs.laundryhub.core.domain.model.sheets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GrossDataTest {

    @Test
    fun `selectCurrentOrLatestGross prefers current month over first row`() {
        val rows = listOf(
            gross(month = "Maret 2025", totalNominal = "Rp1.038.150"),
            gross(month = "April 2026", totalNominal = "Rp4.101.000"),
            gross(month = "Mei 2026", totalNominal = "Rp3.343.000")
        )

        val selected = rows.selectCurrentOrLatestGross(currentYear = 2026, currentMonth = 5)

        assertEquals("Mei 2026", selected?.month)
        assertEquals("Rp3.343.000", selected?.totalNominal)
    }

    @Test
    fun `selectCurrentOrLatestGross falls back to latest parseable month`() {
        val rows = listOf(
            gross(month = "Mei 2025"),
            gross(month = "April 2026"),
            gross(month = "Maret 2026")
        )

        val selected = rows.selectCurrentOrLatestGross(currentYear = 2026, currentMonth = 12)

        assertEquals("April 2026", selected?.month)
    }

    @Test
    fun `selectCurrentOrLatestGross falls back to last nonblank row when months are invalid`() {
        val rows = listOf(
            gross(month = "bad month"),
            gross(month = ""),
            gross(month = "manual total")
        )

        val selected = rows.selectCurrentOrLatestGross(currentYear = 2026, currentMonth = 5)

        assertEquals("manual total", selected?.month)
    }

    @Test
    fun `sortedByGrossMonthDescending puts latest month first and invalid months last`() {
        val rows = listOf(
            gross(month = "Maret 2025"),
            gross(month = "Mei 2026"),
            gross(month = "unknown"),
            gross(month = "April 2026")
        )

        val sorted = rows.sortedByGrossMonthDescending()

        assertEquals(listOf("Mei 2026", "April 2026", "Maret 2025", "unknown"), sorted.map { it.month })
    }

    @Test
    fun `parseGrossMonthKey supports Indonesian and English month names`() {
        assertEquals(202605, parseGrossMonthKey("Mei 2026"))
        assertEquals(202605, parseGrossMonthKey("may 2026"))
        assertEquals(202601, parseGrossMonthKey("Jan 2026"))
        assertNull(parseGrossMonthKey("2026/05"))
    }

    private fun gross(month: String, totalNominal: String = "100"): GrossData {
        return GrossData(month = month, totalNominal = totalNominal, orderCount = "1", tax = "0")
    }
}
