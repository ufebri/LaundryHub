package com.raylabs.laundryhub.ui.common

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
        val window = (view.context as? android.app.Activity)?.window
        if (window == null) return@DisposableEffect onDispose {}

        val controller = WindowInsetsControllerCompat(window, view)
        val previousAppearance = controller.isAppearanceLightStatusBars

        controller.isAppearanceLightStatusBars = useDarkStatusIcons

        onDispose {
            controller.isAppearanceLightStatusBars = previousAppearance
        }
    }
}
