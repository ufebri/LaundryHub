package com.raylabs.laundryhub.core.domain.model.sheets

import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryDataTest {

    @Test
    fun `toInventoryData should map all fields correctly when values are present`() {
        val map = mapOf(
            "id" to "001",
            "station_type" to "Dryer",
            "station_name" to "Dryer A",
            "machine_code" to "DR-A-01",
            "is_available" to "TRUE"
        )

        val result = map.toInventoryData()

        assertEquals("001", result.id)
        assertEquals("Dryer", result.stationType)
        assertEquals("Dryer A", result.stationName)
        assertEquals("DR-A-01", result.machineCode)
        assertEquals(true, result.isAvailable)
    }

    @Test
    fun `toInventoryData should return isAvailable false when value is not TRUE`() {
        val map = mapOf(
            "id" to "002",
            "station_type" to "Washer",
            "station_name" to "Washer B",
            "machine_code" to "WS-B-02",
            "is_available" to "false"
        )

        val result = map.toInventoryData()

        assertEquals(false, result.isAvailable)
    }

    @Test
    fun `toInventoryData should handle missing fields with default values`() {
        val map = emptyMap<String, String>()

        val result = map.toInventoryData()

        assertEquals("", result.id)
        assertEquals("", result.stationType)
        assertEquals("", result.stationName)
        assertEquals("", result.machineCode)
        assertEquals(false, result.isAvailable)
    }
}