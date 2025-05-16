package com.fitgymtrack.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.models.CompletedSeries
import com.fitgymtrack.app.models.WorkoutExercise
import com.fitgymtrack.app.ui.theme.BluePrimary
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.ui.theme.PurplePrimary
import com.fitgymtrack.app.utils.WeightFormatter

/**
 * Componente che visualizza un gruppo di esercizi (superset o circuit)
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

/**
 * Nuovo componente che visualizza un gruppo di allenamento (superset o circuito)
 * con interfaccia mobile-friendly simile alle immagini di riferimento
 */
@Composable
fun ModernWorkoutGroupCard(
    title: String,
    subtitle: String,
    exercises: List<WorkoutExercise>,
    completedSeries: Map<Int, List<CompletedSeries>>,
    isSuperset: Boolean = true,
    roundIndex: Int = 1,
    roundTotal: Int = 3,
    onExerciseSelected: (WorkoutExercise) -> Unit = {},
    onAddSeries: (Int, Float, Int, Int) -> Unit = { _, _, _, _ -> },
    groupIndex: Int = 0,
    expandedGroups: MutableMap<Int, Boolean> = remember { mutableStateMapOf() },
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSuperset) PurplePrimary else BluePrimary
    var selectedExerciseIndex by remember { mutableStateOf(0) }

    // Get or set initial expansion state
    val isExpanded = expandedGroups.getOrPut(groupIndex) { false }

    // Calculate progress
    val totalSets = exercises.firstOrNull()?.serie ?: 0
    val completedGroupSets = calculateCompletedGroupSets(exercises, completedSeries, totalSets)
    val progress = if (totalSets > 0) completedGroupSets.toFloat() / totalSets.toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Header del gruppo con icona, titolo e freccia espandi/comprimi
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            onClick = { expandedGroups[groupIndex] = !isExpanded }
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
                        imageVector = if (isSuperset) Icons.Default.SwapHoriz else Icons.Default.Repeat,
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
                        progress = { progress },
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

        // Contenuto espandibile con gli esercizi
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = backgroundColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Selettore di esercizi
                Text(
                    text = "Seleziona Esercizio",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Round indicator (for circuit only)
                if (!isSuperset) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Round $roundIndex/$roundTotal",
                            style = MaterialTheme.typography.bodySmall,
                            color = backgroundColor,
                            modifier = Modifier
                                .background(
                                    color = backgroundColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Esercizi
                exercises.forEachIndexed { index, exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                color = if (index == selectedExerciseIndex)
                                    backgroundColor.copy(alpha = 0.2f)
                                else
                                    Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedExerciseIndex = index
                                onExerciseSelected(exercise)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon with the same style as in the images
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = if (index == selectedExerciseIndex) backgroundColor else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = backgroundColor.copy(alpha = if (index == selectedExerciseIndex) 1f else 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (index == selectedExerciseIndex) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = exercise.nome,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (index == selectedExerciseIndex) FontWeight.Medium else FontWeight.Normal,
                                color = if (index == selectedExerciseIndex)
                                    backgroundColor
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Optional additional info for isometric exercises
                        if (exercise.isIsometric) {
                            Text(
                                text = "(sec)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Area dell'esercizio attualmente selezionato
                if (exercises.isNotEmpty() && selectedExerciseIndex < exercises.size) {
                    val selectedExercise = exercises[selectedExerciseIndex]
                    val exerciseSeries = completedSeries[selectedExercise.id] ?: emptyList()
                    val seriesCompleted = exerciseSeries.size
                    val seriesTotal = selectedExercise.serie

                    Spacer(modifier = Modifier.height(16.dp))

                    // Current exercise section - black box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        // Exercise name
                        Text(
                            text = selectedExercise.nome,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (selectedExercise.isIsometric) {
                            Text(
                                text = "(sec)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Round indicator or set number
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(26.dp)
                                    .background(
                                        color = backgroundColor.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(13.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (!isSuperset && roundIndex != null)
                                        "Round $roundIndex/$roundTotal"
                                    else
                                        "Serie ${seriesCompleted + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = backgroundColor
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Progress indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(
                                    progress = { seriesCompleted.toFloat() / seriesTotal.toFloat() },
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

                                // Info button
                                IconButton(
                                    onClick = { /* Show exercise info */ },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Informazioni esercizio",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Exercise actions - Add the ability to track sets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Weight input
                            var showWeightPicker by remember { mutableStateOf(false) }
                            var currentWeight by remember { mutableStateOf(selectedExercise.peso.toFloat()) }

                            ValueChip(
                                label = "Peso",
                                value = WeightFormatter.formatWeight(currentWeight) + " kg",
                                onClick = { showWeightPicker = true },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Reps input
                            var showRepsPicker by remember { mutableStateOf(false) }
                            var currentReps by remember { mutableStateOf(selectedExercise.ripetizioni) }

                            ValueChip(
                                label = if (selectedExercise.isIsometric) "Secondi" else "Ripetizioni",
                                value = currentReps.toString(),
                                onClick = { showRepsPicker = true },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Complete set button
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
                                enabled = seriesCompleted < seriesTotal
                            ) {
                                Text("Completa")
                            }

                            // Weight picker dialog
                            if (showWeightPicker) {
                                WeightPickerDialog(
                                    initialWeight = currentWeight,
                                    onDismiss = { showWeightPicker = false },
                                    onConfirm = {
                                        currentWeight = it
                                        showWeightPicker = false
                                    }
                                )
                            }

                            // Reps picker dialog
                            if (showRepsPicker) {
                                RepsPickerDialog(
                                    initialReps = currentReps,
                                    isIsometric = selectedExercise.isIsometric,
                                    onDismiss = { showRepsPicker = false },
                                    onConfirm = {
                                        currentReps = it
                                        showRepsPicker = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}