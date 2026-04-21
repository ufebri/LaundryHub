package com.raylabs.laundryhub.core.domain.model.sheets

data class PackageData(
    val price: String,
    val name: String,
    val duration: String,
    val unit: String,
    val sheetRowIndex: Int = -1
)

fun Map<String, String>.toPackageData(sheetRowIndex: Int = -1): PackageData {
    return PackageData(
        price = (this["harga"] ?: "").toString(),
        name = (this["packages"] ?: "").toString(),
        duration = (this["work"] ?: "").toString(),
        unit = (this["unit"] ?: "").toString(),
        sheetRowIndex = sheetRowIndex
    )
}

fun PackageData.toSheetValues(): List<List<String>> {
    return listOf(
        listOf(
            this.price,
            this.name,
            this.duration,
            this.unit
        )
    )
}
