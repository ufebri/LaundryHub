package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import javax.inject.Inject

class CreateSpreadsheetDataUseCase @Inject constructor(
    private val repository: GoogleSheetRepository
) {
    operator fun invoke(spreadsheetId: String, range: String, data: SpreadsheetData) {
        repository.createData(spreadsheetId, range, data)
    }
}
