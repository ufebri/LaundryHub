package com.raylabs.laundryhub.shared.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual object PlatformDate {
    actual val STANDARD_DATE_FORMAT: String = "dd/MM/yyyy"
    actual val DISPLAY_DATE_FORMAT: String = "dd MMMM yyyy"

    actual fun getTodayDate(dateFormat: String): String {
        return SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date())
    }

    actual fun getDueDate(duration: String, startDate: String): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat(STANDARD_DATE_FORMAT, Locale.getDefault())
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

    actual fun isToday(date: String, formattedDate: String): Boolean {
        val today = getTodayDate(formattedDate)
        return date == today
    }

    actual fun parseDate(date: String, format: String): Long? {
        return try {
            val parser = SimpleDateFormat(format, Locale.getDefault())
            parser.isLenient = false
            parser.parse(date.trim())?.time
        } catch (_: Exception) {
            null
        }
    }

    actual fun formatToLongDate(dateString: String, inputFormat: String, outputFormat: String): String {
        val dateMillis = parseDate(dateString, inputFormat)
        return if (dateMillis != null) {
            SimpleDateFormat(outputFormat, Locale.getDefault()).format(Date(dateMillis))
        } else {
            dateString
        }
    }
}
