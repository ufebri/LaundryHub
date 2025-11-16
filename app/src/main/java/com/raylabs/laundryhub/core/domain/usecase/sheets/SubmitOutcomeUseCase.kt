package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

class SubmitOutcomeUseCase @Inject constructor(
    private val repository: GoogleSheetRepository
) {
    suspend operator fun invoke(outcomeData: OutcomeData): Resource<Boolean> {
        val result = retry {
            repository.addOutcome(outcomeData)
        }
        return result ?: Resource.Error("Failed after 3 attempts.")
    }
}
