package com.fitgymtrack.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitgymtrack.app.models.CompletedSeries
import com.fitgymtrack.app.models.WorkoutExercise
import com.fitgymtrack.app.ui.theme.BluePrimary
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.ui.theme.PurplePrimary
import com.fitgymtrack.app.utils.WeightFormatter

/**
 * Componente per visualizzare un esercizio durante l'allenamento
 */
@Composable
fun ExerciseProgressItem(
    exercise: WorkoutExercise,
    completedSeries: List<CompletedSeries>,
    isTimerRunning: Boolean,
    onAddSeries: (Float, Int) -> Unit,
    isLastExercise: Boolean = false,
    isCompleted: Boolean = false,
    isInGroup: Boolean = false, // Parametro per indicare se l'esercizio è in un gruppo
    modifier: Modifier = Modifier,
    initialWeight: Float? = null, // NUOVO: peso iniziale basato sullo storico
    initialReps: Int? = null // NUOVO: ripetizioni iniziali basate sullo storico
) {
    // Inizializzato a false per avere gli esercizi collassati all'inizio
    var isExpanded by remember { mutableStateOf(false) }

    // NUOVO: Inizializza con valori iniziali se forniti, altrimenti usa i valori dell'esercizio
    var currentWeight by remember(initialWeight) {
        mutableStateOf(initialWeight ?: exercise.peso.toFloat())
    }
    var currentReps by remember(initialReps) {
        mutableStateOf(initialReps ?: exercise.ripetizioni)
    }

    var showWeightPicker by remember { mutableStateOf(false) }
    var showRepsPicker by remember { mutableStateOf(false) }

    // Determina se l'esercizio è isometrico
    val isIsometric = remember(exercise) {
        // Utilizza direttamente la proprietà isIsometric che è già un Boolean
        val isIsometricFromField = exercise.isIsometric

        // Manteniamo anche il rilevamento basato sul nome come fallback
        val isIsometricByName = exercise.nome.lowercase().contains("isometrico") ||
                exercise.nome.lowercase().contains("camminata") ||
                exercise.nome.lowercase().contains("plank") ||
                exercise.nome.lowercase().contains("wall sit") ||
                exercise.nome.lowercase().contains("tenuta") ||
                exercise.nome.lowercase().contains("(sec)")

        isIsometricFromField || isIsometricByName
    }

    // Animazione per la rotazione dell'icona di espansione
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(if (isInGroup) 4.dp else 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.primaryContainer
            else if (isInGroup)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompleted || isInGroup) 0.dp else 2.dp
        )
    ) {
        // Header dell'esercizio (sempre visibile)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(if (isInGroup) 12.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icona di completamento
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Indicatore di superset/circuit
                if (exercise.linkedToPrevious && !isInGroup) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = when (exercise.setType) {
                            "superset" -> PurplePrimary
                            "circuit" -> BluePrimary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Nome e progresso
                Column {
                    Text(
                        text = exercise.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${completedSeries.size}/${exercise.serie} serie completate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Icona di espansione
            IconButton(
                onClick = { isExpanded = !isExpanded }
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                    modifier = Modifier.rotate(rotationState)
                )
            }
        }

        // Contenuto espandibile
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Non mostrare i controlli di peso e ripetizioni se l'esercizio è completato
                if (!isCompleted) {
                    // Sezione per il peso e le ripetizioni/secondi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Peso
                        ValueChip(
                            label = "Peso (kg)",
                            value = WeightFormatter.formatWeight(currentWeight),
                            onClick = { showWeightPicker = true },
                            modifier = Modifier.weight(1f)
                        )

                        // Ripetizioni o secondi - Cambiamo l'etichetta in base al tipo di esercizio
                        ValueChip(
                            label = if (isIsometric) "Secondi" else "Ripetizioni",
                            value = currentReps.toString(),
                            onClick = { showRepsPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Timer isometrico
                    IsometricTimer(
                        seconds = currentReps,
                        seriesNumber = completedSeries.size + 1, // Passa il numero della serie corrente
                        onSeriesCompleted = {
                            if (!isCompleted && completedSeries.size < exercise.serie) {
                                onAddSeries(currentWeight, currentReps)
                            }
                        }
                    )

                }

                // Serie completate
                if (completedSeries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Serie completate:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Lista delle serie completate
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        completedSeries.forEachIndexed { index, series ->
                            CompletedSeriesItem(
                                seriesNumber = index + 1,
                                weight = series.peso,
                                reps = series.ripetizioni,
                                isIsometric = isIsometric
                            )
                        }
                    }
                }

                // Pulsanti di azione solo se non è completato
                if (!isCompleted) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Griglia di serie
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mostra una griglia di pulsanti numerati per le serie
                        for (i in 1..exercise.serie) {
                            SeriesButton(
                                number = i,
                                isCompleted = i <= completedSeries.size,
                                isActive = i == completedSeries.size + 1,
                                onClick = {
                                    if (i == completedSeries.size + 1 && !isTimerRunning) {
                                        onAddSeries(currentWeight, currentReps)
                                    }
                                },
                                isEnabled = i == completedSeries.size + 1 && !isTimerRunning
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pulsante per completare la serie
                    Button(
                        onClick = { onAddSeries(currentWeight, currentReps) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isTimerRunning && completedSeries.size < exercise.serie,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (exercise.setType) {
                                "superset" -> PurplePrimary
                                "circuit" -> BluePrimary
                                else -> Indigo600
                            },
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Completa serie ${completedSeries.size + 1}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Mostra i dialoghi per la selezione del peso e delle ripetizioni
        if (showWeightPicker) {
            WeightPickerDialog(
                initialWeight = currentWeight,
                onDismiss = { showWeightPicker = false },
                onConfirm = { newWeight ->
                    currentWeight = newWeight
                    showWeightPicker = false
                }
            )
        }

        if (showRepsPicker) {
            RepsPickerDialog(
                initialReps = currentReps,
                isIsometric = isIsometric,
                onDismiss = { showRepsPicker = false },
                onConfirm = { newReps ->
                    currentReps = newReps
                    showRepsPicker = false
                }
            )
        }
    }
}

/**
 * Componente per visualizzare una serie completata
 */
@Composable
fun CompletedSeriesItem(
    seriesNumber: Int,
    weight: Float,
    reps: Int,
    isIsometric: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Serie $seriesNumber",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "${WeightFormatter.formatWeight(weight)} kg × $reps ${if (isIsometric) "sec" else "rep"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Pulsante per selezionare una serie
 */
@Composable
fun SeriesButton(
    number: Int,
    isCompleted: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    isEnabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isActive -> Indigo600
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                isCompleted || isActive -> Color.White
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}