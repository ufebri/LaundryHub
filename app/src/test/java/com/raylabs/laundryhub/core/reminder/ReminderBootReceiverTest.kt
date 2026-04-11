package com.raylabs.laundryhub.core.reminder

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.raylabs.laundryhub.core.domain.model.reminder.ReminderSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class ReminderBootReceiverTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    @Test
    fun `shouldHandleReminderBootBroadcast only accepts boot completed`() {
        assertTrue(shouldHandleReminderBootBroadcast(Intent.ACTION_BOOT_COMPLETED))
        assertFalse(shouldHandleReminderBootBroadcast(Intent.ACTION_TIMEZONE_CHANGED))
        assertFalse(shouldHandleReminderBootBroadcast(null))
    }

    @Test
    fun `onReceive ignores unrelated broadcasts`() {
        ReminderBootReceiver().onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))

        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun `applyReminderBootSchedule schedules daily summary when reminder and daily notifications are enabled`() {
        val scheduler: ReminderNotificationScheduler = mock()

        applyReminderBootSchedule(
            settings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = true,
                notificationHour = 7,
                notificationMinute = 45
            ),
            reminderNotificationScheduler = scheduler
        )

        verify(scheduler).scheduleDailySummary(7, 45)
    }

    @Test
    fun `applyReminderBootSchedule cancels daily summary when reminder notifications are not fully enabled`() {
        val scheduler: ReminderNotificationScheduler = mock()

        applyReminderBootSchedule(
            settings = ReminderSettings(
                isReminderEnabled = true,
                isDailyNotificationEnabled = false
            ),
            reminderNotificationScheduler = scheduler
        )

        verify(scheduler).cancelDailySummary()
    }

    @Test
    fun `onReceive schedules daily summary for boot completed broadcasts`() {
        ReminderBootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }
}
