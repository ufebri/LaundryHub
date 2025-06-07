package com.raylabs.laundryhub.ui.inventory.state

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData

data class PackageItem(
    val name: String,
    val displayPrice: String
)

fun List<PackageData>.toUi(): List<PackageItem> {
    return map {
        val price = it.price.trim()
        val finalPrice = if (price.endsWith(",-")) price else "$price ,-"
        PackageItem(
            name = it.name,
            displayPrice = finalPrice
        )
    }
}