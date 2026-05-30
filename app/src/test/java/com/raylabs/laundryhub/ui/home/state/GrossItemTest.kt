package com.raylabs.laundryhub.ui.home.state

import com.raylabs.laundryhub.core.domain.model.sheets.GrossData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GrossItemTest {

    @Test
    fun `stableGrossDetailKey stays unique when backend ids are duplicated`() {
        val first = duplicateGross.stableGrossDetailKey(index = 0)
        val second = duplicateGross.stableGrossDetailKey(index = 1)

        assertNotEquals(first, second)
    }

    @Test
    fun `toSummaryItem formats numeric order count once`() {
        val item = duplicateGross.toUi().toSummaryItem()

        assertEquals("4 order", item.footer)
    }

    @Test
    fun `toSummaryItem does not duplicate existing order suffix`() {
        val item = duplicateGross.copy(orderCount = "4 order").toUi().toSummaryItem()

        assertEquals("4 order", item.footer)
    }

    private val duplicateGross = GrossData(
        id = 0,
        month = "may",
        totalNominal = "100000",
        orderCount = "4",
        tax = "1000"
    )
}
