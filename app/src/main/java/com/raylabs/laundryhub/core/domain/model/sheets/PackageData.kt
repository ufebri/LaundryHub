package com.raylabs.laundryhub.core.domain.model.sheets

data class PackageData(
    val price: String,
    val name: String,
    val duration: String,
    val unit: String
)

fun Map<String, String>.toPackageData(): PackageData {
    return PackageData(
        price = (this["harga"] ?: "").toString(),
        name = (this["packages"] ?: "").toString(),
        duration = (this["work"] ?: "").toString(),
        unit = (this["unit"] ?: "").toString()
    )
}