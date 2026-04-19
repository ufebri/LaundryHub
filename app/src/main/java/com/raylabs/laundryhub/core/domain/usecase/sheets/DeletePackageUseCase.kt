package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry

class DeletePackageUseCase(
    private val repository: GoogleSheetRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        sheetRowIndex: Int
    ): Resource<Boolean> {
        if (sheetRowIndex < 2) {
            return Resource.Error("Package row not found.")
        }

        return retry(onRetry = onRetry) {
            repository.deletePackage(sheetRowIndex)
        } ?: UseCaseErrorHandling.handleFailedSubmit
    }
}
