// GradientCard.kt
package com.fitgymtrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fitgymtrack.app.ui.theme.GradientUtils

@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: Brush,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient),
            content = content
        )
    }
}

@Composable
fun BlueGradientCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    GradientCard(
        modifier = modifier,
        gradient = GradientUtils.blueGradient,
        content = content
    )
}

@Composable
fun GreenGradientCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    GradientCard(
        modifier = modifier,
        gradient = GradientUtils.greenGradient,
        content = content
    )
}

@Composable
fun PurpleGradientCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    GradientCard(
        modifier = modifier,
        gradient = GradientUtils.purpleGradient,
        content = content
    )
}