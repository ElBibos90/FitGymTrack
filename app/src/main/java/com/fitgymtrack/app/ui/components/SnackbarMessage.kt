package com.fitgymtrack.app.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.ui.theme.Indigo600
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SnackbarMessage(
    message: String,
    modifier: Modifier = Modifier,
    isSuccess: Boolean = true,
    onDismiss: () -> Unit

) {
    rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }

    // Determine color based on success/error
    val backgroundColor = if (isSuccess) {
        Color(0xFF4CAF50) // Success green
    } else {
        Color(0xFFF44336) // Error red
    }

    // Auto-dismiss after 3 seconds
    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            isVisible = true
            delay(3000)
            isVisible = false
            delay(300) // Wait for animation to complete
            onDismiss()
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = if (isLandscape) 16.dp else 32.dp,
                start = 16.dp,
                end = 16.dp
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}