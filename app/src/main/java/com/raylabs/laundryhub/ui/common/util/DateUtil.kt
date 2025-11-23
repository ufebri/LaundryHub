package com.raylabs.laundryhub.ui.common.util

import android.os.Build
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtil {

    const val STANDARD_DATE_FORMATED = "dd/MM/yyyy" //ex: 22/10/2025
    const val DISPLAY_DATE_FORMATED = "dd MMMM yyyy" //ex 22 Nov 2025

    // Mendapatkan tanggal hari ini dalam format yang sesuai
    fun getTodayDate(dateFormat: String = "yyyy-MM-dd"): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Cek apakah format mengandung jam/menit
            return if (dateFormat.contains("H") || dateFormat.contains("m") || dateFormat.contains("s")) {
                LocalDateTime.now().format(DateTimeFormatter.ofPattern(dateFormat))
            } else {
                LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat))
            }
        } else {
            val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
            sdf.format(Date())
        }
    }

    fun isToday(date: String, formatedDate: String): Boolean {
        val today = getTodayDate(dateFormat = formatedDate)
        return date == today
    }

    fun parseDate(dateString: String, formatedDate: String = "yyyy-MM-dd"): Date? {
        val dateFormat = SimpleDateFormat(formatedDate, Locale.getDefault())
        return try {
            dateFormat.parse(dateString)
        } catch (_: Exception) {
            null
        }
    }

    fun formatToLongDate(
        dateString: String,
        inputFormat: String = "yyyy-MM-dd",
        outputFormat: String = DISPLAY_DATE_FORMATED
    ): String {
        val date = parseDate(dateString, inputFormat)
        val outputFormats = SimpleDateFormat(outputFormat, Locale.getDefault())
        return date?.let { outputFormats.format(it) } ?: dateString
    }

    /**
     * Hitung due date berdasarkan start date + durasi (contoh: "6h", "3d").
     * Format input: "dd-MM-yyyy HH:mm"
     */
    fun getDueDate(
        duration: String,
        startDate: String = getTodayDate("dd-MM-yyyy") + " 08:00"
    ): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat(STANDARD_DATE_FORMATED, Locale.getDefault())
        return try {
            val start = dateFormat.parse(startDate) ?: return startDate
            val cal = Calendar.getInstance().apply { time = start }

            when {
                duration.endsWith("h") -> {
                    val hours = duration.dropLast(1).toIntOrNull() ?: return startDate
                    cal.add(Calendar.HOUR_OF_DAY, hours)
                }

                duration.endsWith("d") -> {
                    val days = duration.dropLast(1).toIntOrNull() ?: return startDate
                    cal.add(Calendar.DAY_OF_MONTH, days)
                }

                else -> return startDate
            }

            outputFormat.format(cal.time)
        } catch (_: Exception) {
            startDate
        }
    }
}
