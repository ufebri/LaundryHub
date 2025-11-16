package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

class ReadOutcomeTransactionUseCase @Inject constructor(
    private val repository: GoogleSheetRepository
) {
    suspend operator fun invoke(): Resource<List<OutcomeData>> {
        val result = retry {
            repository.readOutcomeTransactions()
        }
        return result ?: Resource.Error("Failed after 3 attempts.")
    }
}
