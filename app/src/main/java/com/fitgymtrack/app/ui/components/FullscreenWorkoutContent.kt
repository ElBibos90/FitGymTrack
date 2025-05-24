package com.fitgymtrack.app.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.models.*
import com.fitgymtrack.app.ui.theme.BluePrimary
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.ui.theme.PurplePrimary
import com.fitgymtrack.app.utils.PlateauInfo
import com.fitgymtrack.app.utils.SoundManager
import com.fitgymtrack.app.utils.WeightFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Modalità fullscreen per l'allenamento - un esercizio alla volta
 * VERSIONE CORRETTA - Risolve problemi di tema e layout
 */
@Composable
fun FullscreenWorkoutContent(
    workout: ActiveWorkout,
    seriesState: CompletedSeriesState,
    isTimerRunning: Boolean,
    recoveryTime: Int,
    currentRecoveryExerciseId: Int?,
    currentSelectedExerciseId: Int?,
    exerciseValues: Map<Int, Pair<Float, Int>>,
    plateauInfo: Map<Int, PlateauInfo>,
    elapsedTime: String,
    onSeriesCompleted: (Int, Float, Int, Int) -> Unit,
    onStopTimer: () -> Unit,
    onSaveWorkout: () -> Unit = {},
    onSelectExercise: (Int) -> Unit = {},
    onExerciseValuesChanged: (Int, Pair<Float, Int>) -> Unit = { _, _ -> },
    onDismissPlateau: (Int) -> Unit = {},
    onShowPlateauDetails: (PlateauInfo) -> Unit = {},
    onShowGroupPlateauDetails: (String, List<PlateauInfo>) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    // Raggruppa gli esercizi come nel sistema esistente
    val exerciseGroups = groupExercisesByType(workout.esercizi)

    // Stato per navigazione
    var currentGroupIndex by remember { mutableStateOf(0) }

    // Calcola gruppo ed esercizio correnti
    val currentGroup = exerciseGroups.getOrNull(currentGroupIndex)
    if (currentGroup == null) {
        // Se non ci sono gruppi, mostra un messaggio o uno stato vuoto
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nessun esercizio disponibile",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        return
    }

    val currentExercise = if (currentGroup.size > 1) {
        // È un superset/circuit - usa l'esercizio selezionato o il primo
        currentGroup.find { it.id == currentSelectedExerciseId } ?: currentGroup.first()
    } else {
        // Esercizio singolo
        currentGroup.first()
    }

    // Serie completate
    val seriesMap = when (seriesState) {
        is CompletedSeriesState.Success -> seriesState.series
        else -> emptyMap()
    }

    val completedSeries = seriesMap[currentExercise.id] ?: emptyList()
    val currentSeriesNumber = completedSeries.size + 1

    // Valori peso e ripetizioni
    val values = exerciseValues[currentExercise.id]
    var currentWeight by remember(values) {
        mutableStateOf(values?.first ?: currentExercise.peso.toFloat())
    }
    var currentReps by remember(values) {
        mutableStateOf(values?.second ?: currentExercise.ripetizioni)
    }

    // Dialog states
    var showWeightPicker by remember { mutableStateOf(false) }
    var showRepsPicker by remember { mutableStateOf(false) }

    // Plateau info
    val exercisePlateau = plateauInfo[currentExercise.id]
    var showPlateauDialog by remember { mutableStateOf<PlateauInfo?>(null) }

    // Suoni
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // Calcola progresso generale
    val totalProgress = calculateWorkoutProgress(exerciseGroups, seriesMap)

    // Aggiorna i valori quando cambia l'esercizio
    LaunchedEffect(currentExercise.id, values) {
        values?.let {
            currentWeight = it.first
            currentReps = it.second
        }
    }

    // Funzioni di navigazione
    val navigateToNextGroup = {
        if (currentGroupIndex < exerciseGroups.size - 1) {
            currentGroupIndex++

            // Se il nuovo gruppo è un superset/circuit, seleziona il primo esercizio
            val newGroup = exerciseGroups[currentGroupIndex]
            if (newGroup.size > 1) {
                onSelectExercise(newGroup.first().id)
            }
        }
    }

    val navigateToPrevGroup = {
        if (currentGroupIndex > 0) {
            currentGroupIndex--

            // Se il nuovo gruppo è un superset/circuit, seleziona il primo esercizio
            val newGroup = exerciseGroups[currentGroupIndex]
            if (newGroup.size > 1) {
                onSelectExercise(newGroup.first().id)
            }
        }
    }

    // Gestione swipe
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = 100f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // RISOLTO: Usa il tema corretto
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(offsetX) > swipeThreshold) {
                            if (offsetX > 0) {
                                navigateToPrevGroup()
                            } else {
                                navigateToNextGroup()
                            }
                        }
                        offsetX = 0f
                    }
                ) { _, dragAmount ->
                    offsetX += dragAmount
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header con progresso
            FullscreenWorkoutHeader(
                currentGroupIndex = currentGroupIndex,
                totalGroups = exerciseGroups.size,
                totalProgress = totalProgress,
                elapsedTime = elapsedTime,
                onBack = onBack
            )

            // Contenuto principale - RIORGANIZZATO
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                FullscreenExerciseContentNew(
                    exercise = currentExercise,
                    group = currentGroup,
                    completedSeries = completedSeries,
                    currentSeriesNumber = currentSeriesNumber,
                    currentWeight = currentWeight,
                    currentReps = currentReps,
                    plateau = exercisePlateau,
                    isTimerRunning = isTimerRunning,
                    onWeightChange = { weight ->
                        currentWeight = weight
                        onExerciseValuesChanged(currentExercise.id, Pair(weight, currentReps))
                    },
                    onRepsChange = { reps ->
                        currentReps = reps
                        onExerciseValuesChanged(currentExercise.id, Pair(currentWeight, reps))
                    },
                    onShowWeightPicker = { showWeightPicker = true },
                    onShowRepsPicker = { showRepsPicker = true },
                    onPlateauClick = { exercisePlateau?.let(onShowPlateauDetails) },
                    onSupersetExerciseSelect = { exerciseId ->
                        onSelectExercise(exerciseId)
                    },
                    selectedExerciseId = currentSelectedExerciseId,
                    onCompleteSeries = {
                        onSeriesCompleted(
                            currentExercise.id,
                            currentWeight,
                            currentReps,
                            currentSeriesNumber
                        )

                        // Suono serie completata
                        coroutineScope.launch {
                            soundManager.playWorkoutSound(SoundManager.WorkoutSound.SERIES_COMPLETE)
                        }
                    }
                )
            }

            // Navigazione bottom
            FullscreenNavigationBar(
                canGoPrev = currentGroupIndex > 0,
                canGoNext = currentGroupIndex < exerciseGroups.size - 1,
                currentIndex = currentGroupIndex,
                totalCount = exerciseGroups.size,
                exerciseGroups = exerciseGroups,
                onPrevious = navigateToPrevGroup,
                onNext = navigateToNextGroup,
                onJumpTo = { index ->
                    currentGroupIndex = index

                    val newGroup = exerciseGroups[index]
                    if (newGroup.size > 1) {
                        onSelectExercise(newGroup.first().id)
                    }
                }
            )
        }

        // Timer di recupero overlay
        AnimatedVisibility(
            visible = recoveryTime > 0 && isTimerRunning,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            FullscreenRecoveryTimer(
                seconds = recoveryTime,
                onStop = onStopTimer
            )
        }
    }

    // Dialoghi
    if (showWeightPicker) {
        WeightPickerDialog(
            initialWeight = currentWeight,
            onDismiss = { showWeightPicker = false },
            onConfirm = { weight ->
                currentWeight = weight
                onExerciseValuesChanged(currentExercise.id, Pair(weight, currentReps))
                showWeightPicker = false
            }
        )
    }

    if (showRepsPicker) {
        RepsPickerDialog(
            initialReps = currentReps,
            isIsometric = currentExercise.isIsometric,
            onDismiss = { showRepsPicker = false },
            onConfirm = { reps ->
                currentReps = reps
                onExerciseValuesChanged(currentExercise.id, Pair(currentWeight, reps))
                showRepsPicker = false
            }
        )
    }

    showPlateauDialog?.let { plateau ->
        PlateauDetailDialog(
            plateauInfo = plateau,
            onDismiss = { showPlateauDialog = null }
        )
    }
}

