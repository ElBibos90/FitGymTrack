package com.fitgymtrack.app.ui.screens

// NUOVO: Import per controllo limiti
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.extensions.WorkoutNotificationExtensions
import com.fitgymtrack.app.models.WorkoutPlan
import com.fitgymtrack.app.ui.components.SnackbarMessage
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // Gestisce il pulsante "back" del sistema
    BackHandler {
        onBack()
    }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    // MODIFICATO: Controllo limiti prima di creare scheda
                    IconButton(
                        onClick = {
                            // Controlla limiti solo quando user clicca per creare
                            WorkoutNotificationExtensions.checkLimitsBeforeCreation(
                                context = context,
                                resourceType = "workouts",
                                onLimitReached = {
                                    // La notifica è già stata creata automaticamente
                                    // Mostra anche snackbar per feedback immediato
                                    snackbarMessage = "Hai raggiunto il limite di schede per il tuo piano"
                                    isSnackbarSuccess = false
                                    showSnackbar = true
                                },
                                onCanProceed = {
                                    // Limite OK, procedi con la creazione
                                    onCreateWorkout()
                                }
                            )
                        }
                    ) {
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

                                // MODIFICATO: Anche qui controllo limiti
                                Button(
                                    onClick = {
                                        WorkoutNotificationExtensions.checkLimitsBeforeCreation(
                                            context = context,
                                            resourceType = "workouts",
                                            onLimitReached = {
                                                // Notifica + snackbar
                                                snackbarMessage = "Hai raggiunto il limite di schede per il tuo piano"
                                                isSnackbarSuccess = false
                                                showSnackbar = true
                                            },
                                            onCanProceed = {
                                                onCreateWorkout()
                                            }
                                        )
                                    }
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
    // Parsing difensivo della data - gestisce anche il caso null
    val formattedDate = try {
        // Log e verifica della data originale
        val originalDate = workoutPlan.dataCreazione
        Log.d("WorkoutPlanCard", "Data originale: ${originalDate ?: "null"}")

        // Se la data è null, usiamo "Data sconosciuta"
        if (originalDate == null) {
            "Data sconosciuta"
        } else {
            // Lista di formati di data da provare
            val parsers = listOf(
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            )

            var date: Date? = null
            var successfulFormat = ""

            // Pulire la stringa da possibili spazi o caratteri extra
            val cleanDateString = originalDate.trim()

            // Prova tutti i formati fino a trovare uno che funziona
            for (parser in parsers) {
                try {
                    date = parser.parse(cleanDateString)
                    successfulFormat = parser.toPattern()
                    Log.d("WorkoutPlanCard", "Formato data riconosciuto: $successfulFormat")
                    break
                } catch (e: Exception) {
                    // Continua con il prossimo parser
                    Log.d("WorkoutPlanCard", "Formato non riconosciuto: ${parser.toPattern()}")
                }
            }

            // Se è stata trovata una data valida, la formatta per la visualizzazione
            date?.let {
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                outputFormat.format(it)
            } ?: run {
                // Se non è stato trovato nessun formato valido, log dell'errore e valore di fallback
                Log.e("WorkoutPlanCard", "Impossibile interpretare la data: $cleanDateString")
                "Data sconosciuta"
            }
        }
    } catch (e: Exception) {
        // In caso di qualsiasi errore, log e valore di fallback
        Log.e("WorkoutPlanCard", "Errore durante il parsing della data: ${e.message}", e)
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

                // Pulsanti di azione migliorati - stile chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsante Esercizi con stile chip
                    ElevatedButton(
                        onClick = onExpandClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (isExpanded)
                                MaterialTheme.colorScheme.primaryContainer
                            else Color(0xFFE6E1FF),  // Colore lilla chiaro
                            contentColor = if (isExpanded)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else Indigo600
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Esercizi",
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Pulsante Modifica con stile chip
                    ElevatedButton(
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFFE8EAFF),  // Colore blu chiaro
                            contentColor = Indigo600
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Modifica",
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Pulsante Elimina con stile chip
                    ElevatedButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFFEF7070),  // Colore rosso molto chiaro
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Elimina",
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Sezione esercizi espandibile
            if (isExpanded) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(horizontal = 16.dp)
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

                            HorizontalDivider(
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