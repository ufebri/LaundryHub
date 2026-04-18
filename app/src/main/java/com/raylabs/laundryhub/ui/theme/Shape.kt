package com.raylabs.laundryhub.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

val Shapes.surfaceHero: Shape
    get() = RoundedCornerShape(24.dp)

val Shapes.surfacePanel: Shape
    get() = RoundedCornerShape(18.dp)

val Shapes.pill: Shape
    get() = RoundedCornerShape(999.dp)

val Shapes.modalSheetTop: Shape
    get() = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
