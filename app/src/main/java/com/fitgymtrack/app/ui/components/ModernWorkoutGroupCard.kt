package com.fitgymtrack.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitgymtrack.app.models.CompletedSeries
import com.fitgymtrack.app.models.WorkoutExercise
import com.fitgymtrack.app.ui.theme.BluePrimary
import com.fitgymtrack.app.ui.theme.PurplePrimary
import com.fitgymtrack.app.utils.WeightFormatter

/**
 * Nuovo componente che visualizza un gruppo di allenamento (superset o circuito)
 * con interfaccia mobile-friendly simile alle immagini di riferimento
 */
@Composable
fun ModernWorkoutGroupCard(
    title: String,
    subtitle: String,
    exercises: List<WorkoutExercise>,
    selectedExerciseId: Int,
    serieCompletate: Map<Int, List<CompletedSeries>>,
    onExerciseSelected: (Int) -> Unit = {},
    onAddSeries: (Int, Float, Int, Int) -> Unit = { _, _, _, _ -> },
    isTimerRunning: Boolean = false,
    exerciseValues: Map<Int, Pair<Float, Int>> = emptyMap(),
    isExpanded: Boolean = false, // Aggiungiamo questo parametro con default a false
    onExpandToggle: () -> Unit = {} // Aggiungiamo un callback per gestire l'espansione
) {
    val backgroundColor = if (title.contains("Superset", ignoreCase = true)) PurplePrimary else BluePrimary
    val selectedExerciseIndex = exercises.indexOfFirst { it.id == selectedExerciseId }
    val selectedExercise = exercises.getOrNull(selectedExerciseIndex) ?: exercises.firstOrNull() ?: return
    val completedSeries = serieCompletate[selectedExercise.id] ?: emptyList()
    val seriesCompleted = completedSeries.size
    val seriesTotal = selectedExercise.serie

    // Variabili per peso e ripetizioni
    var showWeightPicker by remember { mutableStateOf(false) }
    var showRepsPicker by remember { mutableStateOf(false) }

    // Ottieni i valori attuali dai valori passati o dall'esercizio
    val values = exerciseValues[selectedExercise.id]
    var currentWeight by remember(values) { mutableStateOf(values?.first ?: selectedExercise.peso.toFloat()) }
    var currentReps by remember(values) { mutableStateOf(values?.second ?: selectedExercise.ripetizioni) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Header del gruppo con icona, titolo e freccia espandi/comprimi
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isExpanded) 0.dp else 12.dp,
                bottomEnd = if (isExpanded) 0.dp else 12.dp
            ),
            onClick = onExpandToggle // Gestiamo il click sull'header
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (title.contains("Superset", ignoreCase = true)) Icons.Default.SwapHoriz else Icons.Default.Repeat,
                        contentDescription = null,
                        tint = backgroundColor
                    )

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = backgroundColor
                        )

                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = backgroundColor.copy(alpha = 0.7f)
                        )
                    }
                }

                // Indicatore di progresso e freccia
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Progress bar
                    LinearProgressIndicator(
                        progress = { if (seriesTotal > 0) seriesCompleted.toFloat() / seriesTotal.toFloat() else 0f },
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = backgroundColor,
                        trackColor = backgroundColor.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Expand/collapse arrow
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                        tint = backgroundColor
                    )
                }
            }
        }

        // Contenuto espandibile con AnimatedVisibility
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                // Selettore di esercizi
                SupersetExerciseTabs(
                    exercises = exercises,
                    selectedExerciseId = selectedExercise.id,
                    onExerciseSelect = onExerciseSelected,
                    serieCompletate = serieCompletate,
                    compact = true
                )

                // Esercizio selezionato - con sfondo grigio scuro
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Nome esercizio
                        Text(
                            text = selectedExercise.nome,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Exercise progress indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Serie indicator
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = backgroundColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Serie ${seriesCompleted + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = backgroundColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            // Progress and info
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(
                                    progress = { if (seriesTotal > 0) seriesCompleted.toFloat() / seriesTotal.toFloat() else 0f },
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = backgroundColor,
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "$seriesCompleted/$seriesTotal",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Timer isometrico se l'esercizio è isometrico
                        if (selectedExercise.isIsometric) {
                            var isTimerRunning by remember { mutableStateOf(false) }
                            var timerCompleted by remember { mutableStateOf(false) }
                            var timeLeft by remember { mutableStateOf(currentReps) }

                            // Aggiorna timeLeft quando cambiano le ripetizioni/secondi
                            LaunchedEffect(currentReps) {
                                if (!isTimerRunning) {
                                    timeLeft = currentReps
                                }
                            }

                            // Effetto per gestire il countdown quando il timer è attivo
                            LaunchedEffect(isTimerRunning) {
                                if (isTimerRunning) {
                                    while (timeLeft > 0 && isTimerRunning) {
                                        delay(1000L)
                                        timeLeft--
                                    }

                                    if (timeLeft <= 0) {
                                        isTimerRunning = false
                                        timerCompleted = true

                                        // Auto-completa la serie quando il timer raggiunge zero
                                        if (seriesCompleted < seriesTotal) {
                                            onAddSeries(
                                                selectedExercise.id,
                                                currentWeight,
                                                currentReps,
                                                seriesCompleted + 1
                                            )
                                        }
                                    }
                                }
                            }

                            CompactIsometricTimer(
                                seconds = currentReps,
                                isRunning = isTimerRunning,
                                onTimerRunningChange = { isTimerRunning = it },
                                onTimerComplete = {
                                    // Auto-completa la serie
                                    onAddSeries(
                                        selectedExercise.id,
                                        currentWeight,
                                        currentReps,
                                        seriesCompleted + 1
                                    )
                                },
                                onTimerReset = { timerCompleted = false },
                                accentColor = backgroundColor
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Peso e ripetizioni
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Peso
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF444444),
                                onClick = { showWeightPicker = true }  // Aggiunto handler onClick
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Peso",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${WeightFormatter.formatWeight(currentWeight)} kg",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Ripetizioni
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF444444),
                                onClick = { showRepsPicker = true }  // Aggiunto handler onClick
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (selectedExercise.isIsometric) "Secondi" else "Ripetizioni",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${currentReps}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottone completa
                        Button(
                            onClick = {
                                onAddSeries(
                                    selectedExercise.id,
                                    currentWeight,
                                    currentReps,
                                    seriesCompleted + 1
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = backgroundColor
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isTimerRunning && seriesCompleted < seriesTotal
                        ) {
                            Text(
                                text = "Completa",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Dialog per la selezione del peso
        if (showWeightPicker) {
            WeightPickerDialog(
                initialWeight = currentWeight,
                onDismiss = { showWeightPicker = false },
                onConfirm = { weight ->
                    // Aggiorna il peso e chiudi il dialog
                    currentWeight = weight
                    showWeightPicker = false
                }
            )
        }

        // Dialog per la selezione delle ripetizioni
        if (showRepsPicker) {
            RepsPickerDialog(
                initialReps = currentReps,
                isIsometric = selectedExercise.isIsometric,
                onDismiss = { showRepsPicker = false },
                onConfirm = { reps ->
                    // Aggiorna le ripetizioni e chiudi il dialog
                    currentReps = reps
                    showRepsPicker = false
                }
            )
        }
    }
}