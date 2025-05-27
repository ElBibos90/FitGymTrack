package com.fitgymtrack.app.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo


/**
 * Enum per le dimensioni dello schermo
 */
enum class ScreenSize {
    Small,   // < 600dp height
    Medium,  // 600-800dp height
    Large    // > 800dp height
}

/**
 * Hook per rilevare la dimensione dello schermo
 */
@Composable
private fun SmallScreenCompactControls(
    currentSeries: Int,
    totalSeries: Int,
    completedSeries: Int,
    currentWeight: Float,
    currentReps: Int,
    isIsometric: Boolean,
    onShowWeightPicker: () -> Unit,
    onShowRepsPicker: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Serie + Progress
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Serie",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )

                Text(
                    text = "$completedSeries/$totalSeries",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Mini progress bar
                LinearProgressIndicator(
                    progress = { completedSeries.toFloat() / totalSeries.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }
        }

        // Peso
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { onShowWeightPicker() },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Peso",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = "${WeightFormatter.formatWeight(currentWeight)} kg",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Ripetizioni/Secondi
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { onShowRepsPicker() },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isIsometric) Icons.Default.Timer else Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isIsometric) "Sec" else "Rep",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = currentReps.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun rememberScreenSize(): ScreenSize {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenHeightDp) {
        when {
            configuration.screenHeightDp < 600 -> ScreenSize.Small
            configuration.screenHeightDp < 800 -> ScreenSize.Medium
            else -> ScreenSize.Large
        }
    }
}

/**
 * VERSIONE RESPONSIVE del FullscreenWorkoutContent
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
    onBack: () -> Unit = {},
    onNavigateToNext: () -> Unit = {},  // Callback per navigare al prossimo esercizio
    onWorkoutComplete: () -> Unit = {}  // Callback quando l'allenamento è completato
) {
    val screenSize = rememberScreenSize()

    // Log per debug
    val configuration = LocalConfiguration.current

    when (screenSize) {
        ScreenSize.Small -> SmallScreenWorkoutLayout(
            workout = workout,
            seriesState = seriesState,
            isTimerRunning = isTimerRunning,
            recoveryTime = recoveryTime,
            currentRecoveryExerciseId = currentRecoveryExerciseId,
            currentSelectedExerciseId = currentSelectedExerciseId,
            exerciseValues = exerciseValues,
            plateauInfo = plateauInfo,
            elapsedTime = elapsedTime,
            onSeriesCompleted = onSeriesCompleted,
            onStopTimer = onStopTimer,
            onSaveWorkout = onSaveWorkout,
            onSelectExercise = onSelectExercise,
            onExerciseValuesChanged = onExerciseValuesChanged,
            onDismissPlateau = onDismissPlateau,
            onShowPlateauDetails = onShowPlateauDetails,
            onShowGroupPlateauDetails = onShowGroupPlateauDetails,
            onBack = onBack,
            onNavigateToNext = onNavigateToNext,
            onWorkoutComplete = onWorkoutComplete
        )

        ScreenSize.Medium -> MediumScreenWorkoutLayout(
            workout = workout,
            seriesState = seriesState,
            isTimerRunning = isTimerRunning,
            recoveryTime = recoveryTime,
            currentRecoveryExerciseId = currentRecoveryExerciseId,
            currentSelectedExerciseId = currentSelectedExerciseId,
            exerciseValues = exerciseValues,
            plateauInfo = plateauInfo,
            elapsedTime = elapsedTime,
            onSeriesCompleted = onSeriesCompleted,
            onStopTimer = onStopTimer,
            onSaveWorkout = onSaveWorkout,
            onSelectExercise = onSelectExercise,
            onExerciseValuesChanged = onExerciseValuesChanged,
            onDismissPlateau = onDismissPlateau,
            onShowPlateauDetails = onShowPlateauDetails,
            onShowGroupPlateauDetails = onShowGroupPlateauDetails,
            onBack = onBack,
            onNavigateToNext = onNavigateToNext,
            onWorkoutComplete = onWorkoutComplete
        )

        ScreenSize.Large -> LargeScreenWorkoutLayout(
            workout = workout,
            seriesState = seriesState,
            isTimerRunning = isTimerRunning,
            recoveryTime = recoveryTime,
            currentRecoveryExerciseId = currentRecoveryExerciseId,
            currentSelectedExerciseId = currentSelectedExerciseId,
            exerciseValues = exerciseValues,
            plateauInfo = plateauInfo,
            elapsedTime = elapsedTime,
            onSeriesCompleted = onSeriesCompleted,
            onStopTimer = onStopTimer,
            onSaveWorkout = onSaveWorkout,
            onSelectExercise = onSelectExercise,
            onExerciseValuesChanged = onExerciseValuesChanged,
            onDismissPlateau = onDismissPlateau,
            onShowPlateauDetails = onShowPlateauDetails,
            onShowGroupPlateauDetails = onShowGroupPlateauDetails,
            onBack = onBack,
            onNavigateToNext = onNavigateToNext,
            onWorkoutComplete = onWorkoutComplete
        )
    }
}

/**
 * LAYOUT SMALL SCREEN - Tutto scrollable + mini navigation
 */
