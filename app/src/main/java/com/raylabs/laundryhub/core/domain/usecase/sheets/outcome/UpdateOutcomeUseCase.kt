package com.raylabs.laundryhub.core.domain.usecase.sheets.outcome

import com.raylabs.laundryhub.core.domain.model.sheets.OutcomeData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry

class UpdateOutcomeUseCase(
    private val repository: LaundryRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        order: OutcomeData
    ): Resource<Boolean> {
        return retry(onRetry = onRetry) {
            repository.updateOutcome(order)
        } ?: UseCaseErrorHandling.handleFailedSubmit
    }
}