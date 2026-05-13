package com.raylabs.laundryhub.ui.startup

sealed interface StartupConnectionUiState {
    object Checking : StartupConnectionUiState
    object Ready : StartupConnectionUiState
    object Retrying : StartupConnectionUiState
    data class Maintenance(val message: String?) : StartupConnectionUiState
    object Unavailable : StartupConnectionUiState
}
