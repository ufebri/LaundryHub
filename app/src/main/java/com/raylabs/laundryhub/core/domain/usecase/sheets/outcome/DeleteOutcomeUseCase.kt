package com.raylabs.laundryhub.core.domain.usecase.sheets.outcome

import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry

class DeleteOutcomeUseCase(
    private val repository: GoogleSheetRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        outcomeId: String
    ): Resource<Boolean> {
        return retry(onRetry = onRetry) {
            repository.deleteOutcome(outcomeId)
        } ?: UseCaseErrorHandling.handleFailedSubmit
    }
}
