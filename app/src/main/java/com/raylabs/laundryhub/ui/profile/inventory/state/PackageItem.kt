package com.raylabs.laundryhub.ui.profile.inventory.state

import com.raylabs.laundryhub.core.domain.model.sheets.PackageData
import java.text.NumberFormat
import java.util.Locale

data class PackageItem(
    val name: String,
    val price: String,
    val work: String,
    val unit: String = "",
    val sheetRowIndex: Int = -1,
) {
    val displayPrice: String
        get() = formatPackagePrice(price)

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
            unit = it.unit,
            sheetRowIndex = it.sheetRowIndex
        )
    }
}

private fun formatPackagePrice(rawPrice: String): String {
    val trimmed = rawPrice.trim()
    if (trimmed.isBlank()) return ""

    val normalizedDigits = trimmed.filter(Char::isDigit)
    val numericValue = normalizedDigits.toLongOrNull()
    if (numericValue != null) {
        val locale = Locale.Builder()
            .setLanguage("id")
            .setRegion("ID")
            .build()
        val formatted = NumberFormat.getInstance(locale).format(numericValue)
        return "Rp$formatted"
    }

    return trimmed
}
