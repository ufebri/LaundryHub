package com.raylabs.laundryhub.ui.profile.state

import com.raylabs.laundryhub.ui.common.util.SectionState

data class ProfileUiState(
    val user: SectionState<UserItem> = SectionState(),
    val logout: SectionState<Boolean> = SectionState(),
    val showWhatsAppOption: Boolean = true,
    val cacheSize: SectionState<Long> = SectionState(isLoading = true),
    val connectedSpreadsheet: SectionState<ConnectedSpreadsheetItem?> = SectionState(isLoading = true),
    val spreadsheetValidation: SectionState<String> = SectionState(),
    val clearCache: SectionState<Boolean> = SectionState(),
    val showClearCacheDialog: Boolean = false,
    val showChangeSpreadsheetDialog: Boolean = false
)
