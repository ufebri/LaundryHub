package com.raylabs.laundryhub.ui.common.util

import com.raylabs.laundryhub.ui.home.state.SortOption
import java.text.NumberFormat
import java.util.Locale

object TextUtil {

    fun String.capitalizeFirstLetter(): String {
        return this.lowercase().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }

    fun String.toRupiahFormat(): String {
        if (this.isEmpty()) return ""
        val number = this.filter { it.isDigit() }.toLongOrNull() ?: return this
        val locale = Locale.Builder()
            .setLanguage("id")
            .setRegion("ID")
            .build()
        val formatter = NumberFormat.getInstance(locale)
        return "Rp ${formatter.format(number)}"
    }

    fun String.removeRupiahFormat(): String {
        return this.replace("Rp ", "").replace("Rp", "")
    }

    fun String.removeRupiahFormatWithComma(): String {
        return this.replace("Rp ", "").replace("Rp", "").replace(".", "").replace(",", "")
    }

    fun SortOption.getDisplayName(): String {
        return this.name
            .replace("_", " ")
            .lowercase()
            .capitalizeFirstLetter() // Menggunakan fungsi ekstensi yang sudah ada
    }
}
