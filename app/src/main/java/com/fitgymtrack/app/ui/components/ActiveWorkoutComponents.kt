package com.fitgymtrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.ui.theme.Indigo600
import kotlinx.coroutines.delay

/**
 * Timer di recupero dopo una serie
 */
@Composable
fun RecoveryTimer(
    seconds: Int,
    isRunning: Boolean,
    onFinish: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var timeLeft by remember { mutableStateOf(seconds) }
    var timerRunning by remember { mutableStateOf(isRunning) }

    // Aggiorna timeLeft quando cambia seconds
    LaunchedEffect(seconds) {
        timeLeft = seconds
    }

    // Aggiorna timerRunning quando cambia isRunning
    LaunchedEffect(isRunning) {
        timerRunning = isRunning
    }

    // Effetto per gestire il countdown
    LaunchedEffect(key1 = timerRunning) {
        if (timerRunning) {
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            timerRunning = false
            onFinish()
        }
    }

    // Formatta il tempo in formato mm:ss
    val formattedTime = remember(timeLeft) {
        val minutes = timeLeft / 60
        val seconds = timeLeft % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Indigo600.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = Indigo600
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Recupero",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 20.sp
                    )
                }
            }

            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timerRunning) Indigo600 else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (timerRunning) Color.White else MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (timerRunning) "Ferma" else "Salta")
            }
        }
    }
}

/**
 * Timer per esercizi isometrici
 */
@Composable
fun IsometricTimer(
    seconds: Int,
    modifier: Modifier = Modifier
) {
    var timeLeft by remember { mutableStateOf(seconds) }
    var timerRunning by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    // Aggiorna timeLeft quando cambia seconds
    LaunchedEffect(seconds) {
        timeLeft = seconds
        isCompleted = false
    }

    // Effetto per gestire il countdown
    LaunchedEffect(key1 = timerRunning) {
        if (timerRunning) {
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            timerRunning = false
            isCompleted = true

            // Reset automatico dopo 2 secondi
            delay(2000L)
            timeLeft = seconds
            isCompleted = false
        }
    }

    // Formatta il tempo in formato mm:ss
    val formattedTime = remember(timeLeft) {
        val minutes = timeLeft / 60
        val seconds = timeLeft % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = when {
            isCompleted -> MaterialTheme.colorScheme.primaryContainer
            timerRunning -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Timer Isometrico",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.titleLarge,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    timerRunning -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!isCompleted) {
                        timerRunning = !timerRunning
                    }
                },
                enabled = !isCompleted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isCompleted -> MaterialTheme.colorScheme.primary
                        timerRunning -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    }
                )
            ) {
                Text(
                    text = when {
                        isCompleted -> "Completato"
                        timerRunning -> "Pausa"
                        else -> "Avvia"
                    }
                )
            }
        }
    }
}

/**
 * Componente per visualizzare il progresso dell'allenamento
 */
@Composable
fun WorkoutProgressIndicator(
    activeExercises: Int,
    completedExercises: Int,
    totalExercises: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Esercizi attivi
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Attivi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "$activeExercises",
                    style = MaterialTheme.typography.titleMedium,
                    color = Indigo600
                )
            }

            Divider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            // Esercizi completati
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Completati",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "$completedExercises",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Divider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            // Esercizi totali
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Totali",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "$totalExercises",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            // Progresso
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Progresso",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

/**
 * Dialog di conferma per uscire dall'allenamento
 */
@Composable
fun ExitWorkoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Conferma uscita")
        },
        text = {
            Text(
                text = "Sei sicuro di voler uscire dall'allenamento in corso? Tutti i progressi non salvati andranno persi."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Esci")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Annulla")
            }
        }
    )
}

/**
 * Dialog di conferma per completare l'allenamento
 */
@Composable
fun CompleteWorkoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Conferma completamento")
        },
        text = {
            Text(
                text = "Vuoi completare l'allenamento corrente? Potrai visualizzarlo nello storico allenamenti."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Completa")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Annulla")
            }
        }
    )
}

/**
 * Chip per visualizzare un valore (peso o ripetizioni)
 */
@Composable
fun ValueChip(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(IntrinsicSize.Min),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}