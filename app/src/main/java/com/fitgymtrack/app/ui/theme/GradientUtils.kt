package com.fitgymtrack.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object GradientUtils {
    // Predefined gradients
    val blueGradient = Brush.verticalGradient(
        colors = listOf(BluePrimary, BlueSecondary)
    )

    val greenGradient = Brush.verticalGradient(
        colors = listOf(GreenPrimary, GreenSecondary)
    )

    val purpleGradient = Brush.verticalGradient(
        colors = listOf(PurplePrimary, PurpleSecondary)
    )

    // Custom gradient factory
    fun customGradient(startColor: Color, endColor: Color): Brush {
        return Brush.verticalGradient(
            colors = listOf(startColor, endColor)
        )
    }
}