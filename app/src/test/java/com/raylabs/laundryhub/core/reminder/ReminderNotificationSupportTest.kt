package com.raylabs.laundryhub.core.reminder

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ReminderNotificationSupportTest {

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `hasReminderNotificationPermission follows runtime permission on android 13 and above`() {
        val application = ApplicationProvider.getApplicationContext<Application>()

        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(application.hasReminderNotificationPermission())

        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(application.hasReminderNotificationPermission())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S_V2])
    fun `hasReminderNotificationPermission always returns true below android 13`() {
        val application = ApplicationProvider.getApplicationContext<Application>()

        assertTrue(application.hasReminderNotificationPermission())
    }
}
