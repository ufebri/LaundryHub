package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.repository.SpreadsheetConfigRepository
import javax.inject.Inject

class SaveSpreadsheetConnectionUseCase @Inject constructor(
    private val repository: SpreadsheetConfigRepository
) {
    suspend operator fun invoke(
        spreadsheetId: String,
        spreadsheetName: String,
        spreadsheetUrl: String?
    ) {
        repository.saveSpreadsheetConnection(
            spreadsheetId = spreadsheetId,
            spreadsheetName = spreadsheetName,
            spreadsheetUrl = spreadsheetUrl
        )
    }
}
