package com.raylabs.laundryhub.core.domain.model.sheets

import org.junit.Assert.assertEquals
import org.junit.Test

class PackageDataExtTest {

    @Test
    fun `toPackageData returns correct values when all keys are present`() {
        val map = mapOf(
            "harga" to "5000",
            "packages" to "Reguler",
            "work" to "3d",
            "unit" to "kg"
        )

        val result = map.toPackageData()

        assertEquals("5000", result.price)
        assertEquals("Reguler", result.name)
        assertEquals("3d", result.duration)
        assertEquals("kg", result.unit)
    }

    @Test
    fun `toPackageData handles missing keys with empty strings`() {
        val map = mapOf(
            "harga" to "7000",
            "packages" to "Express"
            // work and unit missing
        )

        val result = map.toPackageData()

        assertEquals("7000", result.price)
        assertEquals("Express", result.name)
        assertEquals("", result.duration)
        assertEquals("", result.unit)
    }

    @Test
    fun `toPackageData handles completely empty map`() {
        val map = emptyMap<String, String>()
        val result = map.toPackageData()

        assertEquals("", result.price)
        assertEquals("", result.name)
        assertEquals("", result.duration)
        assertEquals("", result.unit)
    }
}