package com.raylabs.laundryhub.ui.common.dummy.inventory

import com.raylabs.laundryhub.ui.common.util.SectionState
import com.raylabs.laundryhub.ui.inventory.state.InventoryGroupItem
import com.raylabs.laundryhub.ui.inventory.state.InventoryUiState
import com.raylabs.laundryhub.ui.inventory.state.MachineItem
import com.raylabs.laundryhub.ui.inventory.state.PackageItem

val dummyInventoryUiState = InventoryUiState(
    inventory = SectionState(
        data = listOf(
            InventoryGroupItem(
                stationType = "Washing Machine",
                machines = listOf(
                    MachineItem("1", "Washer #1", "W01", isAvailable = true),
                    MachineItem("5", "Washer #2", "W02", isAvailable = false),
                )
            ),
            InventoryGroupItem(
                stationType = "Drying Machine",
                machines = listOf(
                    MachineItem("4", "Dryer #1", "D01", isAvailable = false),
                )
            ),
            InventoryGroupItem(
                stationType = "Ironing Machine",
                machines = listOf(
                    MachineItem("2", "Iron #A", "I01", isAvailable = true),
                )
            ),
            InventoryGroupItem(
                stationType = "Folding",
                machines = listOf(
                    MachineItem("3", "Table #2", "F02", isAvailable = true),
                )
            )
        )
    ),
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