package com.fitgymtrack.app.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.UserExercise
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.UserExerciseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserExerciseScreen(
    onBack: () -> Unit,
    viewModel: UserExerciseViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var user by remember { mutableStateOf<com.fitgymtrack.app.models.User?>(null) }
    val exercisesState by viewModel.exercisesState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val exercises by viewModel.exercises.collectAsState()

    // Stato per il filtro di ricerca
    var searchQuery by remember { mutableStateOf("") }

    // Stato per il dialogo di conferma eliminazione
    var confirmDeleteExerciseId by remember { mutableStateOf<Int?>(null) }

    // Stato per il dialogo di aggiunta/modifica esercizio
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<UserExercise?>(null) }

    // Ottieni l'utente corrente
    LaunchedEffect(key1 = Unit) {
        sessionManager.getUserData().collect { userData ->
            user = userData
            userData?.id?.let { userId ->
                viewModel.loadUserExercises(userId)
            }
        }
    }

    // Effetto per gestire gli stati delle operazioni
    LaunchedEffect(key1 = deleteState) {
        if (deleteState is UserExerciseViewModel.OperationState.Success) {
            // Qui potremmo mostrare un messaggio di successo toast
            viewModel.resetDeleteState()
        }
    }

    // Filtra gli esercizi in base alla ricerca
    val filteredExercises = exercises.filter { exercise ->
        exercise.nome.contains(searchQuery, ignoreCase = true) ||
                exercise.gruppoMuscolare.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Esercizi Personalizzati") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddExerciseDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aggiungi esercizio"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Informazioni
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Informazioni sugli esercizi personalizzati",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Puoi creare e gestire i tuoi esercizi personalizzati\n" +
                                    "• Gli esercizi creati sono in attesa di approvazione\n" +
                                    "• Gli esercizi approvati saranno disponibili per tutti gli utenti",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Campo di ricerca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Cerca esercizi...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cerca"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Cancella"
                            )
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stato del caricamento
            when (exercisesState) {
                is UserExerciseViewModel.ExercisesState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UserExerciseViewModel.ExercisesState.Error -> {
                    val error = (exercisesState as UserExerciseViewModel.ExercisesState.Error).message
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Errore",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Errore nel caricamento degli esercizi",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    user?.id?.let { userId ->
                                        viewModel.loadUserExercises(userId)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Riprova",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Riprova")
                            }
                        }
                    }
                }
                else -> {
                    // Lista esercizi o messaggio di nessun esercizio
                    if (filteredExercises.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (searchQuery.isNotEmpty()) {
                                    // Nessun risultato per la ricerca
                                    Text(
                                        text = "Nessun esercizio corrisponde alla ricerca",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { searchQuery = "" }
                                    ) {
                                        Text("Cancella ricerca")
                                    }
                                } else if (exercises.isEmpty()) {
                                    // Nessun esercizio creato
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Non hai ancora creato esercizi personalizzati",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showAddExerciseDialog = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Aggiungi",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Crea il tuo primo esercizio")
                                    }
                                }
                            }
                        }
                    } else {
                        // Lista degli esercizi
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredExercises) { exercise ->
                                ExerciseCard(
                                    exercise = exercise,
                                    onEdit = {
                                        editingExercise = exercise
                                        showAddExerciseDialog = true
                                    },
                                    onDelete = {
                                        confirmDeleteExerciseId = exercise.id
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog di conferma eliminazione
    if (confirmDeleteExerciseId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteExerciseId = null },
            title = { Text("Conferma eliminazione") },
            text = { Text("Sei sicuro di voler eliminare questo esercizio? Questa azione non può essere annullata.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteExerciseId?.let { exerciseId ->
                            user?.id?.let { userId ->
                                coroutineScope.launch {
                                    viewModel.deleteExercise(exerciseId, userId)
                                    confirmDeleteExerciseId = null
                                }
                            }
                        }
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
                    onClick = { confirmDeleteExerciseId = null }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    // Dialog per aggiungere/modificare esercizio
    if (showAddExerciseDialog) {
        UserExerciseFormDialog(
            exercise = editingExercise,
            onDismiss = {
                showAddExerciseDialog = false
                editingExercise = null
            },
            onSave = { nome, gruppoMuscolare, descrizione, attrezzatura, isIsometric ->
                user?.id?.let { userId ->
                    if (editingExercise != null) {
                        // Aggiorna esercizio esistente
                        viewModel.updateExercise(
                            id = editingExercise!!.id,
                            nome = nome,
                            gruppoMuscolare = gruppoMuscolare,
                            descrizione = descrizione,
                            attrezzatura = attrezzatura,
                            isIsometric = isIsometric,
                            userId = userId
                        )
                    } else {
                        // Crea nuovo esercizio
                        viewModel.createExercise(
                            nome = nome,
                            gruppoMuscolare = gruppoMuscolare,
                            descrizione = descrizione,
                            attrezzatura = attrezzatura,
                            isIsometric = isIsometric,
                            userId = userId
                        )
                    }
                    showAddExerciseDialog = false
                    editingExercise = null
                }
            },
            viewModel = viewModel
        )
    }
}

@Composable
fun ExerciseCard(
    exercise: UserExercise,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Nome e tag esercizio
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = exercise.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge gruppo muscolare
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = exercise.gruppoMuscolare,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Badge stato approvazione
                        StatusBadge(status = exercise.status)

                        // Badge isometrico (se applicabile)
                        if (exercise.isIsometric) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "Isometrico",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                // Pulsanti azioni
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifica",
                            tint = Indigo600
                        )
                    }

                    IconButton(
                        onClick = onDelete,
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

            // Descrizione (se presente)
            if (!exercise.descrizione.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = exercise.descrizione,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Attrezzatura (se presente)
            if (!exercise.attrezzatura.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attrezzatura:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = exercise.attrezzatura,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "approved" -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            "Approvato"
        )
        "pending_review" -> Pair(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            "In revisione"
        )
        "user_only" -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
            "Privato"
        )
        else -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            "Sconosciuto"
        )
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}