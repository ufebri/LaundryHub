package com.raylabs.laundryhub.ui.common.util

import android.os.Build
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object DateUtil {

    // Mendapatkan tanggal hari ini dalam format yang sesuai
    fun getTodayDate(dateFormat: String = "yyyy-MM-dd"): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat))
        } else {
            val dateFormat = SimpleDateFormat(dateFormat, Locale.getDefault())
            dateFormat.format(Date())
        }
    }

    fun isToday(date: String): Boolean {
        val today = getTodayDate()
        return date == today
    }

    fun parseDate(dateString: String, formatedDate: String = "yyyy-MM-dd"): Date? {
        val dateFormat = SimpleDateFormat(formatedDate, Locale.getDefault())
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}