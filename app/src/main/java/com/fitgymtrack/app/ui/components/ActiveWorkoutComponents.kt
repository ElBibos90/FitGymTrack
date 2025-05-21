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
    // Usiamo un valore ricordato per il conteggio alla rovescia, inizializzato una sola volta
    // al valore originale dei secondi, e poi modificato solo dal LaunchedEffect
    var timeLeft by remember(seconds) { mutableStateOf(seconds) }

    // Anche se il valore di isRunning cambia, non vogliamo resettare timeLeft
    // quindi lo gestiamo separatamente
    var timerActive by remember { mutableStateOf(isRunning) }

    // Aggiorniamo lo stato del timer quando cambia isRunning
    LaunchedEffect(isRunning) {
        timerActive = isRunning
    }

    // Effetto per il countdown - questo è il cuore della soluzione
    LaunchedEffect(key1 = seconds, key2 = isRunning) {
        // Se il timer non è attivo, non fare nulla
        if (!isRunning) return@LaunchedEffect

        // Assicuriamoci che il valore iniziale sia corretto
        timeLeft = seconds

        // Ogni secondo esatto, decrementa timeLeft di 1
        while (timeLeft > 0 && timerActive) {
            // Attendi un secondo esatto prima di decrementare
            delay(1000)
            timeLeft -= 1
        }

        // Se il timer raggiunge 0, notifica il completamento
        if (timeLeft <= 0) {
            onFinish()
        }
    }

    // Formatta il tempo in formato mm:ss
    val formattedTime = remember(timeLeft) {
        val minutes = timeLeft / 60
        val remainingSeconds = timeLeft % 60
        String.format("%02d:%02d", minutes, remainingSeconds)
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

            // Cambio nel pulsante: mostra "Ferma" o "Salta" in base allo stato del timer
            Button(
                onClick = {
                    timerActive = false // Ferma immediatamente il countdown
                    onStop()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timerActive) Indigo600 else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (timerActive) Color.White else MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (timerActive) "Ferma" else "Salta")
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
    seriesNumber: Int,
    onSeriesCompleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var timeLeft by remember { mutableStateOf(seconds) }
    var timerRunning by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    // Aggiorna timeLeft quando cambia seconds
    LaunchedEffect(seconds, seriesNumber) {
        timeLeft = seconds
        isCompleted = false
        timerRunning = false
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

            // Completamento automatico della serie
            onSeriesCompleted()

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

            HorizontalDivider(
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

            HorizontalDivider(
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

            HorizontalDivider(
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