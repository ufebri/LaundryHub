package com.raylabs.laundryhub.core.domain.usecase.settings

import com.raylabs.laundryhub.core.domain.model.settings.SpreadsheetConfig
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSpreadsheetConfigUseCase @Inject constructor(
    private val repository: SpreadsheetConfigRepository
) {
    operator fun invoke(): Flow<SpreadsheetConfig> = repository.spreadsheetConfig
}
