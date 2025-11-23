package com.raylabs.laundryhub.ui.common.dummy.inventory

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.profile.inventory.state.InventoryUiState
import com.raylabs.laundryhub.ui.profile.inventory.state.PackageItem

val dummyInventoryUiState = InventoryUiState(
    packages = SectionState(
        data = listOf(
            PackageItem("Regular", "Rp 5.000 ,-", "3d"),
            PackageItem("Express - 6H", "Rp 10.000 ,-", "6h"),
            PackageItem("Express - 24H", "Rp 8.000 ,-", "1d"),
            PackageItem("Regular Cuci", "Rp 3.000 ,-", "3d"),
            PackageItem("Express - 6H Cuci", "Rp 5.000 ,-", "6h")
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