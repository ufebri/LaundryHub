package com.raylabs.laundryhub.core.domain.model.sheets

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PackageDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testInitializationAndProperties() {
        val data = PackageData(
            id = 2,
            price = "15000",
            name = "Express",
            duration = "1 day",
            unit = "kg",
            sheetRowIndex = 5
        )

        assertEquals(2, data.id)
        assertEquals("15000", data.price)
        assertEquals("Express", data.name)
        assertEquals("1 day", data.duration)
        assertEquals("kg", data.unit)
        assertEquals(5, data.sheetRowIndex)
    }

    @Test
    fun testSerialization() {
        val data = PackageData(id = 1, price = "5000", name = "Regular", duration = "3 days", unit = "kg", sheetRowIndex = 2)
        val serialized = json.encodeToString(data)
        val deserialized = json.decodeFromString<PackageData>(serialized)
        assertEquals(data, deserialized)
    }

    @Test
    fun testToPackageData() {
        val map = mapOf(
            "harga" to "10000",
            "packages" to "Premium",
            "work" to "2 days",
            "unit" to "pcs"
        )
        val result = map.toPackageData(sheetRowIndex = 12)

        assertEquals("10000", result.price)
        assertEquals("Premium", result.name)
        assertEquals("2 days", result.duration)
        assertEquals("pcs", result.unit)
        assertEquals(12, result.sheetRowIndex)
        assertEquals(0, result.id)
    }

    @Test
    fun testToSheetValues() {
        val data = PackageData(id = 1, price = "5000", name = "Regular", duration = "3 days", unit = "kg")
        val sheetValues = data.toSheetValues()

        assertEquals(1, sheetValues.size)
        assertEquals("5000", sheetValues[0][0])
        assertEquals("Regular", sheetValues[0][1])
        assertEquals("3 days", sheetValues[0][2])
        assertEquals("kg", sheetValues[0][3])
    }
}
