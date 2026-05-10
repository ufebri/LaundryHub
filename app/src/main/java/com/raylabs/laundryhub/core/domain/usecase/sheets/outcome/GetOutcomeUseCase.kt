package com.raylabs.laundryhub.core.domain.usecase.sheets.outcome

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

class GetOutcomeUseCase @Inject constructor(
    private val repository: LaundryRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        outcomeID: String
    ): Resource<OutcomeData> {
        val result = retry(onRetry = onRetry) {
            repository.getOutcomeById(outcomeID)
        }
        return result ?: UseCaseErrorHandling.handleFailRetry
    }
}