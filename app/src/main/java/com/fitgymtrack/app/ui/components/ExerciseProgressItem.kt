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
import com.fitgymtrack.app.ui.theme.Indigo600

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
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var currentWeight by remember { mutableStateOf(exercise.peso.toFloat()) }
    var currentReps by remember { mutableStateOf(exercise.ripetizioni) }
    var showWeightPicker by remember { mutableStateOf(false) }
    var showRepsPicker by remember { mutableStateOf(false) }

    // Determina se l'esercizio è isometrico
    val isIsometricByFlag = remember(exercise) {
        // Verifica se c'è un campo isIsometric booleano o un campo isIsometricInt
        val isIsometricBool = when {
            exercise::class.java.declaredFields.any { it.name == "isIsometric" } ->
                try { exercise.isIsometric } catch (e: Exception) { false }
            exercise::class.java.declaredFields.any { it.name == "isIsometricInt" } ->
                try { exercise.javaClass.getMethod("getIsIsometricInt").invoke(exercise) as Int > 0 } catch (e: Exception) { false }
            exercise::class.java.declaredFields.any { it.name == "is_isometric" } ->
                try {
                    val value = exercise.javaClass.getMethod("get_is_isometric").invoke(exercise)
                    value is Boolean && value || value is String && (value == "1" || value.equals("true", ignoreCase = true)) || value is Int && value > 0
                } catch (e: Exception) { false }
            else -> false
        }
        isIsometricBool
    }

    val isIsometricByName = remember(exercise) {
        exercise.nome.lowercase().contains("isometrico") ||
                exercise.nome.lowercase().contains("camminata") ||
                exercise.nome.lowercase().contains("plank") ||
                exercise.nome.lowercase().contains("wall sit") ||
                exercise.nome.lowercase().contains("tenuta")
    }

    val isIsometric = isIsometricByFlag || isIsometricByName

    // Animazione per la rotazione dell'icona di espansione
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 180f,
        label = "rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompleted) 0.dp else 2.dp
        )
    ) {
        // Header dell'esercizio (sempre visibile)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp),
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
                // Sezione per il peso e le ripetizioni/secondi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Peso
                    ValueChip(
                        label = "Peso (kg)",
                        value = formatWeight(currentWeight),
                        onClick = { showWeightPicker = true },
                        modifier = Modifier.weight(1f)
                    )

                    // Ripetizioni o secondi
                    ValueChip(
                        label = if (isIsometric) "Secondi" else "Ripetizioni",
                        value = currentReps.toString(),
                        onClick = { showRepsPicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Timer isometrico
                if (isIsometric) {
                    Spacer(modifier = Modifier.height(8.dp))
                    IsometricTimer(seconds = currentReps)
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

                // Pulsanti di azione
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
                            containerColor = Indigo600,
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
                text = "${formatWeight(weight)} kg × $reps ${if (isIsometric) "sec" else "rep"}",
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

/**
 * Formatta il peso per la visualizzazione
 */
private fun formatWeight(weight: Float): String {
    val wholeNumber = weight.toInt()
    val fraction = weight - wholeNumber

    return when {
        fraction == 0f -> wholeNumber.toString()
        fraction == 0.125f -> "$wholeNumber.125"
        fraction == 0.25f -> "$wholeNumber.25"
        fraction == 0.375f -> "$wholeNumber.375"
        fraction == 0.5f -> "$wholeNumber.5"
        fraction == 0.625f -> "$wholeNumber.625"
        fraction == 0.75f -> "$wholeNumber.75"
        fraction == 0.875f -> "$wholeNumber.875"
        else -> String.format("%.2f", weight)
    }
}