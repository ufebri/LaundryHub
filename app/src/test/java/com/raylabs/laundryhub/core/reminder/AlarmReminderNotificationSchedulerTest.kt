package com.raylabs.laundryhub.core.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AlarmReminderNotificationSchedulerTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `calculate next trigger keeps same day when target time is still ahead`() {
        val now = calendarUtc(
            year = 2026,
            month = Calendar.APRIL,
            day = 11,
            hour = 8,
            minute = 10
        ).timeInMillis

        val actual = calculateNextReminderTriggerAtMillis(
            nowMillis = now,
            hourOfDay = 9,
            minute = 30,
            timeZone = utc
        )

        val expected = calendarUtc(
            year = 2026,
            month = Calendar.APRIL,
            day = 11,
            hour = 9,
            minute = 30
        ).timeInMillis

        assertEquals(expected, actual)
    }

    @Test
    fun `calculate next trigger moves to next day when target time already passed`() {
        val now = calendarUtc(
            year = 2026,
            month = Calendar.APRIL,
            day = 11,
            hour = 10,
            minute = 45
        ).timeInMillis

        val actual = calculateNextReminderTriggerAtMillis(
            nowMillis = now,
            hourOfDay = 9,
            minute = 30,
            timeZone = utc
        )

        val expected = calendarUtc(
            year = 2026,
            month = Calendar.APRIL,
            day = 12,
            hour = 9,
            minute = 30
        ).timeInMillis

        assertEquals(expected, actual)
    }

    @Test
    fun `calculate next trigger clamps invalid time values`() {
        val now = calendarUtc(
            year = 2026,
            month = Calendar.APRIL,
            day = 11,
            hour = 10,
            minute = 45
        ).timeInMillis

        val actual = calculateNextReminderTriggerAtMillis(
            nowMillis = now,
            hourOfDay = 25,
            minute = -7,
            timeZone = utc
        )

        val expected = calendarUtc(
            year = 2026,
            month = Calendar.APRIL,
            day = 11,
            hour = 23,
            minute = 0
        ).timeInMillis

        assertEquals(expected, actual)
    }

    private fun calendarUtc(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Calendar {
        return Calendar.getInstance(utc).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
