package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.repository.GoogleSheetRepository
import com.raylabs.laundryhub.shared.util.Resource
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
            .map(::normalizePackageLabel)

        val packages = (packageResult as? Resource.Success)?.data.orEmpty()
            .map { normalizePackageKey(it.name) }

        val seenPackageKeys = mutableSetOf<String>()
        val otherPackages = remarks
            .filter { it.isNotBlank() }
            .filterNot { normalizePackageKey(it) in packages }
            .filter { seenPackageKeys.add(normalizePackageKey(it)) }

        return if (otherPackages.isEmpty()) {
            Resource.Empty
        } else {
            Resource.Success(otherPackages)
        }
    }
}

private fun normalizePackageLabel(value: String): String =
    value.trim().replace(Regex("\\s+"), " ")

private fun normalizePackageKey(value: String): String =
    normalizePackageLabel(value).lowercase()
