package com.raylabs.laundryhub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme as Material2Theme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple200,
    onPrimary = Color.White,
    secondary = Teal200,
    onSecondary = Color.Black,
    background = Color(0xFF181829),
    onBackground = Color.White,
    surface = Color(0xFF1F1F2E),
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Purple500,
    onPrimary = Color.White,
    secondary = Teal200,
    onSecondary = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val DarkColorPalette = darkColors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200,
    background = Color(0xFF181829),
    surface = Color(0xFF1F1F2E),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun LaundryHubTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val material3Scheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val material2Colors = if (darkTheme) DarkColorPalette else LightColorPalette

    MaterialTheme(colorScheme = material3Scheme) {
        Material2Theme(
            colors = material2Colors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
