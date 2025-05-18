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
import com.fitgymtrack.app.models.CompletedSeries
import com.fitgymtrack.app.models.WorkoutExercise
import com.fitgymtrack.app.ui.theme.BluePrimary
import com.fitgymtrack.app.utils.WeightFormatter

/**
 * Card per visualizzare un gruppo di esercizi in superset
 * Permette la navigazione tra gli esercizi del superset
 * L'espansione/compressione è gestita esternamente per mantenere lo stato
 */
@Composable
fun SupersetCard(
    exercises: List<WorkoutExercise>,
    selectedExerciseId: Int,
    serieCompletate: Map<Int, List<CompletedSeries>>,
    isTimerRunning: Boolean,
    onExerciseSelect: (Int) -> Unit,
    onAddSeries: (Int, Float, Int, Int) -> Unit,
    isExpanded: Boolean = false, // Default a false per iniziare chiuso
    onExpandToggle: () -> Unit = {}, // Callback chiamato quando l'utente fa click sull'header
    exerciseValues: Map<Int, Pair<Float, Int>> = emptyMap()
) {
    val selectedExerciseIndex = exercises.indexOfFirst { it.id == selectedExerciseId }
    val selectedExercise = exercises.getOrNull(selectedExerciseIndex) ?: exercises.firstOrNull() ?: return
    val completedSeries = serieCompletate[selectedExercise.id] ?: emptyList()

    // Calcolo del progresso del superset
    val totalCompletedSeries = exercises.sumOf { exercise ->
        (serieCompletate[exercise.id]?.size ?: 0)
    }
    val totalRequiredSeries = exercises.sumOf { it.serie }
    val supersetProgress = if (totalRequiredSeries > 0) {
        totalCompletedSeries.toFloat() / totalRequiredSeries
    } else {
        0f
    }

    // Calcola se il superset è completato
    val isSupersetCompleted = exercises.all { exercise ->
        val series = serieCompletate[exercise.id] ?: emptyList()
        series.size >= exercise.serie
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSupersetCompleted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSupersetCompleted) 0.dp else 2.dp
        )
    ) {
        // Header del superset - cliccando qui si espande/comprime
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpandToggle), // Assicura che il click funzioni
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Titolo e info
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Superset",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${exercises.size} esercizi",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Indicatore di progresso
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { supersetProgress },
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "$totalCompletedSeries/$totalRequiredSeries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Icona che mostra lo stato espanso/compresso
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Contenuto espandibile - verrà mostrato solo se isExpanded è true
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
                // Selettore di esercizi
                SupersetExerciseTabs(
                    exercises = exercises,
                    selectedExerciseId = selectedExercise.id,
                    onExerciseSelect = onExerciseSelect,
                    serieCompletate = serieCompletate
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Esercizio selezionato
                val exerciseValues = exerciseValues[selectedExercise.id]
                ExerciseProgressItem(
                    exercise = selectedExercise,
                    completedSeries = completedSeries,
                    isTimerRunning = isTimerRunning,
                    onAddSeries = { weight, reps ->
                        onAddSeries(selectedExercise.id, weight, reps, completedSeries.size + 1)
                    },
                    isLastExercise = selectedExerciseIndex == exercises.size - 1,
                    isCompleted = false,
                    isInGroup = true,
                    initialWeight = exerciseValues?.first,
                    initialReps = exerciseValues?.second
                )
            }
        }
    }
}

/**
 * Card per un superset completato
 */
