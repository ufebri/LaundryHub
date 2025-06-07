package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.ui.common.util.Resource
import javax.inject.Inject

class GetOtherPackageUseCase @Inject constructor(
    private val repository: GoogleSheetRepository
) {
    suspend operator fun invoke(): Resource<List<String>> {
        val remarkResult = repository.readOtherPackage()
        val packageResult = repository.readPackageData()

        if (remarkResult is Resource.Error) return Resource.Error(remarkResult.message)
        if (packageResult is Resource.Error) return Resource.Error(packageResult.message)

        val remarks = (remarkResult as? Resource.Success)?.data.orEmpty()
            .map { it.trim() }

        val packages = (packageResult as? Resource.Success)?.data.orEmpty()
            .map { it.name }

        val otherPackages = remarks
            .filter { it.isNotBlank() && it !in packages }
            .distinct()

        return if (otherPackages.isEmpty()) {
            Resource.Empty
        } else {
            Resource.Success(otherPackages)
        }
    }
}