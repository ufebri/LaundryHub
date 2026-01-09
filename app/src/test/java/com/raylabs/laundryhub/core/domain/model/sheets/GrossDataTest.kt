package com.raylabs.laundryhub.core.domain.model.sheets

import org.junit.Assert.assertEquals
import org.junit.Test

class GrossDataTest {

    @Test
    fun `toGrossData maps fields correctly`() {
        val map = mapOf(
            GROSS_MONTH to "oktober",
            GROSS_TOTAL_NOMINAL to "Rp3.611.300",
            GROSS_ORDER_COUNT to "152",
            GROSS_TAX to "Rp18.057"
        )

        val data = map.toGrossData()

        assertEquals("oktober", data.month)
        assertEquals("Rp3.611.300", data.totalNominal)
        assertEquals("152", data.orderCount)
        assertEquals("Rp18.057", data.tax)
    }
}
