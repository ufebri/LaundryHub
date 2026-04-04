package com.raylabs.laundryhub.core.domain.repository

import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetValidationResult
import com.raylabs.laundryhub.ui.common.util.Resource

interface SpreadsheetValidationRepository {
    suspend fun validateSpreadsheet(input: String): Resource<SpreadsheetValidationResult>
}