@Composable
private fun SmallScreenWorkoutLayout(
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
    onSaveWorkout: () -> Unit,
    onSelectExercise: (Int) -> Unit,
    onExerciseValuesChanged: (Int, Pair<Float, Int>) -> Unit,
    onDismissPlateau: (Int) -> Unit,
    onShowPlateauDetails: (PlateauInfo) -> Unit,
    onShowGroupPlateauDetails: (String, List<PlateauInfo>) -> Unit,
    onBack: () -> Unit,
    onNavigateToNext: () -> Unit = {},
    onWorkoutComplete: () -> Unit = {}
) {
    // Setup base come il layout originale
    val exerciseGroups = groupExercisesByType(workout.esercizi)
    var currentGroupIndex by remember { mutableStateOf(0) }

    val currentGroup = exerciseGroups.getOrNull(currentGroupIndex) ?: return
    val currentExercise = if (currentGroup.size > 1) {
        currentGroup.find { it.id == currentSelectedExerciseId } ?: currentGroup.first()
    } else {
        currentGroup.first()
    }

    val seriesMap = when (seriesState) {
        is CompletedSeriesState.Success -> seriesState.series
        else -> emptyMap()
    }

    val completedSeries = seriesMap[currentExercise.id] ?: emptyList()
    val currentSeriesNumber = completedSeries.size + 1

    // Controlla se l'esercizio corrente è completato
    val isCurrentExerciseCompleted = completedSeries.size >= currentExercise.serie
    val hasMoreExercises = currentGroupIndex < exerciseGroups.size - 1

    // Controlla se TUTTI gli esercizi dell'allenamento sono completati
    val isAllWorkoutCompleted = calculateAllExercisesCompleted(exerciseGroups, seriesMap)

    // Funzione helper per auto-navigazione dopo timer
    val onRecoveryTimerComplete = {
        if (isCurrentExerciseCompleted && hasMoreExercises) {
            // Passa automaticamente al prossimo esercizio
            currentGroupIndex++
            val newGroup = exerciseGroups[currentGroupIndex]
            if (newGroup.size > 1) {
                onSelectExercise(newGroup.first().id)
            }
            onNavigateToNext()
        } else if (isCurrentExerciseCompleted && !hasMoreExercises) {
            // Allenamento completato
            onWorkoutComplete()
        }
        onStopTimer()
    }

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
    var showCompleteWorkoutDialog by remember { mutableStateOf(false) }

    // Plateau info
    val exercisePlateau = plateauInfo[currentExercise.id]
    var showPlateauDialog by remember { mutableStateOf<PlateauInfo?>(null) }

    // Suoni
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val coroutineScope = rememberCoroutineScope()

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
            val newGroup = exerciseGroups[currentGroupIndex]
            if (newGroup.size > 1) {
                onSelectExercise(newGroup.first().id)
            }
        }
    }

    val navigateToPrevGroup = {
        if (currentGroupIndex > 0) {
            currentGroupIndex--
            val newGroup = exerciseGroups[currentGroupIndex]
            if (newGroup.size > 1) {
                onSelectExercise(newGroup.first().id)
            }
        }
    }

    // **LAYOUT SMALL SCREEN - TUTTO SCROLLABLE**
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header compatto
            SmallScreenHeader(
                currentGroupIndex = currentGroupIndex,
                totalGroups = exerciseGroups.size,
                elapsedTime = elapsedTime,
                currentGroup = currentGroup,
                onBack = onBack
            )

            // Contenuto scrollable
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = rememberLazyListState(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nome esercizio
                item {
                    SmallScreenExerciseName(
                        exercise = currentExercise,
                        group = currentGroup
                    )
                }

                // Tabs per superset/circuit
                if (currentGroup.size > 1) {
                    item {
                        SmallScreenSupersetTabs(
                            exercises = currentGroup,
                            selectedExerciseId = currentSelectedExerciseId ?: currentExercise.id,
                            onExerciseSelect = onSelectExercise,
                            isSuperset = currentExercise.setType == "superset"
                        )
                    }
                }

                // Progress serie + Peso + Ripetizioni - TUTTO IN UNA RIGA
                item {
                    SmallScreenCompactControls(
                        currentSeries = currentSeriesNumber,
                        totalSeries = currentExercise.serie,
                        completedSeries = completedSeries.size,
                        currentWeight = currentWeight,
                        currentReps = currentReps,
                        isIsometric = currentExercise.isIsometric,
                        onShowWeightPicker = { showWeightPicker = true },
                        onShowRepsPicker = { showRepsPicker = true }
                    )
                }

                // Timer isometrico - PROGRESS BAR SEMPLICE
                if (currentExercise.isIsometric) {
                    item {
                        SmallScreenIsometricTimer(
                            seconds = currentReps,
                            currentSeriesNumber = currentSeriesNumber,
                            onTimerComplete = {
                                onSeriesCompleted(
                                    currentExercise.id,
                                    currentWeight,
                                    currentReps,
                                    currentSeriesNumber
                                )
                                coroutineScope.launch {
                                    soundManager.playWorkoutSound(SoundManager.WorkoutSound.SERIES_COMPLETE)
                                }
                            },
                            isEnabled = completedSeries.size < currentExercise.serie
                        )
                    }
                }

                // Pulsante completa serie
                item {
                    WorkoutCompleteButton(
                        isWorkoutCompleted = isAllWorkoutCompleted,
                        currentSeriesNumber = currentSeriesNumber,
                        isEnabled = completedSeries.size < currentExercise.serie && !isTimerRunning,
                        onClick = {
                            if (isAllWorkoutCompleted) {
                                showCompleteWorkoutDialog = true
                            } else {
                                onSeriesCompleted(
                                    currentExercise.id,
                                    currentWeight,
                                    currentReps,
                                    currentSeriesNumber
                                )
                                coroutineScope.launch {
                                    soundManager.playWorkoutSound(SoundManager.WorkoutSound.SERIES_COMPLETE)
                                }
                            }
                        }
                    )
                }

                // Badge plateau
                if (exercisePlateau != null) {
                    item {
                        SmallScreenPlateauBadge(
                            onClick = { onShowPlateauDetails(exercisePlateau) }
                        )
                    }
                }

                // Spazio extra per la mini navigation
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Mini Navigation - FLOATING
        SmallScreenMiniNavigation(
            canGoPrev = currentGroupIndex > 0,
            canGoNext = currentGroupIndex < exerciseGroups.size - 1,
            currentIndex = currentGroupIndex,
            totalCount = exerciseGroups.size,
            onPrevious = navigateToPrevGroup,
            onNext = navigateToNextGroup,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Timer di recupero overlay
        AnimatedVisibility(
            visible = recoveryTime > 0 && isTimerRunning,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
        ) {
            SmallScreenRecoveryTimer(
                seconds = recoveryTime,
                isExerciseCompleted = isCurrentExerciseCompleted,
                hasMoreExercises = hasMoreExercises,
                onStop = onRecoveryTimerComplete
            )
        }
    }

    // Dialog per peso
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

    // Dialog per ripetizioni
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

    // Dialog plateau
    showPlateauDialog?.let { plateau ->
        PlateauDetailDialog(
            plateauInfo = plateau,
            onDismiss = { showPlateauDialog = null }
        )
    }

    // Dialog completamento allenamento
    if (showCompleteWorkoutDialog) {
        CompleteWorkoutConfirmDialog(
            onDismiss = { showCompleteWorkoutDialog = false },
            onConfirm = {
                showCompleteWorkoutDialog = false
                onWorkoutComplete()
            }
        )
    }

    // Dialog completamento allenamento
    if (showCompleteWorkoutDialog) {
        CompleteWorkoutConfirmDialog(
            onDismiss = { showCompleteWorkoutDialog = false },
            onConfirm = {
                showCompleteWorkoutDialog = false
                onWorkoutComplete()
            }
        )
    }

    // Dialog completamento allenamento
    if (showCompleteWorkoutDialog) {
        CompleteWorkoutConfirmDialog(
            onDismiss = { showCompleteWorkoutDialog = false },
            onConfirm = {
                showCompleteWorkoutDialog = false
                onWorkoutComplete()
            }
        )
    }
}

