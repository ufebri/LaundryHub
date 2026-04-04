package com.raylabs.laundryhub.core.domain.repository

import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig
import kotlinx.coroutines.flow.Flow

interface SpreadsheetConfigRepository {
    val spreadsheetConfig: Flow<SpreadsheetConfig>

    suspend fun saveSpreadsheetConnection(
        spreadsheetId: String,
        spreadsheetName: String,
        spreadsheetUrl: String?
    )

    suspend fun clearSpreadsheetConnection()
}
