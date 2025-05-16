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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    // Gestisce il completamento dell'allenamento
    LaunchedEffect(completeWorkoutState) {
        if (completeWorkoutState is CompleteWorkoutState.Success) {
            onWorkoutCompleted()
        } else if (completeWorkoutState is CompleteWorkoutState.Error) {
            snackbarHostState.showSnackbar(
                message = (completeWorkoutState as CompleteWorkoutState.Error).message,
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
                        WorkoutCompletedScreen(
                            workout = workout,
                            seriesState = seriesState,
                            elapsedTime = elapsedTime,
                            onComplete = {
                                viewModel.completeWorkout()
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
                                onSeriesCompleted = { exerciseId, weight, reps, serieNumber ->
                                    viewModel.addCompletedSeries(exerciseId, weight, reps, serieNumber)
                                },
                                onStopTimer = {
                                    viewModel.stopRecoveryTimer()
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
                                onSeriesCompleted = { exerciseId, weight, reps, serieNumber ->
                                    viewModel.addCompletedSeries(exerciseId, weight, reps, serieNumber)
                                },
                                onStopTimer = {
                                    viewModel.stopRecoveryTimer()
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
 * Nuova visualizzazione dell'allenamento con UI moderna
 */
@Composable
private fun ModernActiveWorkoutContent(
    workout: ActiveWorkout,
    seriesState: CompletedSeriesState,
    isTimerRunning: Boolean,
    recoveryTime: Int,
    currentRecoveryExerciseId: Int?,
    onSeriesCompleted: (Int, Float, Int, Int) -> Unit,
    onStopTimer: () -> Unit,
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
                if (group.size > 1 && (group.first().setType == "superset" || group.first().setType == "circuit")) {
                    // Gruppo di esercizi (superset/circuit)
                    // Nuova visualizzazione moderna
                    val isSuperset = group.first().setType == "superset"
                    val title = if (isSuperset) "Superset ${index + 1}" else "Circuito ${index + 1}"
                    val subtitle = "${group.size} esercizi"

                    ModernWorkoutGroupCard(
                        title = title,
                        subtitle = subtitle,
                        exercises = group,
                        completedSeries = seriesMap,
                        isSuperset = isSuperset,
                        onAddSeries = onSeriesCompleted
                    )
                } else {
                    // Esercizio singolo
                    val exercise = group.first()
                    val completedSeries = seriesMap[exercise.id] ?: emptyList()

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
                        isCompleted = false
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
                if (group.size > 1 && (group.first().setType == "superset" || group.first().setType == "circuit")) {
                    // Gruppo di esercizi (superset/circuit) completato
                    val isSuperset = group.first().setType == "superset"
                    val title = if (isSuperset) "Superset ${index + 1}" else "Circuito ${index + 1}"
                    val subtitle = "${group.size} esercizi - Completato"

                    ModernWorkoutGroupCard(
                        title = title,
                        subtitle = subtitle,
                        exercises = group,
                        completedSeries = seriesMap,
                        isSuperset = isSuperset,
                        onAddSeries = { _, _, _, _ -> /* Non dovrebbe essere chiamato */ }
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

@Composable
private fun ActiveWorkoutContent(
    workout: ActiveWorkout,
    seriesState: CompletedSeriesState,
    isTimerRunning: Boolean,
    recoveryTime: Int,
    currentRecoveryExerciseId: Int?,
    onSeriesCompleted: (Int, Float, Int, Int) -> Unit,
    onStopTimer: () -> Unit
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

                if (group.size > 1 && (group.first().setType == "superset" || group.first().setType == "circuit")) {
                    // Gruppo di esercizi (superset/circuit)
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Modified ExerciseGroupCard to use custom expansion state
                        ManagedExerciseGroupCard(
                            exercises = group,
                            completedSeries = seriesMap,
                            isTimerRunning = isTimerRunning,
                            onAddSeries = { exerciseId, weight, reps, serieNumber ->
                                onSeriesCompleted(exerciseId, weight, reps, serieNumber)
                            },
                            isCompleted = false,
                            isExpanded = isExpanded,
                            onExpandToggle = { expandedGroups[index] = !isExpanded }
                        )
                    }
                } else {
                    // Esercizio singolo
                    val exercise = group.first()
                    val completedSeries = seriesMap[exercise.id] ?: emptyList()

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
                        isCompleted = false
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

                if (group.size > 1 && (group.first().setType == "superset" || group.first().setType == "circuit")) {
                    // Gruppo di esercizi (superset/circuit) completato
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ManagedExerciseGroupCard(
                            exercises = group,
                            completedSeries = seriesMap,
                            isTimerRunning = false,
                            onAddSeries = { _, _, _, _ -> /* Non dovrebbe essere chiamato */ },
                            isCompleted = true,
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

@Composable
private fun WorkoutCompletedScreen(
    workout: ActiveWorkout,
    seriesState: CompletedSeriesState,
    elapsedTime: Int,
    onComplete: () -> Unit
) {
    val seriesMap = when (seriesState) {
        is CompletedSeriesState.Success -> seriesState.series
        else -> emptyMap()
    }

    // Calcola le statistiche dell'allenamento
    var totalSeries = 0
    var totalWeight = 0f
    var totalReps = 0

    seriesMap.values.forEach { seriesList ->
        totalSeries += seriesList.size

        seriesList.forEach { series ->
            totalWeight += series.peso
            totalReps += series.ripetizioni
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Allenamento Completato!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Hai completato con successo l'allenamento",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Statistiche
        Text(
            text = "Riepilogo",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Serie",
                value = totalSeries.toString(),
                icon = Icons.Default.FitnessCenter,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Peso",
                value = "$totalWeight kg",
                icon = Icons.Default.Speed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Ripetizioni",
                value = totalReps.toString(),
                icon = Icons.Default.Repeat,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Durata",
                value = "${elapsedTime} min",
                icon = Icons.Default.Timer,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Pulsante di completamento
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Salva Allenamento",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
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