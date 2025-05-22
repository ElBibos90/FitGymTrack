package com.fitgymtrack.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.*
import com.fitgymtrack.app.ui.components.*
import com.fitgymtrack.app.ui.theme.BluePrimary
import com.fitgymtrack.app.ui.theme.PurplePrimary
import com.fitgymtrack.app.utils.PlateauInfo
import com.fitgymtrack.app.viewmodel.ActiveWorkoutViewModel
import kotlinx.coroutines.launch
import android.util.Log

private fun isCircuit(exercise: WorkoutExercise): Boolean {
    return exercise.setType == "circuit"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    schedaId: Int,
    userId: Int,
    onNavigateBack: () -> Unit,
    onWorkoutCompleted: () -> Unit,
    viewModel: ActiveWorkoutViewModel = viewModel()
) {
    val workoutState by viewModel.workoutState.collectAsState()
    val seriesState by viewModel.seriesState.collectAsState()
    val saveSeriesState by viewModel.saveSeriesState.collectAsState()
    val completeWorkoutState by viewModel.completeWorkoutState.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val workoutCompleted by viewModel.workoutCompleted.collectAsState()
    val recoveryTime by viewModel.recoveryTime.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val currentRecoveryExerciseId by viewModel.currentRecoveryExerciseId.collectAsState()

    // Osserva l'esercizio selezionato per superset/circuit
    val currentSelectedExerciseId by viewModel.currentSelectedExerciseId.collectAsState()

    // Mappa dei valori di peso e ripetizioni per esercizio
    val exerciseValues by viewModel.exerciseValues.collectAsState()

    // NUOVO: Stati per il plateau
    val plateauInfo by viewModel.plateauInfo.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showCompleteWorkoutDialog by remember { mutableStateOf(false) }

    // NUOVO: Dialog per dettagli plateau
    var showPlateauDetailDialog by remember { mutableStateOf<PlateauInfo?>(null) }

    // NUOVO: Dialog per plateau di gruppo
    var showGroupPlateauDialog by remember { mutableStateOf<Pair<String, List<PlateauInfo>>?>(null) }

    // Stato per tenere traccia della modalità di visualizzazione
    var useModernUI by remember { mutableStateOf(true) }

    // Stato per tenere traccia dei gruppi espansi nella visualizzazione moderna
    val expandedModernGroups = remember { mutableStateMapOf<Int, Boolean>() }

    // Gestisce la navigazione indietro
    BackHandler(workoutCompleted) {
        onWorkoutCompleted()
    }

    // Gestisce il completamento dell'allenamento
    LaunchedEffect(completeWorkoutState) {
        if (completeWorkoutState is CompleteWorkoutState.Success) {
            if (!workoutCompleted) {
                snackbarHostState.showSnackbar(
                    message = "Allenamento salvato con successo!",
                    duration = SnackbarDuration.Short
                )
            }
        } else if (completeWorkoutState is CompleteWorkoutState.Error) {
            snackbarHostState.showSnackbar(
                message = (completeWorkoutState as CompleteWorkoutState.Error).message,
                duration = SnackbarDuration.Long
            )
        }
    }

    // Gestisce il pulsante indietro del dispositivo
    BackHandler {
        showExitConfirmDialog = true
    }

    // Carica l'allenamento all'avvio
    LaunchedEffect(schedaId, userId) {
        viewModel.initializeWorkout(userId, schedaId)
    }

    // Gestisce gli errori del salvataggio delle serie
    LaunchedEffect(saveSeriesState) {
        if (saveSeriesState is SaveSeriesState.Error) {
            snackbarHostState.showSnackbar(
                message = (saveSeriesState as SaveSeriesState.Error).message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (workoutState) {
                                is ActiveWorkoutState.Success -> {
                                    "Allenamento"
                                }
                                else -> "Allenamento"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        val elapsedTimeFormatted by remember(elapsedTime) {
                            derivedStateOf { viewModel.getFormattedElapsedTime() }
                        }
                        Text(
                            text = "Durata: $elapsedTimeFormatted",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                },
                actions = {
                    // NUOVO: Bottone di debug per testare plateau (solo in debug)
                    IconButton(onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Test: Forzando controllo plateau...")
                        }
                        // Forza il re-check dei plateau
                        viewModel.resetDismissedPlateaus()
                        viewModel.forceCheckPlateaus()
                    }) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Test Plateau"
                        )
                    }

                    IconButton(onClick = { useModernUI = !useModernUI }) {
                        Icon(
                            imageVector = if (useModernUI) Icons.Default.ViewList else Icons.Default.ViewModule,
                            contentDescription = "Cambia visualizzazione"
                        )
                    }

                    if (!workoutCompleted) {
                        IconButton(onClick = { showCompleteWorkoutDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completa allenamento"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Contenuto principale
            when (workoutState) {
                is ActiveWorkoutState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Caricamento allenamento...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                is ActiveWorkoutState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Errore",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = (workoutState as ActiveWorkoutState.Error).message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.initializeWorkout(userId, schedaId) }
                        ) {
                            Text("Riprova")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = onNavigateBack
                        ) {
                            Text("Torna indietro")
                        }
                    }
                }

                is ActiveWorkoutState.Success -> {
                    val workout = (workoutState as ActiveWorkoutState.Success).workout

                    if (workoutCompleted) {
                        WorkoutSuccessScreen(
                            totalSeries = calculateTotalSeries(seriesState),
                            totalWeight = calculateTotalWeight(seriesState).toInt(),
                            onBackToHome = {
                                onWorkoutCompleted()
                            }
                        )
                    } else {
                        if (useModernUI) {
                            ModernActiveWorkoutContent(
                                workout = workout,
                                seriesState = seriesState,
                                isTimerRunning = isTimerRunning,
                                recoveryTime = recoveryTime,
                                currentRecoveryExerciseId = currentRecoveryExerciseId,
                                currentSelectedExerciseId = currentSelectedExerciseId,
                                exerciseValues = exerciseValues,
                                plateauInfo = plateauInfo, // NUOVO
                                onSeriesCompleted = { exerciseId, weight, reps, serieNumber ->
                                    Log.d("ActiveWorkout", "Completamento serie: exerciseId=$exerciseId, weight=$weight, reps=$reps, series=$serieNumber")
                                    viewModel.addCompletedSeries(exerciseId, weight, reps, serieNumber)
                                },
                                onStopTimer = {
                                    viewModel.stopRecoveryTimer()
                                },
                                onSaveWorkout = {
                                    showCompleteWorkoutDialog = true
                                },
                                onSelectExercise = { exerciseId ->
                                    Log.d("ActiveWorkout", "Selezionato esercizio: $exerciseId")
                                    viewModel.selectExercise(exerciseId)
                                },
                                expandedGroups = expandedModernGroups,
                                onExerciseValuesChanged = { exerciseId, values ->
                                    Log.d("ActiveWorkout", "Valori esercizio modificati: exerciseId=$exerciseId, weight=${values.first}, reps=${values.second}")
                                    viewModel.saveExerciseValues(exerciseId, values.first, values.second)
                                },
                                // NUOVO: Callbacks per plateau
                                onDismissPlateau = { exerciseId ->
                                    viewModel.dismissPlateau(exerciseId)
                                },
                                onShowPlateauDetails = { plateau ->
                                    showPlateauDetailDialog = plateau
                                },
                                onShowGroupPlateauDetails = { groupTitle, plateauList ->
                                    showGroupPlateauDialog = Pair(groupTitle, plateauList)
                                }
                            )
                        } else {
                            Text(
                                text = "Modalità classica non disponibile in questa versione",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    // Stato Idle, non mostrare nulla
                }
            }

            // Timer di recupero
            AnimatedVisibility(
                visible = recoveryTime > 0 && isTimerRunning,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                RecoveryTimer(
                    seconds = recoveryTime,
                    isRunning = isTimerRunning,
                    onFinish = {},
                    onStop = { viewModel.stopRecoveryTimer() }
                )
            }
        }

        // Dialog di conferma uscita
        if (showExitConfirmDialog) {
            ExitWorkoutDialog(
                onDismiss = { showExitConfirmDialog = false },
                onConfirm = {
                    viewModel.cancelWorkout()
                    onNavigateBack()
                }
            )
        }

        // Dialog di completamento allenamento
        if (showCompleteWorkoutDialog) {
            CompleteWorkoutDialog(
                onDismiss = { showCompleteWorkoutDialog = false },
                onConfirm = {
                    viewModel.completeWorkout()
                    viewModel.markWorkoutAsCompleted()
                    showCompleteWorkoutDialog = false
                }
            )
        }

        // NUOVO: Dialog dettagli plateau
        showPlateauDetailDialog?.let { plateau ->
            PlateauDetailDialog(
                plateauInfo = plateau,
                onDismiss = { showPlateauDetailDialog = null }
            )
        }

        // NUOVO: Dialog plateau di gruppo
        showGroupPlateauDialog?.let { (groupTitle, plateauList) ->
            GroupPlateauDialog(
                groupTitle = groupTitle,
                plateauList = plateauList,
                onDismiss = { showGroupPlateauDialog = null }
            )
        }
    }
}

@Composable
private fun ModernActiveWorkoutContent(
    workout: ActiveWorkout,
    seriesState: CompletedSeriesState,
    isTimerRunning: Boolean,
    recoveryTime: Int,
    currentRecoveryExerciseId: Int?,
    currentSelectedExerciseId: Int?,
    exerciseValues: Map<Int, Pair<Float, Int>>,
    plateauInfo: Map<Int, PlateauInfo>, // NUOVO
    onSeriesCompleted: (Int, Float, Int, Int) -> Unit,
    onStopTimer: () -> Unit,
    onSaveWorkout: () -> Unit = {},
    onSelectExercise: (Int) -> Unit = {},
    expandedGroups: MutableMap<Int, Boolean> = remember { mutableStateMapOf() },
    onExerciseValuesChanged: (Int, Pair<Float, Int>) -> Unit = { _, _ -> },
    // NUOVI callbacks per plateau
    onDismissPlateau: (Int) -> Unit = {},
    onShowPlateauDetails: (PlateauInfo) -> Unit = {},
    onShowGroupPlateauDetails: (String, List<PlateauInfo>) -> Unit = { _, _ -> }
) {
    Log.d("ActiveWorkout", "ModernActiveWorkoutContent: numero esercizi=${workout.esercizi.size}")

    val seriesMap = when (seriesState) {
        is CompletedSeriesState.Success -> seriesState.series
        else -> emptyMap()
    }

    // Raggruppa gli esercizi in base a setType e linkedToPrevious
    val exerciseGroups = groupExercisesByType(workout.esercizi)

    Log.d("ActiveWorkout", "Gruppi di esercizi trovati: ${exerciseGroups.size}")
    exerciseGroups.forEachIndexed { index, group ->
        Log.d("ActiveWorkout", "Gruppo $index: ${group.size} esercizi, tipo=${group.firstOrNull()?.setType ?: "N/A"}")
    }

    // Calcola gli esercizi attivi e completati
    val completedGroups = exerciseGroups.filter { group ->
        group.all { exercise ->
            val completedSeries = seriesMap[exercise.id] ?: emptyList()
            completedSeries.size >= exercise.serie
        }
    }

    val activeGroups = exerciseGroups.filter { group ->
        group.any { exercise ->
            val completedSeries = seriesMap[exercise.id] ?: emptyList()
            completedSeries.size < exercise.serie
        }
    }

    // Calcola il progresso
    val progress = if (workout.esercizi.isNotEmpty()) {
        val completedExercises = workout.esercizi.count { exercise ->
            val completedSeries = seriesMap[exercise.id] ?: emptyList()
            completedSeries.size >= exercise.serie
        }
        completedExercises.toFloat() / workout.esercizi.size.toFloat()
    } else {
        0f
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        state = rememberLazyListState(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WorkoutProgressIndicator(
                activeExercises = activeGroups.sumOf { it.size },
                completedExercises = completedGroups.sumOf { it.size },
                totalExercises = workout.esercizi.size,
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (activeGroups.isNotEmpty()) {
            item {
                Text(
                    text = "Esercizi da completare",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            itemsIndexed(activeGroups) { index, group ->
                val isSuperset = group.size > 1 && isSuperset(group.first())
                val isCircuit = group.size > 1 && isCircuit(group.first())

                if (isSuperset || isCircuit) {
                    val selectedExerciseInGroup = if (currentSelectedExerciseId != null && group.any { it.id == currentSelectedExerciseId }) {
                        group.first { it.id == currentSelectedExerciseId }
                    } else {
                        group.first()
                    }

                    val isGroupExpanded = expandedGroups[index] ?: false
                    val groupTitle = if (isSuperset) "Superset ${index + 1}" else "Circuit ${index + 1}"
                    val groupColor = if (isSuperset) BluePrimary else PurplePrimary

                    // NUOVO: Verifica se il gruppo contiene plateau
                    val hasPlateauInGroup = group.any { plateauInfo.containsKey(it.id) }

                    Log.d("ActiveWorkout", "Rendering ${if (isSuperset) "superset" else "circuit"} gruppo $index: expanded=$isGroupExpanded, " +
                            "exerciseId=${selectedExerciseInGroup.id}, nome=${selectedExerciseInGroup.nome}")

                    Box {
                        SupersetGroupCard(
                            title = groupTitle,
                            exercises = group,
                            selectedExerciseId = selectedExerciseInGroup.id,
                            serieCompletate = seriesMap,
                            onExerciseSelected = { onSelectExercise(it) },
                            onAddSeries = { exerciseId, weight, reps, serieNumber ->
                                Log.d("ActiveWorkout", "AddSeries da gruppo: exerciseId=$exerciseId, weight=$weight, reps=$reps, series=$serieNumber")
                                onSeriesCompleted(exerciseId, weight, reps, serieNumber)
                            },
                            isTimerRunning = isTimerRunning,
                            exerciseValues = exerciseValues,
                            isExpanded = isGroupExpanded,
                            onExpandToggle = {
                                Log.d("ActiveWorkout", "Toggle espansione gruppo $index: ${!isGroupExpanded}")
                                expandedGroups[index] = !isGroupExpanded
                            },
                            accentColor = groupColor
                        )

                        // NUOVO: Badge plateau se presente nel gruppo
                        if (hasPlateauInGroup) {
                            PlateauBadge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                onClick = {
                                    // Raccogli TUTTI i plateau del gruppo
                                    val groupPlateaus = group.mapNotNull { exercise ->
                                        plateauInfo[exercise.id]
                                    }

                                    if (groupPlateaus.isNotEmpty()) {
                                        // Se ci sono più plateau, mostra il dialog di gruppo
                                        if (groupPlateaus.size > 1) {
                                            onShowGroupPlateauDetails(groupTitle, groupPlateaus)
                                        } else {
                                            // Se c'è solo un plateau, mostra il dialog singolo
                                            onShowPlateauDetails(groupPlateaus.first())
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // Esercizio singolo
                    val exercise = group.first()
                    val completedSeries = seriesMap[exercise.id] ?: emptyList()
                    val values = exerciseValues[exercise.id]
                    val exercisePlateau = plateauInfo[exercise.id]

                    Log.d("ActiveWorkout", "Rendering esercizio singolo: id=${exercise.id}, " +
                            "nome=${exercise.nome}, completedSeries=${completedSeries.size}/${exercise.serie}")

                    Box {
                        ExerciseProgressItem(
                            exercise = exercise,
                            completedSeries = completedSeries,
                            isTimerRunning = isTimerRunning && (currentRecoveryExerciseId == exercise.id || currentRecoveryExerciseId == null),
                            onAddSeries = { weight, reps ->
                                Log.d("ActiveWorkout", "AddSeries da elemento singolo: exerciseId=${exercise.id}, weight=$weight, reps=$reps")
                                onSeriesCompleted(
                                    exercise.id,
                                    weight,
                                    reps,
                                    completedSeries.size + 1
                                )
                            },
                            isLastExercise = false,
                            isCompleted = false,
                            initialWeight = values?.first,
                            initialReps = values?.second
                        )

                        // NUOVO: Badge plateau se presente
                        if (exercisePlateau != null) {
                            PlateauBadge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                onClick = { onShowPlateauDetails(exercisePlateau) }
                            )
                        }
                    }
                }
            }
        }

        // Esercizi completati
        if (completedGroups.isNotEmpty()) {
            item {
                Text(
                    text = "Esercizi completati",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }

            itemsIndexed(completedGroups) { index, group ->
                if (group.size > 1 && isSuperset(group.first())) {
                    Log.d("ActiveWorkout", "Rendering superset completato $index: ${group.size} esercizi")

                    CompletedSupersetCard(
                        title = "Superset ${index + 1}",
                        exercises = group,
                        serieCompletate = seriesMap
                    )
                } else {
                    val exercise = group.first()
                    val completedSeries = seriesMap[exercise.id] ?: emptyList()

                    Log.d("ActiveWorkout", "Rendering esercizio completato: id=${exercise.id}, " +
                            "nome=${exercise.nome}, completedSeries=${completedSeries.size}/${exercise.serie}")

                    ExerciseProgressItem(
                        exercise = exercise,
                        completedSeries = completedSeries,
                        isTimerRunning = false,
                        onAddSeries = { _, _ -> },
                        isCompleted = true
                    )
                }
            }
        }

        // Spazio extra in fondo per far posto al timer di recupero
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// Funzione di supporto per calcolare il numero totale di serie completate
private fun calculateTotalSeries(seriesState: CompletedSeriesState): Int {
    return when (seriesState) {
        is CompletedSeriesState.Success -> {
            seriesState.series.values.sumOf { it.size }
        }
        else -> 0
    }
}

// Funzione di supporto per calcolare il peso totale sollevato
private fun calculateTotalWeight(seriesState: CompletedSeriesState): Float {
    return when (seriesState) {
        is CompletedSeriesState.Success -> {
            var totalWeight = 0f
            seriesState.series.values.forEach { seriesList ->
                seriesList.forEach { series ->
                    totalWeight += series.peso
                }
            }
            totalWeight
        }
        else -> 0f
    }
}

/**
 * Verifica se un esercizio è parte di un superset
 */
private fun isSuperset(exercise: WorkoutExercise): Boolean {
    return exercise.setType == "superset" || exercise.setType == "1"
}

/**
 * Raggruppa gli esercizi in base al tipo di set e alla connessione
 */
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