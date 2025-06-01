package com.raylabs.laundryhub.ui.common.util

object TextUtil {

    fun String.capitalizeFirstLetter(): String {
        return this.lowercase().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }

}