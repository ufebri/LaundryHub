package com.raylabs.laundryhub.ui.common.dummy.inventory

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.profile.inventory.state.InventoryUiState
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem

val dummyInventoryUiState = InventoryUiState(
    packages = SectionState(
        data = listOf(
            PackageItem("Regular", "Rp5.000", "3d", "kg"),
            PackageItem("Express - 6H", "Rp10.000", "6h", "kg"),
            PackageItem("Express - 24H", "Rp8.000", "1d", "kg"),
            PackageItem("Regular Cuci", "Rp3.000", "3d", "kg"),
            PackageItem("Express - 6H Cuci", "Rp7.000", "6h", "kg")
        )
    ),
    otherPackages = SectionState(
        data = listOf(
            "Express Kilat",
            "Special Package",
            "Cuci Premium"
        )
    )
)
