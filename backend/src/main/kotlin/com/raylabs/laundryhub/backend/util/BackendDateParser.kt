package com.raylabs.laundryhub.backend.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun parseSupportedLaundryDate(value: String?): Date? {
    val sanitized = value?.trim().orEmpty()
    if (sanitized.isBlank()) return null

    val formats = listOf(
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "yyyy-MM-dd",
        "dd/MM/yyyy HH:mm",
        "dd-MM-yyyy HH:mm",
        "yyyy-MM-dd HH:mm",
        "dd MMM yyyy",
        "dd MMMM yyyy",
        "dd MMM yyyy HH:mm",
        "dd MMMM yyyy HH:mm"
    )
    val locales = listOf(Locale.getDefault(), Locale.ENGLISH, Locale.forLanguageTag("id-ID")).distinct()

    return formats.firstNotNullOfOrNull { pattern ->
        locales.firstNotNullOfOrNull { locale ->
            runCatching {
                SimpleDateFormat(pattern, locale).apply {
                    isLenient = false
                }.parse(sanitized)
            }.getOrNull()
        }
    }
}
