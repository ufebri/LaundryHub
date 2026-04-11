package com.raylabs.laundryhub.ui.common

import android.os.Build
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class SystemBarStyleTest {

    @Test
    fun `updateStatusBarAppearance is a no-op when no activity window is available`() {
        val view = View(ApplicationProvider.getApplicationContext())

        val restore = updateStatusBarAppearance(
            window = null,
            view = view,
            useDarkStatusIcons = true
        )

        restore()
    }

    @Test
    fun `updateStatusBarAppearance applies and restores status bar icon contrast`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = false

        val restore = updateStatusBarAppearance(
            window = activity.window,
            view = activity.window.decorView,
            useDarkStatusIcons = true
        )

        assertTrue(controller.isAppearanceLightStatusBars)

        restore()

        assertFalse(controller.isAppearanceLightStatusBars)
    }

    @Test
    fun `ApplyStatusBarStyle updates and restores status bar style through composition`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = false

        activity.setContent {
            ApplyStatusBarStyle(backgroundColor = Color.White)
        }
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(controller.isAppearanceLightStatusBars)

        activity.setContent {}
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(controller.isAppearanceLightStatusBars)
    }
}
