package com.raylabs.laundryhub.core.domain.model.settings

data class SpreadsheetValidationResult(
    val spreadsheetId: String,
    val spreadsheetTitle: String
) {
    val spreadsheetUrl: String
        get() = "https://docs.google.com/spreadsheets/d/$spreadsheetId/edit"
}
