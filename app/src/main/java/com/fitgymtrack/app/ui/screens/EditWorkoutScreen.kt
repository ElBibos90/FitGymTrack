package com.fitgymtrack.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.ui.components.ExerciseSelectionDialog
import com.fitgymtrack.app.ui.components.SnackbarMessage
import com.fitgymtrack.app.ui.components.WorkoutExerciseEditor
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.EditWorkoutViewModel
import com.fitgymtrack.app.viewmodel.UserExerciseViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutScreen(
    schedaId: Int,
    onBack: () -> Unit,
    onWorkoutUpdated: () -> Unit,
    viewModel: EditWorkoutViewModel = viewModel(),
    userExerciseViewModel: UserExerciseViewModel = viewModel() // ViewModel per gli esercizi personalizzati
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // Stato per memorizzare l'utente corrente
    var currentUser by remember { mutableStateOf<com.fitgymtrack.app.models.User?>(null) }

    val workoutName by viewModel.workoutName.collectAsState()
    val workoutDescription by viewModel.workoutDescription.collectAsState()
    val selectedExercises by viewModel.selectedExercises.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    val workoutLoadingState by viewModel.workoutLoadingState.collectAsState()

    // Stati per la selezione degli esercizi
    val availableExercises by viewModel.availableExercises.collectAsState()
    val exercisesLoadingState by viewModel.exercisesLoadingState.collectAsState()
    var showExerciseDialog by remember { mutableStateOf(false) }

    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isSnackbarSuccess by remember { mutableStateOf(true) }

    // Carica i dati della scheda e l'utente corrente all'avvio
    LaunchedEffect(schedaId) {
        // Ottieni l'utente corrente
        sessionManager.getUserData().collect { user ->
            currentUser = user

            // Carica la scheda di allenamento
            viewModel.loadWorkoutPlan(schedaId, sessionManager)

            // Carica anche la lista degli esercizi disponibili
            if (user?.id != null && user.id > 0) {
                viewModel.loadAvailableExercises(user.id)
            }
        }
    }

    // Gestisci le azioni in base allo stato di caricamento
    LaunchedEffect(loadingState) {
        when (loadingState) {
            is EditWorkoutViewModel.LoadingState.Success -> {
                snackbarMessage = "Scheda aggiornata con successo!"
                isSnackbarSuccess = true
                showSnackbar = true
                // Torna alla schermata precedente dopo un breve ritardo
                kotlinx.coroutines.delay(1000)
                onWorkoutUpdated()
            }
            is EditWorkoutViewModel.LoadingState.Error -> {
                snackbarMessage = (loadingState as EditWorkoutViewModel.LoadingState.Error).message
                isSnackbarSuccess = false
                showSnackbar = true
            }
            else -> {}
        }
    }

    // Estrai gli ID degli esercizi già selezionati per passarli al dialogo
    val selectedExerciseIds = selectedExercises.map { it.id }

    // Dialog per la selezione degli esercizi
    if (showExerciseDialog) {
        ExerciseSelectionDialog(
            exercises = availableExercises,
            selectedExerciseIds = selectedExerciseIds,
            isLoading = exercisesLoadingState is EditWorkoutViewModel.LoadingState.Loading,
            onExerciseSelected = { exercise ->
                viewModel.addExercise(exercise)
                snackbarMessage = "Aggiunto: ${exercise.nome}"
                isSnackbarSuccess = true
                showSnackbar = true
            },
            onDismissRequest = {
                showExerciseDialog = false
            },
            // Funzione per aggiornare gli esercizi dopo la creazione
            onExercisesRefresh = {
                // Refresh sincrono degli esercizi
                coroutineScope.launch {
                    currentUser?.id?.let { userId ->
                        viewModel.loadAvailableExercises(userId)
                    }
                }
                Unit
            },
            currentUser = currentUser,
            userExerciseViewModel = userExerciseViewModel
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Modifica scheda") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    // Pulsante di salvataggio nella barra superiore
                    if (loadingState is EditWorkoutViewModel.LoadingState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.updateWorkoutPlan(schedaId)
                                }
                            },
                            enabled = workoutName.isNotBlank() && selectedExercises.isNotEmpty()
                        ) {
                            Text("Salva")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (workoutLoadingState) {
                is EditWorkoutViewModel.LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is EditWorkoutViewModel.LoadingState.Error -> {
                    val errorMessage = (workoutLoadingState as EditWorkoutViewModel.LoadingState.Error).message
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Errore: $errorMessage",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadWorkoutPlan(schedaId, sessionManager) }
                            ) {
                                Text("Riprova")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Sezione informazioni scheda
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Informazioni scheda",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = workoutName,
                                        onValueChange = { viewModel.updateWorkoutName(it) },
                                        label = { Text("Nome scheda*") },
                                        placeholder = { Text("Es. Scheda Forza, Allenamento Gambe...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        isError = workoutName.isBlank() && loadingState is EditWorkoutViewModel.LoadingState.Error
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = workoutDescription,
                                        onValueChange = { viewModel.updateWorkoutDescription(it) },
                                        label = { Text("Descrizione") },
                                        placeholder = { Text("Descrizione opzionale della scheda") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3
                                    )
                                }
                            }
                        }

                        // Sezione esercizi selezionati
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Esercizi selezionati (${selectedExercises.size})",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )

                                        if (selectedExercises.isNotEmpty()) {
                                            Text(
                                                text = "${selectedExercises.size} esercizi",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (selectedExercises.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Nessun esercizio selezionato",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        // Lista degli esercizi con editor
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            selectedExercises.forEachIndexed { index, exercise ->
                                                WorkoutExerciseEditor(
                                                    exercise = exercise,
                                                    onUpdate = { updatedExercise ->
                                                        viewModel.updateExerciseDetails(index, updatedExercise)
                                                    },
                                                    onDelete = {
                                                        viewModel.removeExercise(index)
                                                        snackbarMessage = "Rimosso: ${exercise.nome}"
                                                        isSnackbarSuccess = true
                                                        showSnackbar = true
                                                    },
                                                    onMoveUp = {
                                                        viewModel.moveExerciseUp(index)
                                                    },
                                                    onMoveDown = {
                                                        viewModel.moveExerciseDown(index)
                                                    },
                                                    isFirst = index == 0,
                                                    isLast = index == selectedExercises.size - 1
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { showExerciseDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Aggiungi esercizi"
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Aggiungi esercizi")
                                    }
                                }
                            }
                        }

                        // Pulsante Salva
                        item {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.updateWorkoutPlan(schedaId)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = workoutName.isNotBlank() && selectedExercises.isNotEmpty() &&
                                        loadingState !is EditWorkoutViewModel.LoadingState.Loading
                            ) {
                                if (loadingState is EditWorkoutViewModel.LoadingState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Salva modifiche",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Snackbar per feedback
            if (showSnackbar) {
                SnackbarMessage(
                    message = snackbarMessage,
                    isSuccess = isSnackbarSuccess,
                    onDismiss = { showSnackbar = false }
                )
            }
        }
    }
}