// ======== COMPONENTI SMALL SCREEN ========

@Composable
private fun SmallScreenHeader(
    currentGroupIndex: Int,
    totalGroups: Int,
    elapsedTime: String,
    currentGroup: List<WorkoutExercise>,
    onBack: () -> Unit
) {
    val isInGroup = currentGroup.size > 1
    val isSuperset = currentGroup.firstOrNull()?.setType == "superset"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentGroupIndex + 1}/$totalGroups",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            // Badge centrale per Superset/Circuit
            if (isInGroup) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = (if (isSuperset) PurplePrimary else BluePrimary).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isSuperset) "Superset" else "Circuit",
                        color = if (isSuperset) PurplePrimary else BluePrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallScreenExerciseName(
    exercise: WorkoutExercise,
    group: List<WorkoutExercise>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = exercise.nome,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun SmallScreenSupersetTabs(
    exercises: List<WorkoutExercise>,
    selectedExerciseId: Int,
    onExerciseSelect: (Int) -> Unit,
    isSuperset: Boolean
) {
    val accentColor = if (isSuperset) PurplePrimary else BluePrimary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        exercises.forEach { exercise ->
            val isSelected = exercise.id == selectedExerciseId
            val shortName = truncateExerciseName(exercise.nome, 8)

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExerciseSelect(exercise.id) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) {
                    accentColor.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ) {
                Text(
                    text = shortName,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SmallScreenIsometricTimer(
    seconds: Int,
    currentSeriesNumber: Int,
    onTimerComplete: () -> Unit,
    isEnabled: Boolean
) {
    var timeLeft by remember(seconds, currentSeriesNumber) { mutableStateOf(seconds) }
    var isRunning by remember(currentSeriesNumber) { mutableStateOf(false) }
    var isCompleted by remember(currentSeriesNumber) { mutableStateOf(false) }

    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(seconds, currentSeriesNumber) {
        timeLeft = seconds
        isCompleted = false
        isRunning = false
    }

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

    val progress = if (seconds > 0) {
        1f - (timeLeft.toFloat() / seconds.toFloat())
    } else 0f

    val formattedTime = remember(timeLeft) {
        val minutes = timeLeft / 60
        val secs = timeLeft % 60
        String.format("%02d:%02d", minutes, secs)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = when {
            isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            timeLeft <= 3 && isRunning -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Timer Isometrico",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )

                    Text(
                        text = formattedTime,
                        color = when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            timeLeft <= 3 && isRunning -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isCompleted && isEnabled) {
                    IconButton(
                        onClick = {
                            if (timeLeft <= 0) {
                                timeLeft = seconds
                                isCompleted = false
                            }
                            isRunning = !isRunning
                        }
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar semplice
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    timeLeft <= 3 && isRunning -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.tertiary
                },
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun SmallScreenPlateauBadge(
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFF5722).copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                contentDescription = "Plateau",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Plateau Rilevato - Tocca per dettagli",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SmallScreenMiniNavigation(
    canGoPrev: Boolean,
    canGoNext: Boolean,
    currentIndex: Int,
    totalCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(50.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsante precedente
            IconButton(
                onClick = onPrevious,
                enabled = canGoPrev
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Precedente",
                    tint = if (canGoPrev) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // Indicatori mini
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(totalCount) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentIndex) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    index < currentIndex -> MaterialTheme.colorScheme.primary
                                    index == currentIndex -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )
                    )
                }
            }

            // Pulsante successivo
            IconButton(
                onClick = onNext,
                enabled = canGoNext
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Successivo",
                    tint = if (canGoNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun SmallScreenRecoveryTimer(
    seconds: Int,
    isExerciseCompleted: Boolean = false,
    hasMoreExercises: Boolean = true,
    onStop: () -> Unit
) {
    var timeLeft by remember(seconds) { mutableStateOf(seconds) }
    var timerActive by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = seconds) {
        timeLeft = seconds
        timerActive = true

        while (timeLeft > 0 && timerActive) {
            if (timeLeft <= 3 && timeLeft > 0) {
                coroutineScope.launch {
                    soundManager.playWorkoutSound(SoundManager.WorkoutSound.COUNTDOWN_BEEP)
                }
            }

            delay(1000)
            timeLeft -= 1
        }

        if (timeLeft <= 0 && timerActive) {
            coroutineScope.launch {
                soundManager.playWorkoutSound(SoundManager.WorkoutSound.REST_COMPLETE)
            }
            onStop()
        }
    }

    val formattedTime = remember(timeLeft) {
        val minutes = timeLeft / 60
        val secs = timeLeft % 60
        String.format("%02d:%02d", minutes, secs)
    }

    val (titleText, buttonText) = when {
        isExerciseCompleted && hasMoreExercises -> {
            "Prossimo Esercizio" to "Continua"
        }
        isExerciseCompleted && !hasMoreExercises -> {
            "Quasi Finito!" to "Completa"
        }
        else -> {
            "Recupero" to "Salta"
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = when {
            isExerciseCompleted && hasMoreExercises -> MaterialTheme.colorScheme.secondary
            isExerciseCompleted && !hasMoreExercises -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        isExerciseCompleted && hasMoreExercises -> Icons.AutoMirrored.Filled.NavigateNext
                        isExerciseCompleted && !hasMoreExercises -> Icons.Default.Flag
                        else -> Icons.Default.Timer
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = titleText,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedTime,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = {
                    timerActive = false
                    onStop()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    buttonText,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ======== LAYOUT MEDIUM E LARGE SCREEN ========

@Composable
private fun MediumScreenWorkoutLayout(
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
    onSaveWorkout: () -> Unit,
    onSelectExercise: (Int) -> Unit,
    onExerciseValuesChanged: (Int, Pair<Float, Int>) -> Unit,
    onDismissPlateau: (Int) -> Unit,
    onShowPlateauDetails: (PlateauInfo) -> Unit,
    onShowGroupPlateauDetails: (String, List<PlateauInfo>) -> Unit,
    onBack: () -> Unit,
    onNavigateToNext: () -> Unit = {},  // Callback per navigare al prossimo esercizio
    onWorkoutComplete: () -> Unit = {}  // Callback quando l'allenamento è completato
) {
    // Per medium screen usiamo un layout simile al large ma leggermente più compatto
    LargeScreenWorkoutLayout(
        workout = workout,
        seriesState = seriesState,
        isTimerRunning = isTimerRunning,
        recoveryTime = recoveryTime,
        currentRecoveryExerciseId = currentRecoveryExerciseId,
        currentSelectedExerciseId = currentSelectedExerciseId,
        exerciseValues = exerciseValues,
        plateauInfo = plateauInfo,
        elapsedTime = elapsedTime,
        onSeriesCompleted = onSeriesCompleted,
        onStopTimer = onStopTimer,
        onSaveWorkout = onSaveWorkout,
        onSelectExercise = onSelectExercise,
        onExerciseValuesChanged = onExerciseValuesChanged,
        onDismissPlateau = onDismissPlateau,
        onShowPlateauDetails = onShowPlateauDetails,
        onShowGroupPlateauDetails = onShowGroupPlateauDetails,
        onBack = onBack,
        onNavigateToNext = onNavigateToNext,
        onWorkoutComplete = onWorkoutComplete
    )
}

@Composable
private fun LargeScreenWorkoutLayout(
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
    onSaveWorkout: () -> Unit,
    onSelectExercise: (Int) -> Unit,
    onExerciseValuesChanged: (Int, Pair<Float, Int>) -> Unit,
    onDismissPlateau: (Int) -> Unit,
    onShowPlateauDetails: (PlateauInfo) -> Unit,
    onShowGroupPlateauDetails: (String, List<PlateauInfo>) -> Unit,
    onBack: () -> Unit,
    onNavigateToNext: () -> Unit = {},  // Callback per navigare al prossimo esercizio
    onWorkoutComplete: () -> Unit = {}  // Callback quando l'allenamento è completato
) {
    // Setup base come il layout originale
    val exerciseGroups = groupExercisesByType(workout.esercizi)
    var currentGroupIndex by remember { mutableStateOf(0) }

    val currentGroup = exerciseGroups.getOrNull(currentGroupIndex) ?: return
    val currentExercise = if (currentGroup.size > 1) {
        currentGroup.find { it.id == currentSelectedExerciseId } ?: currentGroup.first()
    } else {
        currentGroup.first()
    }

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
    var showCompleteWorkoutDialog by remember { mutableStateOf(false) }

    // Plateau info
    val exercisePlateau = plateauInfo[currentExercise.id]
    var showPlateauDialog by remember { mutableStateOf<PlateauInfo?>(null) }

    // Suoni
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // Calcola progresso generale
    val totalProgress = calculateWorkoutProgress(exerciseGroups, seriesMap)

    // Controlla se l'esercizio corrente è completato
    val isCurrentExerciseCompleted = completedSeries.size >= currentExercise.serie
    val hasMoreExercises = currentGroupIndex < exerciseGroups.size - 1

    // Controlla se TUTTI gli esercizi dell'allenamento sono completati
    val isAllWorkoutCompleted = calculateAllExercisesCompleted(exerciseGroups, seriesMap)

    // Funzione helper per auto-navigazione dopo timer
    val onRecoveryTimerComplete = {
        if (isCurrentExerciseCompleted && hasMoreExercises) {
            // Passa automaticamente al prossimo esercizio
            currentGroupIndex++
            val newGroup = exerciseGroups[currentGroupIndex]
            if (newGroup.size > 1) {
                onSelectExercise(newGroup.first().id)
            }
            onNavigateToNext()
        } else if (isCurrentExerciseCompleted && !hasMoreExercises) {
            // Allenamento completato
            onWorkoutComplete()
        }
        onStopTimer()
    }

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
            val newGroup = exerciseGroups[currentGroupIndex]
            if (newGroup.size > 1) {
                onSelectExercise(newGroup.first().id)
            }
        }
    }

    val navigateToPrevGroup = {
        if (currentGroupIndex > 0) {
            currentGroupIndex--
            val newGroup = exerciseGroups[currentGroupIndex]
            if (newGroup.size > 1) {
                onSelectExercise(newGroup.first().id)
            }
        }
    }

    // Gestione swipe
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = 100f

    // Layout originale ma ottimizzato per large screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                isAllWorkoutCompleted = isAllWorkoutCompleted,
                onBack = onBack
            )

            // Contenuto principale
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Contenuto dell'esercizio
                FullscreenExerciseContentNew(
                    exercise = currentExercise,
                    group = currentGroup,
                    completedSeries = completedSeries,
                    currentSeriesNumber = currentSeriesNumber,
                    currentWeight = currentWeight,
                    currentReps = currentReps,
                    plateau = exercisePlateau,
                    isTimerRunning = isTimerRunning,
                    isAllWorkoutCompleted = isAllWorkoutCompleted,
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
                        if (isAllWorkoutCompleted) {
                            showCompleteWorkoutDialog = true
                        } else {
                            onSeriesCompleted(
                                currentExercise.id,
                                currentWeight,
                                currentReps,
                                currentSeriesNumber
                            )

                            coroutineScope.launch {
                                soundManager.playWorkoutSound(SoundManager.WorkoutSound.SERIES_COMPLETE)
                            }
                        }
                    }
                )

                // Navigation bar completa in fondo
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
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
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
                isExerciseCompleted = isCurrentExerciseCompleted,
                hasMoreExercises = hasMoreExercises,
                onStop = onRecoveryTimerComplete
            )
        }
    }

    // Dialoghi (uguale per tutti gli screen size)
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

// ======== COMPONENTI CONDIVISI PER LARGE/MEDIUM SCREEN ========

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
    isAllWorkoutCompleted: Boolean = false,
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
    exercise.setType == "circuit"
    val isIsometric = exercise.isIsometric

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Nome dell'esercizio con badge integrato - ULTRA COMPATTO
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge integrato con il nome se è in un gruppo
            if (isInGroup) {
                Text(
                    text = "${if (isSuperset) "Superset" else "Circuit"}: ${exercise.nome}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = exercise.nome,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
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

        // CONTROLLI PESO E RIPETIZIONI/SECONDI - ULTRA COMPATTI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                currentSeriesNumber = currentSeriesNumber,
                onTimerComplete = onCompleteSeries,
                isEnabled = completedSeries.size < exercise.serie,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Spazio flessibile ma limitato
        Spacer(modifier = Modifier.weight(1f, fill = false))

        // PULSANTE COMPLETA SERIE - PIÙ COMPATTO
        WorkoutCompleteButton(
            isWorkoutCompleted = isAllWorkoutCompleted,
            currentSeriesNumber = currentSeriesNumber,
            isEnabled = completedSeries.size < exercise.serie && !isTimerRunning,
            onClick = onCompleteSeries,
            modifier = Modifier.fillMaxWidth()
        )

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
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        exercises.forEach { exercise ->
            val isSelected = exercise.id == selectedExerciseId
            val shortName = truncateExerciseName(exercise.nome, 10)

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExerciseSelect(exercise.id) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    accentColor.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                border = if (isSelected) {
                    BorderStroke(1.dp, accentColor)
                } else null
            ) {
                Text(
                    text = shortName,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * COMPATTO: Card per il progresso delle serie - RIDOTTA ULTERIORMENTE
 */
@Composable
private fun SeriesProgressCardCompact(
    currentSeries: Int,
    totalSeries: Int,
    completedSeries: Int
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Serie $currentSeries",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$completedSeries/$totalSeries",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Indicatori serie circolari compatti
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(totalSeries) { index ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
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
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
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
    currentSeriesNumber: Int,
    onTimerComplete: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var timeLeft by remember(seconds, currentSeriesNumber) { mutableStateOf(seconds) }
    var isRunning by remember(currentSeriesNumber) { mutableStateOf(false) }
    var isCompleted by remember(currentSeriesNumber) { mutableStateOf(false) }

    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // Reset quando cambiano i secondi O il numero della serie
    LaunchedEffect(seconds, currentSeriesNumber) {
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
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Timer Isometrico",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )

                Text(
                    text = formattedTime,
                    color = when {
                        isCompleted -> MaterialTheme.colorScheme.primary
                        timeLeft <= 3 && isRunning -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 24.sp,
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
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completato",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                contentDescription = "Plateau",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = "Plateau",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun FullscreenWorkoutHeader(
    currentGroupIndex: Int,
    totalGroups: Int,
    totalProgress: Float,
    elapsedTime: String,
    isAllWorkoutCompleted: Boolean = false,
    onBack: () -> Unit,
    onCompleteWorkout: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Esercizio ${currentGroupIndex + 1} di $totalGroups",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                if (isAllWorkoutCompleted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "🎉 Completato!",
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { totalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isAllWorkoutCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        }
    }
}

/**
 * Navigation bar con padding ridotto per stare più in basso
 */
@Composable
private fun FullscreenNavigationBar(
    canGoPrev: Boolean,
    canGoNext: Boolean,
    currentIndex: Int,
    totalCount: Int,
    exerciseGroups: List<List<WorkoutExercise>>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJumpTo: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Precedente",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Prec", fontSize = 14.sp)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(totalCount) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentIndex) 10.dp else 6.dp)
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Succ", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Successivo",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FullscreenRecoveryTimer(
    seconds: Int,
    isExerciseCompleted: Boolean = false,
    hasMoreExercises: Boolean = true,
    onStop: () -> Unit
) {
    var timeLeft by remember(seconds) { mutableStateOf(seconds) }
    var timerActive by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = seconds) {
        timeLeft = seconds
        timerActive = true

        while (timeLeft > 0 && timerActive) {
            if (timeLeft <= 3 && timeLeft > 0) {
                coroutineScope.launch {
                    soundManager.playWorkoutSound(SoundManager.WorkoutSound.COUNTDOWN_BEEP)
                }
            }

            delay(1000)
            timeLeft -= 1
        }

        if (timeLeft <= 0 && timerActive) {
            coroutineScope.launch {
                soundManager.playWorkoutSound(SoundManager.WorkoutSound.REST_COMPLETE)
            }
            // Auto-navigazione gestita dal callback onStop
            onStop()
        }
    }

    val formattedTime = remember(timeLeft) {
        val minutes = timeLeft / 60
        val secs = timeLeft % 60
        String.format("%02d:%02d", minutes, secs)
    }

    // Messaggi dinamici basati sullo stato
    val (titleText, descriptionText) = when {
        isExerciseCompleted && hasMoreExercises -> {
            "Esercizio Completato!" to "Passaggio al prossimo esercizio..."
        }
        isExerciseCompleted && !hasMoreExercises -> {
            "Allenamento Quasi Finito!" to "Preparati per il completamento..."
        }
        else -> {
            "Recupero" to "Riposa tra le serie"
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when {
            isExerciseCompleted && hasMoreExercises -> MaterialTheme.colorScheme.secondary
            isExerciseCompleted && !hasMoreExercises -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
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
                    imageVector = when {
                        isExerciseCompleted && hasMoreExercises -> Icons.AutoMirrored.Filled.NavigateNext
                        isExerciseCompleted && !hasMoreExercises -> Icons.Default.Flag
                        else -> Icons.Default.Timer
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = titleText,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = descriptionText,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        fontSize = 12.sp
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
                    timerActive = false
                    onStop()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = if (isExerciseCompleted) "Continua" else "Salta",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// ======== FUNZIONI HELPER ========

/**
 * Calcola se tutti gli esercizi dell'allenamento sono completati
 */
private fun calculateAllExercisesCompleted(
    exerciseGroups: List<List<WorkoutExercise>>,
    seriesMap: Map<Int, List<CompletedSeries>>
): Boolean {
    return exerciseGroups.all { group ->
        group.all { exercise ->
            val completedSeries = seriesMap[exercise.id] ?: emptyList()
            completedSeries.size >= exercise.serie
        }
    }
}

/**
 * Pulsante intelligente che lampeggia quando l'allenamento è completato
 */
@Composable
private fun WorkoutCompleteButton(
    isWorkoutCompleted: Boolean,
    currentSeriesNumber: Int,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animazione lampeggiante solo quando l'allenamento è completato
    val blinkAnimation by animateFloatAsState(
        targetValue = if (isWorkoutCompleted && isEnabled) 1f else 0f,
        animationSpec = if (isWorkoutCompleted && isEnabled) {
            infiniteRepeatable(
                animation = tween(600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(300)
        },
        label = "blink"
    )

    val buttonColor = when {
        isWorkoutCompleted && isEnabled -> {
            lerp(
                Color(0xFF4CAF50), // Verde brillante
                Color(0xFF81C784), // Verde più chiaro
                blinkAnimation
            )
        }
        else -> MaterialTheme.colorScheme.primary
    }

    val buttonText = when {
        isWorkoutCompleted -> "🏁 Completa Allenamento!"
        else -> "Completa Serie $currentSeriesNumber"
    }

    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier.height(if (isWorkoutCompleted) 56.dp else 52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = buttonText,
            fontSize = if (isWorkoutCompleted) 18.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Dialog di conferma per completare l'allenamento
 */
@Composable
private fun CompleteWorkoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Completa l'allenamento?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "🎉 Fantastico! Hai completato tutti gli esercizi!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Vuoi finalizzare l'allenamento? Potrai visualizzarlo nello storico e vedere i tuoi progressi.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Completa", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Continua")
            }
        }
    )
}

private fun truncateExerciseName(name: String, maxLength: Int = 12): String {
    return if (name.length <= maxLength) {
        name
    } else {
        name.take(maxLength - 2) + ".."
    }
}

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