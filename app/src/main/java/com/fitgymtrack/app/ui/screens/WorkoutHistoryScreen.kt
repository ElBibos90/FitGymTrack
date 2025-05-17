package com.fitgymtrack.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.CompletedSeriesData
import com.fitgymtrack.app.models.WorkoutHistory
import com.fitgymtrack.app.ui.components.SnackbarMessage
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.WorkoutHistoryViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    onBack: () -> Unit,
    viewModel: WorkoutHistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Stati del ViewModel
    val workoutHistoryState by viewModel.workoutHistoryState.collectAsState()
    val workoutDetailState by viewModel.workoutDetailState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val selectedWorkout by viewModel.selectedWorkout.collectAsState()
    val workoutHistory by viewModel.workoutHistory.collectAsState()
    val seriesDetails by viewModel.seriesDetails.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val seriesDetailsState by viewModel.seriesDetailsState.collectAsState()

    // Stato locale
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<WorkoutHistory?>(null) }
    var seriesIdToDelete by remember { mutableStateOf<String?>(null) }
    var seriesToEdit by remember { mutableStateOf<CompletedSeriesData?>(null) }

    // Stati per editing
    var editWeight by remember { mutableStateOf(0f) }
    var editReps by remember { mutableStateOf(0) }
    var editRecoveryTime by remember { mutableStateOf(0) }

    // Stato per la navigazione
    var showingDetails by remember { mutableStateOf(false) }

    // Gestisce la navigazione indietro quando si visualizzano i dettagli
    BackHandler(enabled = showingDetails) {
        showingDetails = false
    }

    // Carica dati all'avvio
    LaunchedEffect(Unit) {
        Log.d("WorkoutHistory", "Caricamento dati iniziale")
        coroutineScope.launch {
            sessionManager.getUserData().collect { user ->
                if (user != null) {
                    Log.d("WorkoutHistory", "Utente trovato: id=${user.id}, caricamento cronologia allenamenti")
                    viewModel.loadWorkoutHistory(user.id)
                } else {
                    Log.e("WorkoutHistory", "Nessun utente trovato in sessione")
                }
            }
        }
    }

    // Gestisci stato delete
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is WorkoutHistoryViewModel.OperationState.Success -> {
                Log.d("WorkoutHistory", "Eliminazione completata: ${(deleteState as WorkoutHistoryViewModel.OperationState.Success).message}")
                snackbarHostState.showSnackbar(
                    (deleteState as WorkoutHistoryViewModel.OperationState.Success).message
                )
                // Se abbiamo eliminato l'allenamento attualmente visualizzato, torna alla lista
                if (workoutToDelete != null && workoutToDelete?.id == selectedWorkout?.id) {
                    showingDetails = false
                }
                viewModel.resetDeleteState()
            }
            is WorkoutHistoryViewModel.OperationState.Error -> {
                Log.e("WorkoutHistory", "Errore durante l'eliminazione: ${(deleteState as WorkoutHistoryViewModel.OperationState.Error).message}")
                snackbarHostState.showSnackbar(
                    (deleteState as WorkoutHistoryViewModel.OperationState.Error).message
                )
                viewModel.resetDeleteState()
            }
            else -> {}
        }
    }

    // Gestisci stato update
    LaunchedEffect(updateState) {
        when (updateState) {
            is WorkoutHistoryViewModel.UpdateState.Success -> {
                Log.d("WorkoutHistory", "Aggiornamento completato: ${(updateState as WorkoutHistoryViewModel.UpdateState.Success).message}")
                snackbarHostState.showSnackbar(
                    (updateState as WorkoutHistoryViewModel.UpdateState.Success).message
                )
                viewModel.resetUpdateState()
            }
            is WorkoutHistoryViewModel.UpdateState.Error -> {
                Log.e("WorkoutHistory", "Errore durante l'aggiornamento: ${(updateState as WorkoutHistoryViewModel.UpdateState.Error).message}")
                snackbarHostState.showSnackbar(
                    (updateState as WorkoutHistoryViewModel.UpdateState.Error).message
                )
                viewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (showingDetails && selectedWorkout != null)
                        selectedWorkout?.schedaNome ?: "Dettagli Allenamento"
                    else
                        "Storico Allenamenti"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showingDetails) {
                            showingDetails = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Visualizzazione sequenziale: solo una delle due viste è visibile in un dato momento

            // Lista degli allenamenti
            AnimatedVisibility(
                visible = !showingDetails,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                when (workoutHistoryState) {
                    is WorkoutHistoryViewModel.WorkoutHistoryState.Loading -> {
                        // Stato di caricamento
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is WorkoutHistoryViewModel.WorkoutHistoryState.Error -> {
                        // Errore
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = (workoutHistoryState as WorkoutHistoryViewModel.WorkoutHistoryState.Error).message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            sessionManager.getUserData().collect { user ->
                                                if (user != null) {
                                                    viewModel.loadWorkoutHistory(user.id)
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Text("Riprova")
                                }
                            }
                        }
                    }
                    else -> {
                        // Lista degli allenamenti
                        if (workoutHistory.isEmpty()) {
                            // Nessun allenamento
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Nessun allenamento trovato",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Inizia un allenamento per vederlo qui",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Lista degli allenamenti
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                items(workoutHistory) { workout ->
                                    WorkoutHistoryItem(
                                        workout = workout,
                                        isSelected = false, // Non serve più questo stato
                                        onClick = {
                                            Log.d("WorkoutHistory", "Selezionato allenamento: id=${workout.id}")
                                            viewModel.selectWorkout(workout)
                                            viewModel.loadSeriesDetails(workout.id)
                                            showingDetails = true // Mostra i dettagli
                                        },
                                        onLongClick = {
                                            workoutToDelete = workout
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dettagli dell'allenamento
            AnimatedVisibility(
                visible = showingDetails && selectedWorkout != null,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                selectedWorkout?.let { workout ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            // Card informazioni generali
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    // Data allenamento
                                    Text(
                                        text = "Data: ${workout.formattedDate}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    // Durata
                                    if (workout.durataTotale != null && workout.durataTotale > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Durata: ${workout.formattedDuration}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    // Stato completamento
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (workout.isCompleted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (workout.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (workout.isCompleted) "Completato" else "Non completato",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (workout.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }

                                    // Note
                                    if (!workout.note.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Note:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = workout.note,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                }
                            }

                            // Pulsante elimina
                            Button(
                                onClick = {
                                    workoutToDelete = workout
                                    showDeleteDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Elimina allenamento")
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Serie completate
                            val workoutSeries = seriesDetails[workout.id] ?: emptyList()

                            Log.d("WorkoutHistory", "Dettagli per allenamento ${workout.id}: " +
                                    "serie trovate=${workoutSeries.size}")

                            if (workoutSeries.isNotEmpty()) {
                                Text(
                                    text = "Serie completate:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                // Group series by exercise for display
                                val seriesByExercise = workoutSeries.groupBy { it.esercizioId ?: 0 }

                                Log.d("WorkoutHistory", "Serie raggruppate per esercizio: ${seriesByExercise.keys.size} esercizi")
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nessuna serie completata per questo allenamento",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Serie raggruppate per esercizio
                        val workoutSeries = seriesDetails[workout.id] ?: emptyList()
                        val seriesByExercise = workoutSeries.groupBy { it.esercizioId ?: 0 }

                        seriesByExercise.forEach { (exerciseId, seriesList) ->
                            if (exerciseId > 0) {
                                item {
                                    val exerciseName = seriesList.firstOrNull()?.esercizioNome ?: "Esercizio sconosciuto"

                                    ExerciseSeriesGroup(
                                        exerciseName = exerciseName,
                                        series = seriesList,
                                        onDeleteSeries = { seriesId ->
                                            seriesIdToDelete = seriesId
                                            showDeleteDialog = true
                                        },
                                        onEditSeries = { series ->
                                            seriesToEdit = series
                                            editWeight = series.peso
                                            editReps = series.ripetizioni
                                            editRecoveryTime = series.tempoRecupero ?: 60
                                            showEditDialog = true
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

    // Dialog Elimina Allenamento/Serie
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                workoutToDelete = null
                seriesIdToDelete = null
            },
            title = {
                Text(
                    if (seriesIdToDelete != null) "Elimina serie" else "Elimina allenamento"
                )
            },
            text = {
                Text(
                    if (seriesIdToDelete != null)
                        "Sei sicuro di voler eliminare questa serie? Questa azione non può essere annullata."
                    else
                        "Sei sicuro di voler eliminare questo allenamento? Questa azione non può essere annullata."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (seriesIdToDelete != null) {
                            // Elimina serie
                            val workoutId = selectedWorkout?.id ?: 0
                            Log.d("WorkoutHistory", "Richiesta eliminazione serie $seriesIdToDelete da allenamento $workoutId")
                            viewModel.deleteCompletedSeries(seriesIdToDelete!!, workoutId)
                        } else if (workoutToDelete != null) {
                            // Elimina allenamento
                            Log.d("WorkoutHistory", "Richiesta eliminazione allenamento ${workoutToDelete?.id}")
                            viewModel.deleteWorkout(workoutToDelete!!.id)
                        }
                        showDeleteDialog = false
                        workoutToDelete = null
                        seriesIdToDelete = null
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
                        seriesIdToDelete = null
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    // Dialog Edit Serie
    if (showEditDialog && seriesToEdit != null) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                seriesToEdit = null
            },
            title = { Text("Modifica serie") },
            text = {
                Column {
                    // Peso
                    OutlinedTextField(
                        value = editWeight.toString(),
                        onValueChange = {
                            editWeight = it.toFloatOrNull() ?: 0f
                        },
                        label = { Text("Peso (kg)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Ripetizioni
                    OutlinedTextField(
                        value = editReps.toString(),
                        onValueChange = {
                            editReps = it.toIntOrNull() ?: 0
                        },
                        label = { Text("Ripetizioni") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tempo di recupero
                    OutlinedTextField(
                        value = editRecoveryTime.toString(),
                        onValueChange = {
                            editRecoveryTime = it.toIntOrNull() ?: 60
                        },
                        label = { Text("Tempo di recupero (sec)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val workoutId = selectedWorkout?.id ?: 0

                        Log.d("WorkoutHistory", "Aggiornamento serie ${seriesToEdit?.id}: " +
                                "peso=$editWeight, rip=$editReps, recupero=$editRecoveryTime")

                        viewModel.updateCompletedSeries(
                            seriesId = seriesToEdit!!.id,
                            workoutId = workoutId,
                            weight = editWeight,
                            reps = editReps,
                            recoveryTime = editRecoveryTime
                        )

                        showEditDialog = false
                        seriesToEdit = null
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        seriesToEdit = null
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
fun WorkoutHistoryItem(
    workout: WorkoutHistory,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
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
                // Nome scheda
                Text(
                    text = workout.schedaNome ?: "Scheda senza nome",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Data formattata
                Text(
                    text = workout.formattedDate,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Durata
                if (workout.durataTotale != null && workout.durataTotale > 0) {
                    Text(
                        text = "Durata: ${workout.formattedDuration}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Status icon
            Icon(
                imageVector = if (workout.isCompleted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = if (workout.isCompleted) "Completato" else "Non completato",
                tint = if (workout.isCompleted) Indigo600 else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ExerciseSeriesGroup(
    exerciseName: String,
    series: List<CompletedSeriesData>,
    onDeleteSeries: (String) -> Unit,
    onEditSeries: (CompletedSeriesData) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Exercise name
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Series list
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                series.sortedBy { it.realSerieNumber ?: it.serieNumber }.forEach { serie ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Serie number
                        Text(
                            text = "Serie ${serie.realSerieNumber ?: serie.serieNumber ?: 0}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        // Weight and reps
                        Text(
                            text = "${serie.peso} kg × ${serie.ripetizioni}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Actions
                        Row {
                            IconButton(
                                onClick = { onEditSeries(serie) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Modifica",
                                    tint = Indigo600
                                )
                            }

                            IconButton(
                                onClick = { onDeleteSeries(serie.id) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Elimina",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}