package com.raylabs.laundryhub.core.reminder

interface ReminderNotificationScheduler {
    fun scheduleDailySummary(hourOfDay: Int, minute: Int)
    fun cancelDailySummary()
}
