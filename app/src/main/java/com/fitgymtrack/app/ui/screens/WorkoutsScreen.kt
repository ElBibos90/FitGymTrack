package com.fitgymtrack.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.WorkoutPlan
import com.fitgymtrack.app.ui.components.SnackbarMessage
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    onBack: () -> Unit,
    onStartWorkout: (Int) -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: WorkoutViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val workoutPlansState by viewModel.workoutPlansState.collectAsState()
    val workoutPlans by viewModel.workoutPlans.collectAsState()

    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isSnackbarSuccess by remember { mutableStateOf(true) }

    // Caricamento iniziale delle schede
    LaunchedEffect(Unit) {
        viewModel.loadWorkoutPlans(sessionManager)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Allenamenti") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Sezione Inizia Allenamento
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
                                        text = "Inizia un allenamento",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Seleziona una scheda per iniziare l'allenamento",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (workoutPlans.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = "Nessuna scheda",
                                                    modifier = Modifier.size(48.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "Non hai ancora creato schede di allenamento",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            workoutPlans.forEach { workoutPlan ->
                                                WorkoutPlanStartCard(
                                                    workoutPlan = workoutPlan,
                                                    onStartClick = { onStartWorkout(workoutPlan.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Sezione Storico Allenamenti
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
                                        text = "Storico Allenamenti",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Qui potremmo mostrare un'anteprima degli ultimi allenamenti
                                    // Per ora mostriamo solo un pulsante per visualizzare lo storico completo
                                    Button(
                                        onClick = onNavigateToHistory,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = "Storico"
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Visualizza storico completo")
                                    }
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

@Composable
fun WorkoutPlanStartCard(
    workoutPlan: WorkoutPlan,
    onStartClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    text = workoutPlan.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (!workoutPlan.descrizione.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = workoutPlan.descrizione,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onStartClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Inizia"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Inizia")
            }
        }
    }
}