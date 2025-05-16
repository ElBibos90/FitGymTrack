package com.fitgymtrack.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitgymtrack.app.models.CompletedSeries
import com.fitgymtrack.app.models.WorkoutExercise
import com.fitgymtrack.app.ui.theme.BluePrimary
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.ui.theme.PurplePrimary

/**
 * Componente per visualizzare un gruppo di esercizi (superset o circuit)
 */
@Composable
fun ExerciseGroupCard(
    exercises: List<WorkoutExercise>,
    completedSeries: Map<Int, List<CompletedSeries>>,
    isTimerRunning: Boolean,
    onAddSeries: (Int, Float, Int, Int) -> Unit,
    isCompleted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val setType = exercises.firstOrNull()?.setType ?: "normal"
    // Inizializzato a false (compresso) invece di true
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (setType) {
                "superset" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                "circuit" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompleted) 0.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Header del gruppo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Icona in base al tipo di set
                    Icon(
                        imageVector = when (setType) {
                            "superset" -> Icons.Default.SwapHoriz
                            "circuit" -> Icons.Default.Repeat
                            else -> Icons.Default.SwapHoriz
                        },
                        contentDescription = null,
                        tint = when (setType) {
                            "superset" -> PurplePrimary
                            "circuit" -> BluePrimary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )

                    // Titolo del gruppo
                    Text(
                        text = when (setType) {
                            "superset" -> "Superset"
                            "circuit" -> "Circuit"
                            else -> "Gruppo"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Badge con numero esercizi
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (setType) {
                                    "superset" -> PurplePrimary.copy(alpha = 0.2f)
                                    "circuit" -> BluePrimary.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${exercises.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (setType) {
                                "superset" -> PurplePrimary
                                "circuit" -> BluePrimary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }

                // Icona per espandere/contrarre
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Contenuto del gruppo
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // Esercizi nel gruppo
                    exercises.forEachIndexed { index, exercise ->
                        val exerciseSeries = completedSeries[exercise.id] ?: emptyList()

                        // Aggiungiamo un divisore tra gli esercizi
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Visualizziamo l'esercizio
                        // Creiamo una lambda che adatta la funzione onAddSeries a ciò che si aspetta ExerciseProgressItem
                        ExerciseProgressItem(
                            exercise = exercise,
                            completedSeries = exerciseSeries,
                            isTimerRunning = isTimerRunning,
                            onAddSeries = { weight, reps ->
                                // Adattiamo i parametri alla funzione generale
                                onAddSeries(exercise.id, weight, reps, exerciseSeries.size + 1)
                            },
                            isLastExercise = index == exercises.size - 1,
                            isCompleted = isCompleted,
                            isInGroup = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(
                                    width = 1.dp,
                                    color = when (setType) {
                                        "superset" -> PurplePrimary.copy(alpha = 0.3f)
                                        "circuit" -> BluePrimary.copy(alpha = 0.3f)
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    }

                    // Informazioni sul gruppo
                    GroupInfoBox(
                        exercises = exercises,
                        completedSeries = completedSeries,
                        setType = setType
                    )
                }
            }
        }
    }
}

/**
 * Nuovo componente per i gruppi di esercizi che gestisce lo stato di espansione
 */
@Composable
fun ManagedExerciseGroupCard(
    exercises: List<WorkoutExercise>,
    completedSeries: Map<Int, List<CompletedSeries>>,
    isTimerRunning: Boolean,
    onAddSeries: (Int, Float, Int, Int) -> Unit,
    isCompleted: Boolean = false,
    isExpanded: Boolean = false,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val setType = exercises.firstOrNull()?.setType ?: "normal"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (setType) {
                "superset" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                "circuit" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompleted) 0.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Header del gruppo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Icona in base al tipo di set
                    Icon(
                        imageVector = when (setType) {
                            "superset" -> Icons.Default.SwapHoriz
                            "circuit" -> Icons.Default.Repeat
                            else -> Icons.Default.SwapHoriz
                        },
                        contentDescription = null,
                        tint = when (setType) {
                            "superset" -> PurplePrimary
                            "circuit" -> BluePrimary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )

                    // Titolo del gruppo
                    Text(
                        text = when (setType) {
                            "superset" -> "Superset"
                            "circuit" -> "Circuit"
                            else -> "Gruppo"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Badge con numero esercizi
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (setType) {
                                    "superset" -> PurplePrimary.copy(alpha = 0.2f)
                                    "circuit" -> BluePrimary.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${exercises.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (setType) {
                                "superset" -> PurplePrimary
                                "circuit" -> BluePrimary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }

                // Add details about progress
                val totalSets = exercises.firstOrNull()?.serie ?: 0
                val completedSets = if (exercises.isNotEmpty()) {
                    exercises.minOf { exercise ->
                        completedSeries[exercise.id]?.size ?: 0
                    }
                } else 0

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$completedSets/$totalSets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (setType) {
                            "superset" -> PurplePrimary
                            "circuit" -> BluePrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    // Icona per espandere/contrarre
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Contenuto del gruppo
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    // Esercizi nel gruppo
                    exercises.forEachIndexed { index, exercise ->
                        val exerciseSeries = completedSeries[exercise.id] ?: emptyList()

                        // Aggiungiamo un divisore tra gli esercizi
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Visualizziamo l'esercizio
                        ExerciseProgressItem(
                            exercise = exercise,
                            completedSeries = exerciseSeries,
                            isTimerRunning = isTimerRunning,
                            onAddSeries = { weight, reps ->
                                // Adattiamo i parametri alla funzione generale
                                onAddSeries(exercise.id, weight, reps, exerciseSeries.size + 1)
                            },
                            isLastExercise = index == exercises.size - 1,
                            isCompleted = isCompleted,
                            isInGroup = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(
                                    width = 1.dp,
                                    color = when (setType) {
                                        "superset" -> PurplePrimary.copy(alpha = 0.3f)
                                        "circuit" -> BluePrimary.copy(alpha = 0.3f)
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    }

                    // Informazioni sul gruppo
                    GroupInfoBox(
                        exercises = exercises,
                        completedSeries = completedSeries,
                        setType = setType
                    )
                }
            }
        }
    }
}

/**
 * Box informativo che mostra dettagli sul gruppo di esercizi
 */
@Composable
fun GroupInfoBox(
    exercises: List<WorkoutExercise>,
    completedSeries: Map<Int, List<CompletedSeries>>,
    setType: String
) {
    val totalSets = exercises.firstOrNull()?.serie ?: 0
    val completedGroupSets = calculateCompletedGroupSets(exercises, completedSeries, totalSets)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = when (setType) {
            "superset" -> PurplePrimary.copy(alpha = 0.1f)
            "circuit" -> BluePrimary.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when (setType) {
                        "superset" -> "Superset Progress"
                        "circuit" -> "Circuit Progress"
                        else -> "Group Progress"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = when (setType) {
                        "superset" -> PurplePrimary
                        "circuit" -> BluePrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Text(
                    text = "$completedGroupSets/$totalSets rounds completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress indicator - Aggiornato per usare la lambda
            LinearProgressIndicator(
                progress = { if (totalSets > 0) completedGroupSets.toFloat() / totalSets.toFloat() else 0f },
                modifier = Modifier
                    .width(100.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when (setType) {
                    "superset" -> PurplePrimary
                    "circuit" -> BluePrimary
                    else -> Indigo600
                },
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
            )
        }
    }
}

/**
 * Calcola quanti giri completi del gruppo sono stati completati
 */
private fun calculateCompletedGroupSets(
    exercises: List<WorkoutExercise>,
    completedSeries: Map<Int, List<CompletedSeries>>,
    totalSets: Int
): Int {
    if (exercises.isEmpty() || totalSets <= 0) return 0

    // Troviamo il minimo numero di serie completate tra tutti gli esercizi del gruppo
    return exercises.minOfOrNull { exercise ->
        completedSeries[exercise.id]?.size ?: 0
    } ?: 0
}