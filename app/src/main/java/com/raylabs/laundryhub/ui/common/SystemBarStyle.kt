package com.raylabs.laundryhub.ui.common

import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun ApplyStatusBarStyle(backgroundColor: Color) {
    val isPreview = LocalInspectionMode.current
    if (isPreview) return

    val view = LocalView.current
    val useDarkStatusIcons = backgroundColor.luminance() > 0.5f

    DisposableEffect(view, backgroundColor, useDarkStatusIcons) {
        val restoreStatusBarAppearance = updateStatusBarAppearance(
            window = (view.context as? android.app.Activity)?.window,
            view = view,
            useDarkStatusIcons = useDarkStatusIcons
        )
        onDispose {
            restoreStatusBarAppearance()
        }
    }
}

internal fun updateStatusBarAppearance(
    window: Window?,
    view: View,
    useDarkStatusIcons: Boolean
): () -> Unit {
    if (window == null) return {}

    val controller = WindowInsetsControllerCompat(window, view)
    val previousAppearance = controller.isAppearanceLightStatusBars
    controller.isAppearanceLightStatusBars = useDarkStatusIcons

    return {
        controller.isAppearanceLightStatusBars = previousAppearance
    }
}
