package com.raylabs.laundryhub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = PurpleLaundryHub,
    primaryVariant = Purple700,
    secondary = PurpleLaundryHub,
    background = DarkBackgroundLaundryHub,
    surface = DarkSurfaceLaundryHub,
    error = RedLaundryHub,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkOnSurfaceLaundryHub,
    onSurface = DarkOnSurfaceLaundryHub,
    onError = Color.White
)

private val LightColorPalette = lightColors(
    primary = PurpleLaundryHub,
    primaryVariant = Purple700,
    secondary = PurpleLaundryHub,
    background = LightBackgroundLaundryHub,
    surface = LightSurfaceLaundryHub,
    error = RedLaundryHub,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightOnSurfaceLaundryHub,
    onSurface = LightOnSurfaceLaundryHub,
    onError = Color.White
)

@Composable
fun LaundryHubTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
