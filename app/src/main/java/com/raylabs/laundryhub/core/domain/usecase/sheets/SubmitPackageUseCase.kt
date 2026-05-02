package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.repository.LaundryRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.shared.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry

class SubmitPackageUseCase(
    private val repository: LaundryRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        packageData: PackageData
    ): Resource<Boolean> {
        validatePackageForSave(packageData)?.let { return it }

        when (val existingPackages = repository.readPackageData()) {
            is Resource.Success -> {
                if (hasDuplicatePackageName(packageData.name, existingPackages.data)) {
                    return Resource.Error("Package name already exists in the master list.")
                }
            }

            is Resource.Error -> return existingPackages
            else -> Unit
        }

        return retry(onRetry = onRetry) {
            repository.addPackage(packageData)
        } ?: UseCaseErrorHandling.handleFailedSubmit
    }
}
