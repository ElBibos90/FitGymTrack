package com.fitgymtrack.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.fitgymtrack.app.viewmodel.ActiveWorkoutViewModel
import kotlinx.coroutines.launch

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

    // NUOVO: Osserva l'esercizio selezionato per superset/circuit
    val currentSelectedExerciseId by viewModel.currentSelectedExerciseId.collectAsState()

    // NUOVO: Mappa dei valori di peso e ripetizioni per esercizio
    val exerciseValues by viewModel.exerciseValues.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showCompleteWorkoutDialog by remember { mutableStateOf(false) }

    // Stato per tenere traccia della modalità di visualizzazione
    // true = visualizzazione moderna (nuova UI come nelle immagini)
    // false = visualizzazione classica (UI esistente)
    var useModernUI by remember { mutableStateOf(true) }

    // Stato per tenere traccia dei gruppi espansi nella visualizzazione moderna
    val expandedModernGroups = remember { mutableStateMapOf<Int, Boolean>() }

    // Gestisce la navigazione indietro
    BackHandler(workoutCompleted) {
        // Se siamo nella schermata di riepilogo, torniamo alla home
        onWorkoutCompleted()
    }

    // Gestisce il completamento dell'allenamento
    LaunchedEffect(completeWorkoutState) {
        if (completeWorkoutState is CompleteWorkoutState.Success) {
            // Se l'allenamento è già stato salvato, non mostrare più snackbar
            if (!workoutCompleted) {
                // Mostra un messaggio di successo
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
                        // Utilizza il nome della scheda invece del nome dell'esercizio
                        Text(
                            text = when (workoutState) {
                                is ActiveWorkoutState.Success -> {
                                    // Mostra il nome della scheda qui
                                    "Allenamento" // Questo dovrebbe essere sostituito con il nome effettivo della scheda
                                }
                                else -> "Allenamento"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Durata: ${viewModel.getFormattedElapsedTime()}",
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
                    // Aggiungiamo un toggle per cambiare la visualizzazione
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
                    LoadingScreen()
                }

                is ActiveWorkoutState.Error -> {
                    ErrorScreen(
                        message = (workoutState as ActiveWorkoutState.Error).message,
                        onRetry = { viewModel.initializeWorkout(userId, schedaId) },
                        onBack = onNavigateBack
                    )
                }

                is ActiveWorkoutState.Success -> {
                    val workout = (workoutState as ActiveWorkoutState.Success).workout

                    if (workoutCompleted) {
                        // Utilizziamo la nuova schermata di successo
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
                                onSeriesCompleted = { exerciseId, weight, reps, serieNumber ->
                                    viewModel.addCompletedSeries(exerciseId, weight, reps, serieNumber)
                                },
                                onStopTimer = {
                                    viewModel.stopRecoveryTimer()
                                },
                                onSaveWorkout = {
                                    showCompleteWorkoutDialog = true
                                },
                                onSelectExercise = { exerciseId ->
                                    viewModel.selectExercise(exerciseId)
                                },
                                expandedGroups = expandedModernGroups
                            )
                        } else {
                            ActiveWorkoutContent(
                                workout = workout,
                                seriesState = seriesState,
                                isTimerRunning = isTimerRunning,
                                recoveryTime = recoveryTime,
                                currentRecoveryExerciseId = currentRecoveryExerciseId,
                                currentSelectedExerciseId = currentSelectedExerciseId,
                                exerciseValues = exerciseValues,
                                onSeriesCompleted = { exerciseId, weight, reps, serieNumber ->
                                    viewModel.addCompletedSeries(exerciseId, weight, reps, serieNumber)
                                },
                                onStopTimer = {
                                    viewModel.stopRecoveryTimer()
                                },
                                onSelectExercise = { exerciseId ->
                                    viewModel.selectExercise(exerciseId)
                                }
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
                    viewModel.markWorkoutAsCompleted()
                    showCompleteWorkoutDialog = false
                }
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
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

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
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
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry
        ) {
            Text("Riprova")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onBack
        ) {
            Text("Torna indietro")
        }
    }
}

/**
 * Nuova visualizzazione dell'allenamento con UI moderna e supporto per navigazione superset
 */
@Composable
private fun ModernActiveWorkoutContent(
    workout: ActiveWorkout,
    seriesState: CompletedSeriesState,
    isTimerRunning: Boolean,
    recoveryTime: Int,
    currentRecoveryExerciseId: Int?,
    currentSelectedExerciseId: Int?,
    exerciseValues: Map<Int, Pair<Float, Int>>,
    onSeriesCompleted: (Int, Float, Int, Int) -> Unit,
    onStopTimer: () -> Unit,
    onSaveWorkout: () -> Unit = {},
    onSelectExercise: (Int) -> Unit = {},
    expandedGroups: MutableMap<Int, Boolean> = remember { mutableStateMapOf() }
) {
    val seriesMap = when (seriesState) {
        is CompletedSeriesState.Success -> seriesState.series
        else -> emptyMap()
    }

    // Raggruppa gli esercizi in base a setType e linkedToPrevious
    val exerciseGroups = groupExercisesByType(workout.esercizi)

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
        state = rememberLazyListState()
    ) {
        item {
            WorkoutProgressIndicator(
                activeExercises = activeGroups.sumOf { it.size },
                completedExercises = completedGroups.sumOf { it.size },
                totalExercises = workout.esercizi.size,
                progress = progress,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        // Esercizi attivi
        if (activeGroups.isNotEmpty()) {
            item {
                Text(
                    text = "Esercizi da completare",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            itemsIndexed(activeGroups) { index, group ->
                // Se il gruppo ha più di un esercizio e il primo è di tipo "superset", trattalo come un superset
                if (group.size > 1 && isSuperset(group.first())) {
                    // Trova l'esercizio da visualizzare nel superset
                    val selectedExerciseInGroup = if (currentSelectedExerciseId != null && group.any { it.id == currentSelectedExerciseId }) {
                        // Se c'è un esercizio selezionato nel gruppo corrente, mostra quello
                        group.first { it.id == currentSelectedExerciseId }
                    } else {
                        // Altrimenti mostra il primo esercizio del gruppo
                        group.first()
                    }

                    // Gestisci lo stato di espansione per questo gruppo specifico
                    val isGroupExpanded = expandedGroups[index] ?: false

                    SupersetGroupCard(
                        title = "Superset ${index + 1}",
                        exercises = group,
                        selectedExerciseId = selectedExerciseInGroup.id,
                        serieCompletate = seriesMap,
                        onExerciseSelected = { onSelectExercise(it) },
                        onAddSeries = { exerciseId, weight, reps, serieNumber ->
                            onSeriesCompleted(exerciseId, weight, reps, serieNumber)
                        },
                        isTimerRunning = isTimerRunning,
                        exerciseValues = exerciseValues,
                        // Passa lo stato di espansione e la callback per aggiornarlo
                        isExpanded = isGroupExpanded,
                        onExpandToggle = {
                            expandedGroups[index] = !isGroupExpanded
                        }
                    )
                } else {
                    // Esercizio singolo o altro tipo
                    val exercise = group.first()
                    val completedSeries = seriesMap[exercise.id] ?: emptyList()
                    val values = exerciseValues[exercise.id]

                    ExerciseProgressItem(
                        exercise = exercise,
                        completedSeries = completedSeries,
                        isTimerRunning = isTimerRunning && (currentRecoveryExerciseId == exercise.id || currentRecoveryExerciseId == null),
                        onAddSeries = { weight, reps ->
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
                    // Superset completato
                    CompletedSupersetCard(
                        title = "Superset ${index + 1}",
                        exercises = group,
                        serieCompletate = seriesMap
                    )
                } else {
                    // Esercizio singolo completato
                    val exercise = group.first()
                    val completedSeries = seriesMap[exercise.id] ?: emptyList()

                    ExerciseProgressItem(
                        exercise = exercise,
                        completedSeries = completedSeries,
                        isTimerRunning = false,
                        onAddSeries = { _, _ -> /* Non dovrebbe essere chiamato */ },
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

/**
 * Visualizzazione dell'allenamento classica con supporto per navigazione superset
 */
@Composable
private fun ActiveWorkoutContent(
    workout: ActiveWorkout,
    seriesState: CompletedSeriesState,
    isTimerRunning: Boolean,
    recoveryTime: Int,
    currentRecoveryExerciseId: Int?,
    currentSelectedExerciseId: Int?,
    exerciseValues: Map<Int, Pair<Float, Int>>,
    onSeriesCompleted: (Int, Float, Int, Int) -> Unit,
    onStopTimer: () -> Unit,
    onSelectExercise: (Int) -> Unit = {}
) {
    // Add this to store expansion states of groups
    val expandedGroups = remember { mutableStateMapOf<Int, Boolean>() }
    val coroutineScope = rememberCoroutineScope()

    val seriesMap = when (seriesState) {
        is CompletedSeriesState.Success -> seriesState.series
        else -> emptyMap()
    }

    // Raggruppa gli esercizi in base a setType e linkedToPrevious
    val exerciseGroups = groupExercisesByType(workout.esercizi)

    // Initialize expansion state for new groups
    LaunchedEffect(exerciseGroups) {
        exerciseGroups.forEachIndexed { index, group ->
            if (!expandedGroups.containsKey(index)) {
                // Default to collapsed
                expandedGroups[index] = false
            }
        }
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
        // Contiamo gli esercizi completati, non i gruppi
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
        state = rememberLazyListState() // Use a remembered state to maintain scroll position
    ) {
        item {
            WorkoutProgressIndicator(
                activeExercises = activeGroups.sumOf { it.size },
                completedExercises = completedGroups.sumOf { it.size },
                totalExercises = workout.esercizi.size,
                progress = progress,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        // Esercizi attivi
        if (activeGroups.isNotEmpty()) {
            item {
                Text(
                    text = "Esercizi da completare",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            itemsIndexed(activeGroups) { index, group ->
                // Modified to use the stored expansion state
                val isExpanded = expandedGroups[index] ?: false

                if (group.size > 1 && isSuperset(group.first())) {
                    // Se il gruppo è un superset
                    val isInSuperset = true

                    // Trova l'esercizio selezionato nel superset
                    val selectedExercise = if (currentSelectedExerciseId != null && group.any { it.id == currentSelectedExerciseId }) {
                        group.first { it.id == currentSelectedExerciseId }
                    } else {
                        group.first()
                    }

                    // Ottieni lo stato di espansione per questo gruppo
                    val isExpanded = expandedGroups[index] ?: false

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SupersetCard(
                            exercises = group,
                            selectedExerciseId = selectedExercise.id,
                            serieCompletate = seriesMap,
                            isTimerRunning = isTimerRunning,
                            onExerciseSelect = { onSelectExercise(it) },
                            onAddSeries = { exerciseId, weight, reps, serieNumber ->
                                onSeriesCompleted(exerciseId, weight, reps, serieNumber)
                            },
                            isExpanded = isExpanded,
                            onExpandToggle = { expandedGroups[index] = !isExpanded },
                            exerciseValues = exerciseValues
                        )
                    }
                } else {
                    // Esercizio singolo
                    val exercise = group.first()
                    val completedSeries = seriesMap[exercise.id] ?: emptyList()
                    val values = exerciseValues[exercise.id]

                    ExerciseProgressItem(
                        exercise = exercise,
                        completedSeries = completedSeries,
                        isTimerRunning = isTimerRunning && (currentRecoveryExerciseId == exercise.id || currentRecoveryExerciseId == null),
                        onAddSeries = { weight, reps ->
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
                // Get the appropriate index for the completed groups
                val groupIndex = activeGroups.size + index
                val isExpanded = expandedGroups[groupIndex] ?: false

                if (group.size > 1 && isSuperset(group.first())) {
                    // Gruppo di esercizi (superset) completato
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CompletedSupersetCard(
                            title = "Superset ${index + 1}",
                            exercises = group,
                            serieCompletate = seriesMap,
                            isExpanded = isExpanded,
                            onExpandToggle = { expandedGroups[groupIndex] = !isExpanded }
                        )
                    }
                } else {
                    // Esercizio singolo completato
                    val exercise = group.first()
                    val completedSeries = seriesMap[exercise.id] ?: emptyList()

                    ExerciseProgressItem(
                        exercise = exercise,
                        completedSeries = completedSeries,
                        isTimerRunning = false,
                        onAddSeries = { _, _ -> /* Non dovrebbe essere chiamato */ },
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

/**
 * Raggruppa gli esercizi in base al tipo di set e alla connessione
 */
private fun groupExercisesByType(exercises: List<WorkoutExercise>): List<List<WorkoutExercise>> {
    val result = mutableListOf<List<WorkoutExercise>>()
    var currentGroup = mutableListOf<WorkoutExercise>()

    exercises.forEachIndexed { index, exercise ->
        // Se è il primo esercizio o non è collegato al precedente, inizia un nuovo gruppo
        if (index == 0 || !exercise.linkedToPrevious) {
            // Se avevamo già un gruppo, aggiungiamolo al risultato
            if (currentGroup.isNotEmpty()) {
                result.add(currentGroup.toList())
            }
            // Inizia un nuovo gruppo con questo esercizio
            currentGroup = mutableListOf(exercise)
        } else {
            // Questo esercizio è collegato al precedente, aggiungilo al gruppo corrente
            currentGroup.add(exercise)
        }
    }

    // Aggiungi l'ultimo gruppo se non è vuoto
    if (currentGroup.isNotEmpty()) {
        result.add(currentGroup.toList())
    }

    return result
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

// La definizione della funzione WorkoutSuccessScreen è stata rimossa
// per evitare conflitti con l'implementazione esistente nel progetto.
// I riferimenti alla funzione sono stati mantenuti poiché utilizzano
// la versione già definita altrove.