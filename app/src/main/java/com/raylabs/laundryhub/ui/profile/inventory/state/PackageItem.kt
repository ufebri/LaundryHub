package com.raylabs.laundryhub.ui.profile.inventory.state

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData

data class PackageItem(
    val name: String,
    val price: String,
    val work: String,
) {
    val displayPrice: String
        get() = "$price,-"
}

fun List<PackageData>.toUi(): List<PackageItem> {
    return map {
        PackageItem(
            name = it.name,
            price = it.price,
            work = it.duration
        )
    }
}