/**
 * NUOVO: Contenuto dell'esercizio riorganizzato come nelle immagini
 */
@Composable
private fun FullscreenExerciseContentNew(
    exercise: WorkoutExercise,
    group: List<WorkoutExercise>,
    completedSeries: List<CompletedSeries>,
    currentSeriesNumber: Int,
    currentWeight: Float,
    currentReps: Int,
    plateau: PlateauInfo?,
    isTimerRunning: Boolean,
    onWeightChange: (Float) -> Unit,
    onRepsChange: (Int) -> Unit,
    onShowWeightPicker: () -> Unit,
    onShowRepsPicker: () -> Unit,
    onPlateauClick: () -> Unit,
    onSupersetExerciseSelect: (Int) -> Unit,
    selectedExerciseId: Int?,
    onCompleteSeries: () -> Unit
) {
    val isInGroup = group.size > 1
    val isSuperset = exercise.setType == "superset"
    val isCircuit = exercise.setType == "circuit"
    val isIsometric = exercise.isIsometric

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp), // Riduciamo il padding da 24.dp
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp) // Riduciamo lo spacing
    ) {
        // Nome dell'esercizio con badge integrato - PIÙ COMPATTO
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge integrato con il nome se è in un gruppo
            if (isInGroup) {
                Text(
                    text = "${if (isSuperset) "Superset" else "Circuit"}: ${exercise.nome}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp, // Riduciamo da 28.sp
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp, // Riduciamo da 32.sp
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = exercise.nome,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp, // Riduciamo da 28.sp
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp, // Riduciamo da 32.sp
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Tabs per navigazione superset/circuit - PIÙ COMPATTE
        if (isInGroup) {
            SupersetNavigationTabsCompact(
                exercises = group,
                selectedExerciseId = selectedExerciseId ?: exercise.id,
                onExerciseSelect = onSupersetExerciseSelect,
                isSuperset = isSuperset
            )
        }

        // Progress serie - PIÙ COMPATTO
        SeriesProgressCardCompact(
            currentSeries = currentSeriesNumber,
            totalSeries = exercise.serie,
            completedSeries = completedSeries.size
        )

        // CONTROLLI PESO E RIPETIZIONI/SECONDI - PIÙ COMPATTI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp) // Riduciamo da 16.dp
        ) {
            FullscreenValueCardCompact(
                label = "Peso",
                value = "${WeightFormatter.formatWeight(currentWeight)} kg",
                icon = Icons.Default.FitnessCenter,
                onTap = onShowWeightPicker,
                modifier = Modifier.weight(1f)
            )

            FullscreenValueCardCompact(
                label = if (isIsometric) "Secondi" else "Ripetizioni",
                value = currentReps.toString(),
                icon = if (isIsometric) Icons.Default.Timer else Icons.Default.Repeat,
                onTap = onShowRepsPicker,
                modifier = Modifier.weight(1f)
            )
        }

        // TIMER ISOMETRICO - PIÙ COMPATTO
        if (isIsometric) {
            FullscreenIsometricTimerCompact(
                seconds = currentReps,
                onTimerComplete = onCompleteSeries,
                isEnabled = completedSeries.size < exercise.serie,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Spazio flessibile ma limitato
        Spacer(modifier = Modifier.weight(1f, fill = false))

        // PULSANTE COMPLETA SERIE
        Button(
            onClick = onCompleteSeries,
            enabled = completedSeries.size < exercise.serie && !isTimerRunning,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // Riduciamo da 64.dp
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Completa",
                fontSize = 18.sp, // Riduciamo da 20.sp
                fontWeight = FontWeight.Bold
            )
        }

        // BADGE PLATEAU - COMPATTO
        if (plateau != null) {
            PlateauBadgeCompact(
                onClick = onPlateauClick
            )
        }
    }
}

/**
 * COMPATTO: Tabs per navigazione superset/circuit
 */
@Composable
private fun SupersetNavigationTabsCompact(
    exercises: List<WorkoutExercise>,
    selectedExerciseId: Int,
    onExerciseSelect: (Int) -> Unit,
    isSuperset: Boolean
) {
    val accentColor = if (isSuperset) PurplePrimary else BluePrimary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp) // Più compatto
    ) {
        exercises.forEach { exercise ->
            val isSelected = exercise.id == selectedExerciseId
            val shortName = truncateExerciseName(exercise.nome, 10) // Più corto

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExerciseSelect(exercise.id) },
                shape = RoundedCornerShape(16.dp), // Più piccolo
                color = if (isSelected) {
                    accentColor.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                border = if (isSelected) {
                    BorderStroke(1.dp, accentColor) // Bordo più sottile
                } else null
            ) {
                Text(
                    text = shortName,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp, // Più piccolo
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp), // Più piccolo
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * COMPATTO: Card per il progresso delle serie
 */
@Composable
private fun SeriesProgressCardCompact(
    currentSeries: Int,
    totalSeries: Int,
    completedSeries: Int
) {
    Surface(
        shape = RoundedCornerShape(10.dp), // Più piccolo
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp), // Più piccolo
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Serie $currentSeries",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 12.sp // Più piccolo
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$completedSeries/$totalSeries",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp, // Più piccolo
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Indicatori serie circolari compatti
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp) // Più piccolo
            ) {
                repeat(totalSeries) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp) // Più piccolo
                            .clip(CircleShape)
                            .background(
                                when {
                                    index < completedSeries -> MaterialTheme.colorScheme.primary
                                    index == completedSeries -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )
                    )
                }
            }
        }
    }
}

