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
    background = Color(0xFF181829),
    surface = Color(0xFFF5F5F5), // abu-abu terang agar bottom sheet tidak putih polos di dark mode
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White, // Teks di atas background utama (gelap) adalah PUTIH
    onSurface = Color.Black    // Teks di atas surface (bottom sheet terang) adalah HITAM
)

private val LightColorPalette = lightColors(
    primary = PurpleLaundryHub,
    primaryVariant = Purple700,
    secondary = PurpleLaundryHub,
    background = Color.White,
    surface = Color.White, 
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black, // Teks di atas background utama (terang) adalah HITAM
    onSurface = Color.Black    // Teks di atas surface (terang) adalah HITAM
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
