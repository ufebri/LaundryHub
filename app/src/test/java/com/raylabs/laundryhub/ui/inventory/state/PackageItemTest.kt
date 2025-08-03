package com.raylabs.laundryhub.ui.inventory.state

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import org.junit.Assert.assertEquals
import org.junit.Test

class PackageItemTest {

    @Test
    fun `displayPrice returns formatted price`() {
        val item = PackageItem(name = "Express", price = "10000", work = "6h")
        assertEquals("10000,-", item.displayPrice)
    }

    @Test
    fun `toUi maps PackageData to PackageItem correctly`() {
        val data = PackageData(name = "Reguler", price = "5000", duration = "3d", unit = "kg")
        val list = listOf(data).toUi()
        assertEquals(1, list.size)
        val item = list[0]
        assertEquals("Reguler", item.name)
        assertEquals("5000", item.price)
        assertEquals("3d", item.work)
    }

    @Test
    fun `toUi returns empty list when input empty`() {
        val result = emptyList<PackageData>().toUi()
        assertEquals(0, result.size)
    }
}