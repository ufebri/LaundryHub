package com.raylabs.laundryhub.ui.common.util

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
        val number = this.toLongOrNull() ?: return ""
        val formatter = NumberFormat.getInstance(Locale("id", "ID"))
        return "${formatter.format(number)}"
    }

    fun String.removeRupiahFormat(): String {
        return this.replace("Rp", "")
    }

    fun String.removeRupiahFormatWithComma(): String {
        return this.replace("Rp", "").replace(".", "").replace(",", "")
    }
}