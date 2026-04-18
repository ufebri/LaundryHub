package com.raylabs.laundryhub.ui.profile.inventory.state

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData

data class PackageItem(
    val name: String,
    val price: String,
    val work: String,
    val unit: String = "",
) {
    val displayPrice: String
        get() = "$price,-"

    val displayRate: String
        get() = buildString {
            append(displayPrice)
            if (unit.isNotBlank()) {
                append("/")
                append(unit)
            }
        }
}

fun List<PackageData>.toUi(): List<PackageItem> {
    return map {
        PackageItem(
            name = it.name,
            price = it.price,
            work = it.duration,
            unit = it.unit
        )
    }
}