@Composable
fun CompletedSupersetCard(
    title: String,
    exercises: List<WorkoutExercise>,
    serieCompletate: Map<Int, List<CompletedSeries>>,
    isExpanded: Boolean = false,
    onExpandToggle: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandToggle() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${exercises.size} esercizi completati",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    .padding(16.dp)
            ) {
                exercises.forEachIndexed { index, exercise ->
                    val completedSeries = serieCompletate[exercise.id] ?: emptyList()

                    if (index > 0) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exercise.nome,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = "Serie completate: ${completedSeries.size}/${exercise.serie}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (completedSeries.isNotEmpty()) {
                            val lastSeries = completedSeries.last()

                            Text(
                                text = "${WeightFormatter.formatWeight(lastSeries.peso)}kg × ${lastSeries.ripetizioni}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card per visualizzare un superset moderno e compatto
 * Anche questa versione deve iniziare chiusa e essere espandibile
 */
@Composable
fun SupersetGroupCard(
    title: String,
    exercises: List<WorkoutExercise>,
    selectedExerciseId: Int,
    serieCompletate: Map<Int, List<CompletedSeries>>,
    onExerciseSelected: (Int) -> Unit,
    onAddSeries: (Int, Float, Int, Int) -> Unit,
    isTimerRunning: Boolean = false,
    exerciseValues: Map<Int, Pair<Float, Int>> = emptyMap(),
    isExpanded: Boolean = false, // Aggiungiamo questo parametro con default a false
    onExpandToggle: () -> Unit = {}, // Aggiungiamo un callback per gestire l'espansione
    onExerciseValuesChanged: (Int, Pair<Float, Int>) -> Unit = { _, _ -> }
) {
    val selectedExerciseIndex = exercises.indexOfFirst { it.id == selectedExerciseId }
    val selectedExercise = exercises.getOrNull(selectedExerciseIndex) ?: exercises.firstOrNull() ?: return
    val completedSeries = serieCompletate[selectedExercise.id] ?: emptyList()
    val values = exerciseValues[selectedExercise.id]

    // Spostiamo le dichiarazioni delle variabili qui in modo che siano accessibili in tutto il componente
    var showWeightPicker by remember { mutableStateOf(false) }
    var currentWeight by remember { mutableStateOf(values?.first ?: selectedExercise.peso.toFloat()) }
    var showRepsPicker by remember { mutableStateOf(false) }
    var currentReps by remember { mutableStateOf(values?.second ?: selectedExercise.ripetizioni) }

    // Aggiorniamo i valori correnti quando cambiano i valori dall'esterno
    LaunchedEffect(values) {
        values?.let {
            currentWeight = it.first
            currentReps = it.second
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Header del superset - cliccabile per espandere/comprimere
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BluePrimary.copy(alpha = 0.15f),
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
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = BluePrimary
                    )

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BluePrimary
                        )

                        Text(
                            text = "${exercises.size} esercizi",
                            style = MaterialTheme.typography.bodySmall,
                            color = BluePrimary.copy(alpha = 0.7f)
                        )
                    }
                }

                // Indicatore di progresso e icona per espandere/comprimere
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Calcolo del progresso
                    val totalSeries = selectedExercise.serie
                    val completedSeriesCount = completedSeries.size
                    val progress = if (totalSeries > 0) {
                        completedSeriesCount.toFloat() / totalSeries
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = BluePrimary,
                        trackColor = BluePrimary.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "$completedSeriesCount/$totalSeries",
                        style = MaterialTheme.typography.bodySmall,
                        color = BluePrimary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Icona che mostra lo stato di espansione
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                        tint = BluePrimary
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

                // Esercizio selezionato - con sfondo che rispetta il tema
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    // Usa i colori del tema invece di valori fissi
                    color = MaterialTheme.colorScheme.surface,
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
                            color = MaterialTheme.colorScheme.onSurface // Usa il colore del tema
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
                                color = BluePrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Serie ${completedSeries.size + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = BluePrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            // Progress and info
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val totalSeries = selectedExercise.serie
                                val completedSeriesCount = completedSeries.size
                                val progress = if (totalSeries > 0) {
                                    completedSeriesCount.toFloat() / totalSeries
                                } else 0f

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = BluePrimary,
                                    // Usa un colore adatto al tema
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "$completedSeriesCount/$totalSeries",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface // Usa il colore del tema
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Peso e ripetizioni
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Peso - con onClick per aprire il dialog
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                // Usa i colori del tema
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { showWeightPicker = true }
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${WeightFormatter.formatWeight(currentWeight)} kg",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Ripetizioni - con onClick per aprire il dialog
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                // Usa i colori del tema
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { showRepsPicker = true }
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "$currentReps",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Bottone completa
                        Button(
                            onClick = {
                                onAddSeries(
                                    selectedExercise.id,
                                    currentWeight, // Ora accessibile qui
                                    currentReps,   // Ora accessibile qui
                                    completedSeries.size + 1
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BluePrimary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isTimerRunning && completedSeries.size < selectedExercise.serie
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

        // Dialog per scegliere il peso
        if (showWeightPicker) {
            WeightPickerDialog(
                initialWeight = currentWeight,
                onDismiss = { showWeightPicker = false },
                onConfirm = { weight ->
                    currentWeight = weight
                    showWeightPicker = false
                    // Notifica il cambio di valori
                    onExerciseValuesChanged(selectedExercise.id, Pair(weight, currentReps))
                }
            )
        }

        // Dialog per scegliere le ripetizioni
        if (showRepsPicker) {
            RepsPickerDialog(
                initialReps = currentReps,
                isIsometric = selectedExercise.isIsometric,
                onDismiss = { showRepsPicker = false },
                onConfirm = { reps ->
                    currentReps = reps
                    showRepsPicker = false
                    // Notifica il cambio di valori
                    onExerciseValuesChanged(selectedExercise.id, Pair(currentWeight, reps))
                }
            )
        }
    }
}

/**
 * Tab per selezionare gli esercizi in un superset
 */
@Composable
fun SupersetExerciseTabs(
    exercises: List<WorkoutExercise>,
    selectedExerciseId: Int,
    onExerciseSelect: (Int) -> Unit,
    serieCompletate: Map<Int, List<CompletedSeries>>,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        exercises.forEach { exercise ->
            val isSelected = exercise.id == selectedExerciseId
            val completedSeries = serieCompletate[exercise.id] ?: emptyList()
            val isCompleted = completedSeries.size >= exercise.serie

            val backgroundColor = when {
                isSelected -> BluePrimary
                isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }

            val textColor = when {
                isSelected -> Color.White
                isCompleted -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(4.dp),
                color = backgroundColor,
                onClick = { onExerciseSelect(exercise.id) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = if (compact) 6.dp else 8.dp,
                            horizontal = if (compact) 8.dp else 12.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = exercise.nome.split(" ").take(2).joinToString(" "),
                        style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}