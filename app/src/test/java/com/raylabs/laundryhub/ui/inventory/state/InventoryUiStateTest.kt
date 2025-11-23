package com.raylabs.laundryhub.ui.inventory.state

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.profile.inventory.state.InventoryUiState
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryUiStateTest {

    @Test
    fun `default state has empty SectionStates`() {
        val state = InventoryUiState()
        assertNull(state.packages.data)
        assertNull(state.otherPackages.data)
        assertNull(state.packages.errorMessage)
        assertNull(state.otherPackages.errorMessage)
        assertFalse(state.packages.isLoading)
        assertFalse(state.otherPackages.isLoading)
    }

    @Test
    fun `state with loaded packages returns correct data`() {
        val list = listOf(PackageItem("Express", "10000", "6h"))
        val section = SectionState(data = list)
        val state = InventoryUiState(packages = section)
        assertEquals(list, state.packages.data)
        assertNull(state.packages.errorMessage)
    }

    @Test
    fun `state with loaded otherPackages returns correct data`() {
        val otherList = listOf("Paket Khusus", "Promo")
        val section = SectionState(data = otherList)
        val state = InventoryUiState(otherPackages = section)
        assertEquals(otherList, state.otherPackages.data)
        assertNull(state.otherPackages.errorMessage)
    }

    @Test
    fun `state with error and loading set correctly`() {
        val errorSection = SectionState<List<PackageItem>>(errorMessage = "Failed")
        val loadingSection = SectionState<List<String>>(isLoading = true)
        val state = InventoryUiState(packages = errorSection, otherPackages = loadingSection)
        assertEquals("Failed", state.packages.errorMessage)
        assertTrue(state.otherPackages.isLoading)
    }
}