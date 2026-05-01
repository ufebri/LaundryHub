package com.raylabs.laundryhub.shared.util

expect object PlatformDate {
    val STANDARD_DATE_FORMAT: String
    val DISPLAY_DATE_FORMAT: String
    fun getTodayDate(dateFormat: String = STANDARD_DATE_FORMAT): String
    fun getDueDate(duration: String, startDate: String): String
    fun isToday(date: String, formattedDate: String): Boolean
    fun parseDate(date: String, format: String = STANDARD_DATE_FORMAT): Long?
    fun formatToLongDate(dateString: String, inputFormat: String = "yyyy-MM-dd", outputFormat: String = DISPLAY_DATE_FORMAT): String
}
