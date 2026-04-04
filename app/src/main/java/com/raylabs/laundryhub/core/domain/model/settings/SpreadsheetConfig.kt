package com.raylabs.laundryhub.core.domain.model.settings

data class SpreadsheetConfig(
    val spreadsheetId: String? = null,
    val spreadsheetName: String? = null,
    val spreadsheetUrl: String? = null,
    val validationVersion: Int = 0
) {
    val hasConfiguredSpreadsheet: Boolean
        get() = !spreadsheetId.isNullOrBlank()

    val hasCurrentValidation: Boolean
        get() = validationVersion >= CURRENT_VALIDATION_VERSION

    companion object {
        const val CURRENT_VALIDATION_VERSION = 2
    }
}
