package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.HistoryData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry
import javax.inject.Inject

class SubmitHistoryUseCase @Inject constructor(
    private val repository: GoogleSheetRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        history: HistoryData
    ): Resource<Boolean> {
        return retry(onRetry = onRetry) {
            repository.addHistoryOrder(history)
        } ?: Resource.Error("Failed to submit data")
    }
}