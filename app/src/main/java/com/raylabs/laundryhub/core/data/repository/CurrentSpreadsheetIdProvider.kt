package com.raylabs.laundryhub.core.data.repository

import com.raylabs.laundryhub.core.domain.repository.SpreadsheetConfigRepository
import com.raylabs.laundryhub.core.domain.repository.SpreadsheetIdProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CurrentSpreadsheetIdProvider @Inject constructor(
    private val repository: SpreadsheetConfigRepository
) : SpreadsheetIdProvider {
    override suspend fun getSpreadsheetId(): String? = repository.spreadsheetConfig.first().spreadsheetId
}
