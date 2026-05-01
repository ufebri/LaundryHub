package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetValidationResult
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetValidationRepository
import com.raylabs.laundryhub.shared.util.Resource
import javax.inject.Inject

class ValidateSpreadsheetUseCase @Inject constructor(
    private val repository: SpreadsheetValidationRepository
) {
    suspend operator fun invoke(input: String): Resource<SpreadsheetValidationResult> {
        return repository.validateSpreadsheet(input)
    }
}
