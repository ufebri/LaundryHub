package com.raylabs.laundryhub.ui.profile.inventory.state

import com.raylabs.laundryhub.ui.common.util.SectionState

data class InventoryUiState(
    val packages: SectionState<List<PackageItem>> = SectionState(),
    val otherPackages: SectionState<List<String>> = SectionState()
)