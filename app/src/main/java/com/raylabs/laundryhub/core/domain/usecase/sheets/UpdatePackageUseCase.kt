package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.core.domain.usecase.UseCaseErrorHandling
import com.raylabs.laundryhub.ui.common.util.Resource
import com.raylabs.laundryhub.ui.common.util.retry

class UpdatePackageUseCase(
    private val repository: GoogleSheetRepository
) {
    suspend operator fun invoke(
        onRetry: ((Int) -> Unit)? = null,
        packageData: PackageData
    ): Resource<Boolean> {
        if (packageData.sheetRowIndex < 2) {
            return Resource.Error("Package row not found.")
        }

        validatePackageForSave(packageData)?.let { return it }

        when (val existingPackages = repository.readPackageData()) {
            is Resource.Success -> {
                if (hasDuplicatePackageName(
                        packageName = packageData.name,
                        existingPackages = existingPackages.data,
                        excludingRowIndex = packageData.sheetRowIndex
                    )
                ) {
                    return Resource.Error("Package name already exists in the master list.")
                }
            }

            is Resource.Error -> return existingPackages
            else -> Unit
        }

        return retry(onRetry = onRetry) {
            repository.updatePackage(packageData)
        } ?: UseCaseErrorHandling.handleFailedSubmit
    }
}
