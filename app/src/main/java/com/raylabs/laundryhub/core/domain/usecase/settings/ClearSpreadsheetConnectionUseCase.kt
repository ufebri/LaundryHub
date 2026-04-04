package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.repository.SpreadsheetConfigRepository
import javax.inject.Inject

class ClearSpreadsheetConnectionUseCase @Inject constructor(
    private val repository: SpreadsheetConfigRepository
) {
    suspend operator fun invoke() {
        repository.clearSpreadsheetConnection()
    }
}
