package com.raylabs.laundryhub.ui.profile.inventory.state

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import org.junit.Assert.assertEquals
import org.junit.Test

class PackageItemTest {

    @Test
    fun `properties map correctly`() {
        val item = PackageItem(
            name = "Express",
            price = "15000",
            work = "1 Day",
            unit = "kg",
            sheetRowIndex = 2,
            id = 5
        )

        assertEquals("Express", item.name)
        assertEquals("15000", item.price)
        assertEquals("1 Day", item.work)
        assertEquals("kg", item.unit)
        assertEquals(2, item.sheetRowIndex)
        assertEquals(5, item.id)
    }

    @Test
    fun `displayPrice formats Indonesian currency correctly for numeric prices`() {
        val item1 = PackageItem(name = "Express", price = "15000", work = "1 Day")
        assertEquals("Rp15.000", item1.displayPrice)

        val item2 = PackageItem(name = "Express", price = " 1234567 ", work = "1 Day")
        assertEquals("Rp1.234.567", item2.displayPrice)
    }

    @Test
    fun `displayPrice returns original price for non-numeric or blank values`() {
        val item1 = PackageItem(name = "Express", price = "free", work = "1 Day")
        assertEquals("free", item1.displayPrice)

        val item2 = PackageItem(name = "Express", price = "   ", work = "1 Day")
        assertEquals("", item2.displayPrice)
    }

    @Test
    fun `displayRate appends unit when not blank`() {
        val itemWithUnit = PackageItem(name = "Express", price = "15000", work = "1 Day", unit = "kg")
        assertEquals("Rp15.000/kg", itemWithUnit.displayRate)

        val itemWithoutUnit = PackageItem(name = "Express", price = "15000", work = "1 Day", unit = "")
        assertEquals("Rp15.000", itemWithoutUnit.displayRate)
    }

    @Test
    fun `list toUi maps all fields from PackageData to PackageItem`() {
        val list = listOf(
            PackageData(id = 1, price = "10000", name = "Regular", duration = "2 Days", unit = "kg", sheetRowIndex = 4)
        )

        val uiList = list.toUi()
        assertEquals(1, uiList.size)
        val uiItem = uiList[0]
        assertEquals("Regular", uiItem.name)
        assertEquals("10000", uiItem.price)
        assertEquals("2 Days", uiItem.work)
        assertEquals("kg", uiItem.unit)
        assertEquals(4, uiItem.sheetRowIndex)
        assertEquals(1, uiItem.id)
    }
}