/**
 * COMPATTO: Card per valori peso/ripetizioni
 */
@Composable
private fun FullscreenValueCardCompact(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onTap() }
            .height(110.dp), // Più piccolo da 120.dp
        shape = RoundedCornerShape(14.dp), // Più piccolo
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp), // Più piccolo
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp) // Più piccolo
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 12.sp // Più piccolo
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp, // Più piccolo
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * COMPATTO: Timer isometrico fullscreen
 */
@Composable
private fun FullscreenIsometricTimerCompact(
    seconds: Int,
    onTimerComplete: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var timeLeft by remember(seconds) { mutableStateOf(seconds) }
    var isRunning by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // Reset quando cambiano i secondi
    LaunchedEffect(seconds) {
        timeLeft = seconds
        isCompleted = false
        isRunning = false
    }

    // Timer logic
    LaunchedEffect(isRunning) {
        if (isRunning && !isCompleted && isEnabled) {
            while (timeLeft > 0 && isRunning) {
                if (timeLeft <= 3) {
                    coroutineScope.launch {
                        soundManager.playWorkoutSound(SoundManager.WorkoutSound.COUNTDOWN_BEEP)
                    }
                }

                delay(1000L)
                timeLeft--
            }

            if (timeLeft <= 0) {
                isRunning = false
                isCompleted = true

                coroutineScope.launch {
                    soundManager.playWorkoutSound(SoundManager.WorkoutSound.TIMER_COMPLETE)
                }

                onTimerComplete()
            }
        }
    }

    val formattedTime = remember(timeLeft) {
        val minutes = timeLeft / 60
        val secs = timeLeft % 60
        String.format("%02d:%02d", minutes, secs)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = when {
            isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            timeLeft <= 3 && isRunning -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
        }
    ) {
        Row( // Cambiamo da Column a Row per renderlo più compatto
            modifier = Modifier.padding(16.dp), // Più piccolo
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Timer Isometrico",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 12.sp // Più piccolo
                )

                Text(
                    text = formattedTime,
                    color = when {
                        isCompleted -> MaterialTheme.colorScheme.primary
                        timeLeft <= 3 && isRunning -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 28.sp, // Più piccolo da 64.sp
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isCompleted && isEnabled) {
                Button(
                    onClick = {
                        if (timeLeft <= 0) {
                            timeLeft = seconds
                            isCompleted = false
                        }
                        isRunning = !isRunning
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning && timeLeft <= 3)
                            MaterialTheme.colorScheme.error
                        else if (isRunning)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.size(48.dp), // Più compatto
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completato",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * COMPATTO: Badge plateau
 */
@Composable
private fun PlateauBadgeCompact(
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFF5722).copy(alpha = 0.9f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), // Più piccolo
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.TrendingFlat,
                contentDescription = "Plateau",
                tint = Color.White,
                modifier = Modifier.size(14.dp) // Più piccolo
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Plateau",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp // Più piccolo
            )
        }
    }
}

/**
 * NUOVO: Card per valori - GRANDE E TOUCH-FRIENDLY
 */
@Composable
private fun FullscreenValueCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable { onTap() }
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * NUOVO: Timer isometrico fullscreen - GRANDE E IMMERSIVO
 */
@Composable
private fun FullscreenIsometricTimerNew(
    seconds: Int,
    onTimerComplete: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var timeLeft by remember(seconds) { mutableStateOf(seconds) }
    var isRunning by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // Reset quando cambiano i secondi
    LaunchedEffect(seconds) {
        timeLeft = seconds
        isCompleted = false
        isRunning = false
    }

    // Timer logic
    LaunchedEffect(isRunning) {
        if (isRunning && !isCompleted && isEnabled) {
            while (timeLeft > 0 && isRunning) {
                if (timeLeft <= 3) {
                    coroutineScope.launch {
                        soundManager.playWorkoutSound(SoundManager.WorkoutSound.COUNTDOWN_BEEP)
                    }
                }

                delay(1000L)
                timeLeft--
            }

            if (timeLeft <= 0) {
                isRunning = false
                isCompleted = true

                coroutineScope.launch {
                    soundManager.playWorkoutSound(SoundManager.WorkoutSound.TIMER_COMPLETE)
                }

                onTimerComplete()
            }
        }
    }

    val formattedTime = remember(timeLeft) {
        val minutes = timeLeft / 60
        val secs = timeLeft % 60
        String.format("%02d:%02d", minutes, secs)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = when {
            isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            timeLeft <= 3 && isRunning -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
        }
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Timer Isometrico",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timer grande e prominente
            Text(
                text = formattedTime,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    timeLeft <= 3 && isRunning -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!isCompleted && isEnabled) {
                Button(
                    onClick = {
                        if (timeLeft <= 0) {
                            timeLeft = seconds
                            isCompleted = false
                        }
                        isRunning = !isRunning
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning && timeLeft <= 3)
                            MaterialTheme.colorScheme.error
                        else if (isRunning)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isRunning) "Pausa" else "Avvia",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isCompleted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completato",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Serie Completata!",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Resto delle funzioni helper rimangono uguali...
private fun truncateExerciseName(name: String, maxLength: Int = 12): String {
    return if (name.length <= maxLength) {
        name
    } else {
        name.take(maxLength - 2) + ".."
    }
}

@Composable
private fun Badge(
    text: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = if (onClick != null) {
            Modifier.clickable { onClick() }
        } else {
            Modifier
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Badge plateau ottimizzato per fullscreen
 */
@Composable
private fun PlateauBadgeFullscreen(
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(25.dp),
        color = Color(0xFFFF5722).copy(alpha = 0.9f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.TrendingFlat,
                contentDescription = "Plateau",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "→ Plateau",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// Altre funzioni helper rimangono uguali (FullscreenWorkoutHeader, FullscreenNavigationBar, etc.)
@Composable
private fun FullscreenWorkoutHeader(
    currentGroupIndex: Int,
    totalGroups: Int,
    totalProgress: Float,
    elapsedTime: String,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Esercizio ${currentGroupIndex + 1} di $totalGroups",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Durata: $elapsedTime",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { totalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun FullscreenNavigationBar(
    canGoPrev: Boolean,
    canGoNext: Boolean,
    currentIndex: Int,
    totalCount: Int,
    exerciseGroups: List<List<WorkoutExercise>>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJumpTo: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPrevious,
                enabled = canGoPrev,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Precedente",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Prec")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(totalCount) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentIndex) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    index < currentIndex -> MaterialTheme.colorScheme.primary
                                    index == currentIndex -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )
                            .clickable { onJumpTo(index) }
                    )
                }
            }

            Button(
                onClick = onNext,
                enabled = canGoNext,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Succ")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Successivo",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FullscreenRecoveryTimer(
    seconds: Int,
    onStop: () -> Unit
) {
    // AGGIUNTO: Gestione suoni e countdown come nel RecoveryTimer originale
    var timeLeft by remember(seconds) { mutableStateOf(seconds) }
    var timerActive by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // AGGIUNTO: Effetto per il countdown con suoni
    LaunchedEffect(key1 = seconds) {
        timeLeft = seconds
        timerActive = true

        // Countdown con suoni
        while (timeLeft > 0 && timerActive) {
            // 🔊 BEEP PER GLI ULTIMI 3 SECONDI DEL RECUPERO
            if (timeLeft <= 3 && timeLeft > 0) {
                coroutineScope.launch {
                    soundManager.playWorkoutSound(SoundManager.WorkoutSound.COUNTDOWN_BEEP)
                }
            }

            delay(1000)
            timeLeft -= 1
        }

        // 🔊 SUONO FINALE RECUPERO COMPLETATO
        if (timeLeft <= 0 && timerActive) {
            coroutineScope.launch {
                soundManager.playWorkoutSound(SoundManager.WorkoutSound.REST_COMPLETE)
            }
        }
    }

    val formattedTime = remember(timeLeft) { // CAMBIATO: usa timeLeft invece di seconds
        val minutes = timeLeft / 60
        val secs = timeLeft % 60
        String.format("%02d:%02d", minutes, secs)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Recupero",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = formattedTime,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = {
                    timerActive = false // AGGIUNTO: Ferma il timer
                    onStop()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    "Salta",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// Funzioni utility
private fun groupExercisesByType(exercises: List<WorkoutExercise>): List<List<WorkoutExercise>> {
    val result = mutableListOf<List<WorkoutExercise>>()
    var currentGroup = mutableListOf<WorkoutExercise>()

    exercises.forEach { exercise ->
        if (currentGroup.isEmpty()) {
            currentGroup.add(exercise)
        } else {
            val prevExercise = currentGroup.last()

            if (exercise.linkedToPrevious &&
                (exercise.setType == prevExercise.setType) &&
                (exercise.setType == "superset" || exercise.setType == "circuit")) {
                currentGroup.add(exercise)
            } else {
                result.add(currentGroup.toList())
                currentGroup = mutableListOf(exercise)
            }
        }
    }

    if (currentGroup.isNotEmpty()) {
        result.add(currentGroup)
    }

    return result
}

private fun calculateWorkoutProgress(
    exerciseGroups: List<List<WorkoutExercise>>,
    seriesMap: Map<Int, List<CompletedSeries>>
): Float {
    if (exerciseGroups.isEmpty()) return 0f

    val completedGroups = exerciseGroups.count { group ->
        group.all { exercise ->
            val completedSeries = seriesMap[exercise.id] ?: emptyList()
            completedSeries.size >= exercise.serie
        }
    }

    return completedGroups.toFloat() / exerciseGroups.size.toFloat()
}