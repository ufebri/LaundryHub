package com.raylabs.laundryhub.core.domain.usecase.sheets

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import com.raylabs.laundryhub.shared.util.Resource

fun validatePackageForSave(packageData: PackageData): Resource.Error? {
    return when {
        packageData.name.isBlank() -> Resource.Error("Package name is required.")
        packageData.price.isBlank() -> Resource.Error("Package price is required.")
        packageData.duration.isBlank() -> Resource.Error("Package duration is required.")
        packageData.unit.isBlank() -> Resource.Error("Package unit is required.")
        else -> null
    }
}

fun hasDuplicatePackageName(
    packageName: String,
    existingPackages: List<PackageData>,
    excludingRowIndex: Int? = null
): Boolean {
    val normalizedTarget = normalizePackageName(packageName)
    return existingPackages.any { item ->
        item.sheetRowIndex != excludingRowIndex &&
            normalizePackageName(item.name) == normalizedTarget
    }
}

private fun normalizePackageName(value: String): String {
    return value.trim()
        .replace(Regex("\\s+"), " ")
        .lowercase()
}
