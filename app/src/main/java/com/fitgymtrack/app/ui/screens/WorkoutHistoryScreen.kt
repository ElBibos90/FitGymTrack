package com.fitgymtrack.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.CompletedSeriesData
import com.fitgymtrack.app.models.WorkoutHistory
import com.fitgymtrack.app.ui.components.SnackbarMessage
import com.fitgymtrack.app.ui.components.WeightPickerDialog
import com.fitgymtrack.app.ui.components.RepsPickerDialog
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.WorkoutHistoryViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    onBack: () -> Unit,
    viewModel: WorkoutHistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // States
    val workoutHistoryState by viewModel.workoutHistoryState.collectAsState()
    val workoutDetailState by viewModel.workoutDetailState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val selectedWorkout by viewModel.selectedWorkout.collectAsState()

    // UI states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<WorkoutHistory?>(null) }
    var seriesToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteSeriesDialog by remember { mutableStateOf(false) }

    var seriesToEdit by remember { mutableStateOf<CompletedSeriesData?>(null) }
    var showWeightPickerDialog by remember { mutableStateOf(false) }
    var showRepsPickerDialog by remember { mutableStateOf(false) }

    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isSnackbarSuccess by remember { mutableStateOf(true) }

    // Load workout history when screen is first shown
    LaunchedEffect(Unit) {
        val userData = sessionManager.getUserData().first()
        userData?.id?.let { userId ->
            viewModel.loadWorkoutHistory(userId)
        }
    }

    // Handle delete state changes
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is WorkoutHistoryViewModel.OperationState.Success -> {
                val message = (deleteState as WorkoutHistoryViewModel.OperationState.Success).message
                snackbarMessage = message
                isSnackbarSuccess = true
                showSnackbar = true
                viewModel.resetDeleteState()
            }
            is WorkoutHistoryViewModel.OperationState.Error -> {
                val message = (deleteState as WorkoutHistoryViewModel.OperationState.Error).message
                snackbarMessage = message
                isSnackbarSuccess = false
                showSnackbar = true
                viewModel.resetDeleteState()
            }
            else -> {}
        }
    }

    // Handle update state changes
    LaunchedEffect(updateState) {
        when (updateState) {
            is WorkoutHistoryViewModel.OperationState.Success -> {
                val message = (updateState as WorkoutHistoryViewModel.OperationState.Success).message
                snackbarMessage = message
                isSnackbarSuccess = true
                showSnackbar = true
                viewModel.resetUpdateState()
            }
            is WorkoutHistoryViewModel.OperationState.Error -> {
                val message = (updateState as WorkoutHistoryViewModel.OperationState.Error).message
                snackbarMessage = message
                isSnackbarSuccess = false
                showSnackbar = true
                viewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storico Allenamenti") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content based on state
            when (workoutHistoryState) {
                is WorkoutHistoryViewModel.WorkoutHistoryState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is WorkoutHistoryViewModel.WorkoutHistoryState.Error -> {
                    val errorMessage = (workoutHistoryState as WorkoutHistoryViewModel.WorkoutHistoryState.Error).message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Errore",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val userData = sessionManager.getUserData().first()
                                        userData?.id?.let { userId ->
                                            viewModel.loadWorkoutHistory(userId)
                                        }
                                    }
                                }
                            ) {
                                Text("Riprova")
                            }
                        }
                    }
                }
                is WorkoutHistoryViewModel.WorkoutHistoryState.Success -> {
                    val workouts = (workoutHistoryState as WorkoutHistoryViewModel.WorkoutHistoryState.Success).workouts

                    if (workouts.isEmpty()) {
                        EmptyHistoryView()
                    } else {
                        WorkoutHistoryList(
                            workouts = workouts,
                            selectedWorkout = selectedWorkout,
                            workoutDetailState = workoutDetailState,
                            onWorkoutClick = { workout ->
                                if (selectedWorkout?.id == workout.id) {
                                    viewModel.selectWorkout(workout)
                                } else {
                                    viewModel.selectWorkout(workout)
                                }
                            },
                            onDeleteWorkout = { workout ->
                                workoutToDelete = workout
                                showDeleteDialog = true
                            },
                            onDeleteSeries = { seriesId ->
                                seriesToDelete = seriesId
                                showDeleteSeriesDialog = true
                            },
                            onEditSeries = { series ->
                                seriesToEdit = series
                                showWeightPickerDialog = true
                            }
                        )
                    }
                }
                else -> {
                    // Idle state, show nothing
                }
            }

            // Snackbar for feedback
            if (showSnackbar) {
                SnackbarMessage(
                    message = snackbarMessage,
                    isSuccess = isSnackbarSuccess,
                    onDismiss = { showSnackbar = false }
                )
            }

            // Delete workout confirmation dialog
            if (showDeleteDialog && workoutToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        workoutToDelete = null
                    },
                    title = { Text("Conferma eliminazione") },
                    text = {
                        Text("Sei sicuro di voler eliminare questo allenamento? Questa azione non può essere annullata.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val userData = sessionManager.getUserData().first()
                                    userData?.id?.let { userId ->
                                        workoutToDelete?.id?.let { workoutId ->
                                            viewModel.deleteWorkout(workoutId, userId)
                                        }
                                    }
                                }
                                showDeleteDialog = false
                                workoutToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Elimina")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                workoutToDelete = null
                            }
                        ) {
                            Text("Annulla")
                        }
                    }
                )
            }

            // Delete series confirmation dialog
            if (showDeleteSeriesDialog && seriesToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteSeriesDialog = false
                        seriesToDelete = null
                    },
                    title = { Text("Conferma eliminazione") },
                    text = {
                        Text("Sei sicuro di voler eliminare questa serie? Questa azione non può essere annullata.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                seriesToDelete?.let { seriesId ->
                                    viewModel.deleteCompletedSeries(seriesId)
                                }
                                showDeleteSeriesDialog = false
                                seriesToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Elimina")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteSeriesDialog = false
                                seriesToDelete = null
                            }
                        ) {
                            Text("Annulla")
                        }
                    }
                )
            }

            // Weight picker dialog
            if (showWeightPickerDialog && seriesToEdit != null) {
                WeightPickerDialog(
                    initialWeight = seriesToEdit!!.peso,
                    onDismiss = {
                        showWeightPickerDialog = false
                    },
                    onConfirm = { newWeight ->
                        showWeightPickerDialog = false
                        showRepsPickerDialog = true
                        seriesToEdit = seriesToEdit?.copy(peso = newWeight)
                    }
                )
            }

            // Reps picker dialog
            if (showRepsPickerDialog && seriesToEdit != null) {
                RepsPickerDialog(
                    initialReps = seriesToEdit!!.ripetizioni,
                    isIsometric = false, // Since we don't have an easy way to know if it's isometric, assume false
                    onDismiss = {
                        showRepsPickerDialog = false
                        seriesToEdit = null
                    },
                    onConfirm = { newReps ->
                        seriesToEdit?.let { series ->
                            viewModel.updateCompletedSeries(
                                seriesId = series.id,
                                weight = series.peso,
                                reps = newReps
                            )
                        }
                        showRepsPickerDialog = false
                        seriesToEdit = null
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = "Nessun allenamento",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nessun allenamento completato",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Inizia un nuovo allenamento per vederlo qui",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WorkoutHistoryList(
    workouts: List<WorkoutHistory>,
    selectedWorkout: WorkoutHistory?,
    workoutDetailState: WorkoutHistoryViewModel.WorkoutDetailState,
    onWorkoutClick: (WorkoutHistory) -> Unit,
    onDeleteWorkout: (WorkoutHistory) -> Unit,
    onDeleteSeries: (String) -> Unit,
    onEditSeries: (CompletedSeriesData) -> Unit
) {
    // Local state to track expanded workout IDs
    var expandedWorkoutId by remember { mutableStateOf<Int?>(null) }

    // When selected workout changes from ViewModel, update the expanded state
    LaunchedEffect(selectedWorkout) {
        expandedWorkoutId = selectedWorkout?.id
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(workouts) { workout ->
            val isExpanded = expandedWorkoutId == workout.id

            WorkoutHistoryItem(
                workout = workout,
                isExpanded = isExpanded,
                workoutDetailState = if (isExpanded) workoutDetailState else null,
                onWorkoutClick = {
                    // Toggle expansion locally first
                    expandedWorkoutId = if (isExpanded) null else workout.id
                    // Then notify ViewModel
                    onWorkoutClick(workout)
                },
                onDeleteWorkout = { onDeleteWorkout(workout) },
                onDeleteSeries = onDeleteSeries,
                onEditSeries = onEditSeries
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryItem(
    workout: WorkoutHistory,
    isExpanded: Boolean,
    workoutDetailState: WorkoutHistoryViewModel.WorkoutDetailState?,
    onWorkoutClick: () -> Unit,
    onDeleteWorkout: () -> Unit,
    onDeleteSeries: (String) -> Unit,
    onEditSeries: (CompletedSeriesData) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onWorkoutClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header (always visible)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (workout.isCompleted)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = workout.schedaNome ?: "Allenamento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = workout.formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (workout.durataTotale != null && workout.durataTotale > 0) {
                            Text(
                                text = "Durata: ${workout.formattedDuration}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!workout.isCompleted) {
                            Text(
                                text = "Non completato",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Row {
                        // Delete workout button
                        IconButton(onClick = onDeleteWorkout) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Elimina allenamento",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        // Expand/collapse button
                        IconButton(onClick = onWorkoutClick) {
                            Icon(
                                imageVector = if (isExpanded)
                                    Icons.Default.ExpandLess
                                else
                                    Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded)
                                    "Comprimi"
                                else
                                    "Espandi"
                            )
                        }
                    }
                }
            }

            // Expanded content (series details)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when (workoutDetailState) {
                        is WorkoutHistoryViewModel.WorkoutDetailState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        is WorkoutHistoryViewModel.WorkoutDetailState.Error -> {
                            val error = (workoutDetailState as WorkoutHistoryViewModel.WorkoutDetailState.Error).message
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Errore",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        is WorkoutHistoryViewModel.WorkoutDetailState.Success -> {
                            val series = (workoutDetailState as WorkoutHistoryViewModel.WorkoutDetailState.Success).series

                            if (series.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nessuna serie registrata per questo allenamento",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Group series by exercise
                                    val seriesByExercise = series.groupBy { it.esercizioId ?: 0 }

                                    seriesByExercise.forEach { (exerciseId, exerciseSeries) ->
                                        if (exerciseId > 0 && exerciseSeries.isNotEmpty()) {
                                            val exerciseName = exerciseSeries.firstOrNull()?.esercizioNome ?: "Esercizio"

                                            // Exercise header
                                            Text(
                                                text = exerciseName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )

                                            // Series list
                                            exerciseSeries.forEach { series ->
                                                SeriesItem(
                                                    series = series,
                                                    onDeleteSeries = { onDeleteSeries(series.id) },
                                                    onEditSeries = { onEditSeries(series) }
                                                )
                                            }

                                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            // Idle state, show nothing
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesItem(
    series: CompletedSeriesData,
    onDeleteSeries: () -> Unit,
    onEditSeries: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Series number indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Indigo600.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (series.realSerieNumber ?: 1).toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Indigo600
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Series details
            Column {
                Text(
                    text = "${series.peso} kg × ${series.ripetizioni} rep",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                if (series.timestamp.isNotEmpty()) {
                    val formattedTime = try {
                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        val date = dateFormat.parse(series.timestamp)
                        timeFormat.format(date)
                    } catch (e: Exception) {
                        series.timestamp
                    }

                    Text(
                        text = "Completata alle $formattedTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Action buttons
        Row {
            // Edit button
            IconButton(
                onClick = onEditSeries,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Modifica",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete button
            IconButton(
                onClick = onDeleteSeries,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Elimina",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}