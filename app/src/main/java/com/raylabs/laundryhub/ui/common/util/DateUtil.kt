package com.raylabs.laundryhub.ui.common.util

import com.raylabs.laundryhub.shared.util.PlatformDate
import java.util.Date

object DateUtil {

    val STANDARD_DATE_FORMATED = PlatformDate.STANDARD_DATE_FORMAT
    val DISPLAY_DATE_FORMATED = PlatformDate.DISPLAY_DATE_FORMAT

    fun getTodayDate(dateFormat: String = "yyyy-MM-dd"): String {
        return PlatformDate.getTodayDate(dateFormat)
    }

    fun isToday(date: String, formatedDate: String): Boolean {
        return PlatformDate.isToday(date, formatedDate)
    }

    fun parseDate(dateString: String, formatedDate: String = "yyyy-MM-dd"): Date? {
        return PlatformDate.parseDate(dateString, formatedDate)?.let { Date(it) }
    }

    fun parseSupportedAppDate(dateString: String?): Date? {
        val sanitized = dateString?.trim().orEmpty()
        if (sanitized.isEmpty()) return null

        val supportedFormats = listOf(
            STANDARD_DATE_FORMATED,
            "dd-MM-yyyy",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm",
            "dd-MM-yyyy HH:mm",
            "yyyy-MM-dd HH:mm"
        )

        supportedFormats.forEach { pattern ->
            PlatformDate.parseDate(sanitized, pattern)?.let { return Date(it) }
        }

        return null
    }

    fun formatDate(date: Date, outputFormat: String = STANDARD_DATE_FORMATED): String {
        return java.text.SimpleDateFormat(outputFormat, java.util.Locale.getDefault()).format(date)
    }

    fun formatToLongDate(
        dateString: String,
        inputFormat: String = "yyyy-MM-dd",
        outputFormat: String = DISPLAY_DATE_FORMATED
    ): String {
        return PlatformDate.formatToLongDate(dateString, inputFormat, outputFormat)
    }

    fun getDueDate(
        duration: String,
        startDate: String = getTodayDate("dd-MM-yyyy") + " 08:00"
    ): String {
        return PlatformDate.getDueDate(duration, startDate)
    }
}
