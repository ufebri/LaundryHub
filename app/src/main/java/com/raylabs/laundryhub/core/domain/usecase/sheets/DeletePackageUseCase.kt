package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry

class DeletePackageUseCase(
    private val repository: LaundryRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        packageName: String
    ): Resource<Boolean> {
        if (packageName.isBlank()) {
            return Resource.Error("Package name is required.")
        }

        return retry(onRetry = onRetry) {
            repository.deletePackage(packageName)
        } ?: UseCaseErrorHandling.handleFailedSubmit
    }
}
