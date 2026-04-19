package com.raylabs.laundryhub.ui.profile.inventory.state

import com.raylabs.laundryhub.ui.common.util.SectionState

data class InventoryUiState(
    val spreadsheetName: String? = null,
    val spreadsheetId: String? = null,
    val spreadsheetUrl: String? = null,
    val packages: SectionState<List<PackageItem>> = SectionState(),
    val otherPackages: SectionState<List<String>> = SectionState(),
    val savePackage: SectionState<Boolean> = SectionState(),
    val deletePackage: SectionState<Boolean> = SectionState()
)
