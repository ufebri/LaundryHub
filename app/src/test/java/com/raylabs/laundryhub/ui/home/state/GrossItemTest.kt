package com.raylabs.laundryhub.ui.home.state

import androidx.compose.ui.graphics.Color
import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import org.junit.Assert.assertEquals
import org.junit.Test

class GrossItemTest {

    @Test
    fun `toUi maps GrossData correctly`() {
        val data = GrossData("juni", "Rp1.672.800", "68", "Rp8.364")
        val ui = data.toUi()

        assertEquals("juni", ui.month)
        assertEquals("Rp1.672.800", ui.totalNominal)
        assertEquals("68", ui.orderCount)
        assertEquals("Rp8.364", ui.tax)
    }

    @Test
    fun `toSummaryItem maps gross item correctly`() {
        val item = GrossItem("juli", "Rp1.928.000", "81", "Rp9.640")
        val summary = item.toSummaryItem()

        assertEquals("Gross Income", summary.title)
        assertEquals("Rp1.928.000", summary.body)
        assertEquals("81 order", summary.footer)
        assertEquals(Color(0xFFE6F4EA), summary.backgroundColor)
        assertEquals(Color.Black, summary.textColor)
        assertEquals(true, summary.isInteractive)
    }
}
