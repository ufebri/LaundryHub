package com.raylabs.laundryhub.ui.inventory.state

import com.raylabs.laundryhub.ui.common.util.SectionState

data class InventoryUiState(
    val inventory: SectionState<List<InventoryGroupItem>> = SectionState(),
    val packages: SectionState<List<PackageItem>> = SectionState(),
    val otherPackages: SectionState<List<String>> = SectionState()
)