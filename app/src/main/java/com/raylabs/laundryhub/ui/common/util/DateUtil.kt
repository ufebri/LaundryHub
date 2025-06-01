package com.raylabs.laundryhub.ui.common.util

import android.os.Build
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object DateUtil {

    // Format tanggal Google Sheets biasanya "yyyy-MM-dd"
    private const val DATE_FORMAT = "yyyy-MM-dd"

    // Mendapatkan tanggal hari ini dalam format yang sesuai
    fun getTodayDate(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT))
        } else {
            val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            dateFormat.format(Date())
        }
    }

    fun isToday(date: String): Boolean {
        val today = getTodayDate()
        return date == today
    }

    fun parseDate(dateString: String): Date? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}