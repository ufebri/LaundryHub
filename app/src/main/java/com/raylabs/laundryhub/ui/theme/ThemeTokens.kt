package com.raylabs.laundryhub.ui.theme

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color

val Colors.appChromeTopTint: Color
    get() = if (isLight) {
        Color(0xFFF1ECFF)
    } else {
        surface.copy(alpha = 0.96f)
    }

val Colors.appScreenBackground: Color
    get() = if (isLight) {
        Color(0xFFFBFAFE)
    } else {
        background
    }

val Colors.appScreenGradientBottom: Color
    get() = if (isLight) {
        Color.White
    } else {
        background
    }

val Colors.appCardSurface: Color
    get() = if (isLight) {
        Color.White
    } else {
        surface
    }

val Colors.appBorderSoft: Color
    get() = if (isLight) {
        primary.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }

val Colors.appPanelTranslucent: Color
    get() = if (isLight) {
        Color.White.copy(alpha = 0.74f)
    } else {
        Color.White.copy(alpha = 0.06f)
    }

val Colors.appPanelElevated: Color
    get() = if (isLight) {
        Color.White.copy(alpha = 0.92f)
    } else {
        surface.copy(alpha = 0.96f)
    }

val Colors.appAccentContainer: Color
    get() = primary.copy(alpha = if (isLight) 0.10f else 0.22f)

val Colors.appSuccessContainer: Color
    get() = if (isLight) {
        Color(0xFFE8F5E9)
    } else {
        Color(0xFF173126)
    }

val Colors.appSuccessContent: Color
    get() = if (isLight) {
        Color(0xFF2E7D32)
    } else {
        Color(0xFF94E7AA)
    }

val Colors.appMutedContainer: Color
    get() = if (isLight) {
        Color(0xFFF2F2F6)
    } else {
        Color.White.copy(alpha = 0.08f)
    }

val Colors.appMutedContent: Color
    get() = if (isLight) {
        Color(0xFF6F6F76)
    } else {
        onSurface.copy(alpha = 0.72f)
    }

val Colors.appInfoContainer: Color
    get() = primary.copy(alpha = if (isLight) 0.08f else 0.18f)

val Colors.appInfoContent: Color
    get() = if (isLight) {
        primary
    } else {
        Color(0xFFE0D4FF)
    }

val Colors.appMutedInfoContainer: Color
    get() = if (isLight) {
        Color(0xFFF4F2F8)
    } else {
        Color.White.copy(alpha = 0.08f)
    }

val Colors.appMutedInfoContent: Color
    get() = if (isLight) {
        Color(0xFF5F5F67)
    } else {
        onSurface.copy(alpha = 0.76f)
    }

val Colors.appErrorContainer: Color
    get() = if (isLight) {
        Color(0xFFFFF0F0)
    } else {
        error.copy(alpha = 0.18f)
    }

val Colors.appErrorContent: Color
    get() = if (isLight) {
        error
    } else {
        error.copy(alpha = 0.90f)
    }
