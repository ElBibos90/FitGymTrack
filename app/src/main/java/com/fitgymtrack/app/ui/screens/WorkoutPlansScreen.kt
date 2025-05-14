package com.fitgymtrack.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.WorkoutPlan
import com.fitgymtrack.app.ui.components.SnackbarMessage
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.WorkoutViewModel
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlansScreen(
    onBack: () -> Unit,
    onCreateWorkout: () -> Unit,
    onEditWorkout: (Int) -> Unit,
    onStartWorkout: (Int) -> Unit,
    viewModel: WorkoutViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val workoutPlansState by viewModel.workoutPlansState.collectAsState()
    val workoutPlans by viewModel.workoutPlans.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val expandedWorkoutId by viewModel.expandedWorkoutId.collectAsState()
    val selectedWorkoutExercises by viewModel.selectedWorkoutExercises.collectAsState()
    val workoutDetailsState by viewModel.workoutDetailsState.collectAsState() // Colleghiamo questo stato

    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isSnackbarSuccess by remember { mutableStateOf(true) }

    // Gestione del dialog di conferma eliminazione
    var showDeleteDialog by remember { mutableStateOf(false) }
    var schedaToDelete by remember { mutableStateOf<Int?>(null) }
    var schedaNameToDelete by remember { mutableStateOf("") }

    // Effetto per mostrare/nascondere lo snackbar in base allo stato di eliminazione
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is WorkoutViewModel.DeleteState.Success -> {
                snackbarMessage = (deleteState as WorkoutViewModel.DeleteState.Success).message
                isSnackbarSuccess = true
                showSnackbar = true
                viewModel.resetDeleteState()
            }
            is WorkoutViewModel.DeleteState.Error -> {
                snackbarMessage = (deleteState as WorkoutViewModel.DeleteState.Error).message
                isSnackbarSuccess = false
                showSnackbar = true
                viewModel.resetDeleteState()
            }
            else -> {}
        }
    }

    // Caricamento iniziale delle schede
    LaunchedEffect(Unit) {
        viewModel.loadWorkoutPlans(sessionManager)
    }

    // Dialog di conferma eliminazione
    if (showDeleteDialog && schedaToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                schedaToDelete = null
            },
            title = { Text("Conferma eliminazione") },
            text = {
                Text("Sei sicuro di voler eliminare la scheda \"$schedaNameToDelete\"? Questa azione non può essere annullata.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        schedaToDelete?.let { viewModel.deleteWorkoutPlan(it) }
                        showDeleteDialog = false
                        schedaToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteDialog = false
                        schedaToDelete = null
                    }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Le tue schede") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateWorkout) {
                        Icon(Icons.Default.Add, contentDescription = "Crea scheda")
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
            when (workoutPlansState) {
                is WorkoutViewModel.WorkoutPlansState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is WorkoutViewModel.WorkoutPlansState.Error -> {
                    val errorMessage = (workoutPlansState as WorkoutViewModel.WorkoutPlansState.Error).message
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
                                onClick = { viewModel.loadWorkoutPlans(sessionManager) }
                            ) {
                                Text("Riprova")
                            }
                        }
                    }
                }

                is WorkoutViewModel.WorkoutPlansState.Success -> {
                    if (workoutPlans.isEmpty()) {
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
                                    contentDescription = "Nessuna scheda",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Non hai ancora creato schede di allenamento",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onCreateWorkout
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Aggiungi",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Crea scheda")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(workoutPlans) { workoutPlan ->
                                WorkoutPlanCard(
                                    workoutPlan = workoutPlan,
                                    isExpanded = expandedWorkoutId == workoutPlan.id,
                                    exercises = if (expandedWorkoutId == workoutPlan.id) selectedWorkoutExercises else emptyList(),
                                    onExpandClick = { viewModel.loadWorkoutExercises(workoutPlan.id) },
                                    onDeleteClick = {
                                        schedaToDelete = workoutPlan.id
                                        schedaNameToDelete = workoutPlan.nome
                                        showDeleteDialog = true
                                    },
                                    onEditClick = { onEditWorkout(workoutPlan.id) },
                                    onStartWorkoutClick = { onStartWorkout(workoutPlan.id) },
                                    isLoading = workoutPlan.id == expandedWorkoutId &&
                                            workoutDetailsState is WorkoutViewModel.WorkoutDetailsState.Loading
                                )
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

@Composable
fun WorkoutPlanCard(
    workoutPlan: WorkoutPlan,
    isExpanded: Boolean,
    exercises: List<com.fitgymtrack.app.models.WorkoutExercise>,
    onExpandClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onStartWorkoutClick: () -> Unit,
    isLoading: Boolean
) {
    // Corretto il parsing della data utilizzando un formato più flessibile
    val formattedDate = try {
        // Prova prima il formato standard yyyy-MM-dd HH:mm:ss
        val parsers = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )

        var date: Date? = null
        for (parser in parsers) {
            try {
                date = parser.parse(workoutPlan.dataCreazione)
                break
            } catch (e: ParseException) {
                // Continua con il prossimo parser
            }
        }

        date?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
        } ?: "Data sconosciuta"
    } catch (e: Exception) {
        "Data sconosciuta"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header della scheda
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = workoutPlan.nome,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (!workoutPlan.descrizione.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = workoutPlan.descrizione,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Creata il: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pulsanti di azione
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExpandClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Nascondi" else "Mostra esercizi"
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isExpanded) "Nascondi" else "Esercizi"
                        )
                    }

                    OutlinedButton(
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Modifica"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Modifica")
                    }

                    OutlinedButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Elimina"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Elimina")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onStartWorkoutClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Inizia"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Inizia allenamento")
                }
            }

            // Sezione esercizi espandibile
            if (isExpanded) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (exercises.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun esercizio trovato in questa scheda",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        exercises.forEach { exercise ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = exercise.nome,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = "${exercise.serie} × ${exercise.ripetizioni}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                if (exercise.peso > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "@ ${exercise.peso} kg",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            if (exercise.gruppoMuscolare != null) {
                                Text(
                                    text = exercise.gruppoMuscolare,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}