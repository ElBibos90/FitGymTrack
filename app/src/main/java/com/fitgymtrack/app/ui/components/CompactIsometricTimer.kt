package com.fitgymtrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Timer compatto per esercizi isometrici all'interno di superset e circuit
 */
@Composable
fun CompactIsometricTimer(
    seconds: Int,
    isRunning: Boolean = false,
    onTimerRunningChange: (Boolean) -> Unit = {},
    onTimerComplete: () -> Unit = {},
    onTimerReset: () -> Unit = {},
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF8B5CF6) // Colore viola per default
) {
    var timeLeft by remember { mutableStateOf(seconds) }
    var timerFinished by remember { mutableStateOf(false) }

    // Aggiorna timeLeft quando cambia seconds
    LaunchedEffect(seconds) {
        timeLeft = seconds
        timerFinished = false
    }

    // Effetto per gestire il countdown
    LaunchedEffect(key1 = isRunning) {
        if (isRunning) {
            while (timeLeft > 0) {
                delay(1000L) // Delay costante di esattamente 1 secondo
                timeLeft -= 1 // Decremento semplice e preciso
            }
            timerFinished = true
            onTimerComplete()
            onTimerRunningChange(false)
        }
    }

    // Formatta il tempo in formato mm:ss
    val formattedTime = remember(timeLeft) {
        val minutes = timeLeft / 60
        val remainingSeconds = timeLeft % 60
        String.format(Locale.getDefault(),"%02d:%02d", minutes, remainingSeconds)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF2A2A2A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer icon and time display
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (timerFinished) accentColor else Color.White
                )
            }

            // Timer controls
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (timerFinished) {
                    // Reset button
                    IconButton(
                        onClick = {
                            timeLeft = seconds
                            timerFinished = false
                            onTimerReset()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // Play/pause button
                    FilledIconButton(
                        onClick = { onTimerRunningChange(!isRunning) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = accentColor
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}