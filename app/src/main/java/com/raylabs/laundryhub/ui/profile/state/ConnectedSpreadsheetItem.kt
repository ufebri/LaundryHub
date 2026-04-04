package com.raylabs.laundryhub.ui.profile.state

import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig

data class ConnectedSpreadsheetItem(
    val spreadsheetId: String,
    val spreadsheetName: String,
    val spreadsheetUrl: String?
) {
    val shortSpreadsheetId: String
        get() = if (spreadsheetId.length <= 16) {
            spreadsheetId
        } else {
            "${spreadsheetId.take(8)}...${spreadsheetId.takeLast(6)}"
        }
}

fun SpreadsheetConfig.toUi(): ConnectedSpreadsheetItem? {
    val currentSpreadsheetId = spreadsheetId ?: return null
    return ConnectedSpreadsheetItem(
        spreadsheetId = currentSpreadsheetId,
        spreadsheetName = spreadsheetName ?: "LaundryHub Spreadsheet",
        spreadsheetUrl = spreadsheetUrl
    )
}
