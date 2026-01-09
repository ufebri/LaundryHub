package com.raylabs.laundryhub.ui.profile.state

import com.raylabs.laundryhub.ui.common.util.SectionState

data class ProfileUiState(
    val user: SectionState<UserItem> = SectionState(),
    val logout: SectionState<Boolean> = SectionState(),
    val showWhatsAppOption: Boolean = true
)
