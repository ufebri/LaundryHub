package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.SpreadsheetData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

class ReadSpreadsheetDataUseCase @Inject constructor(
    private val repository: LaundryRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null
    ): Resource<List<SpreadsheetData>> {
        val result = retry(onRetry = onRetry) { repository.readSummaryTransaction() }
        return result ?: Resource.Error("Failed after 3 attempts.")
    }
}

