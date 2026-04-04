package com.raylabs.laundryhub.ui.spreadsheet.state

import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig

data class SpreadsheetSetupUiState(
    val input: String = "",
    val configuredSpreadsheetId: String? = null,
    val configuredSpreadsheetName: String? = null,
    val configuredSpreadsheetUrl: String? = null,
    val configuredValidationVersion: Int = 0,
    val hasLoadedConfiguration: Boolean = false,
    val isRestoring: Boolean = false,
    val isValidating: Boolean = false,
    val isReady: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showRequestAccess: Boolean = false
) {
    val isBusy: Boolean
        get() = isRestoring || isValidating

    val hasCurrentConfiguredSpreadsheetValidation: Boolean
        get() = configuredValidationVersion >= SpreadsheetConfig.CURRENT_VALIDATION_VERSION
}
