package com.fitgymtrack.app.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fitgymtrack.app.api.ExerciseItem
import com.fitgymtrack.app.models.User
import com.fitgymtrack.app.ui.screens.UserExerciseFormDialog
import com.fitgymtrack.app.viewmodel.UserExerciseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectionDialog(
    exercises: List<ExerciseItem>,
    selectedExerciseIds: List<Int>, // Tracks exercises already selected
    isLoading: Boolean,
    onExerciseSelected: (ExerciseItem) -> Unit,
    onDismissRequest: () -> Unit,
    onExercisesRefresh: () -> Unit, // Nuova callback per aggiornare la lista dopo creazione
    currentUser: User? = null, // Utente corrente per la creazione dell'esercizio
    userExerciseViewModel: UserExerciseViewModel? = null // ViewModel per la gestione degli esercizi personalizzati
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("") }
    var filteredExercises by remember { mutableStateOf(exercises) }

    // State for muscle group filter dialog
    var showGroupFilterDialog by remember { mutableStateOf(false) }

    // State for showing the create exercise dialog
    var showCreateExerciseDialog by remember { mutableStateOf(false) }

    // Extract unique muscle groups from exercise list
    val muscleGroups = remember(exercises) {
        exercises.mapNotNull { it.gruppo_muscolare }
            .filter { it.isNotBlank() }
            .toSet()
            .sorted()
    }

    // Filter exercises when search query or muscle group changes
    LaunchedEffect(searchQuery, selectedGroup, exercises) {
        filteredExercises = exercises.filter { exercise ->
            // Filter by search text
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                exercise.nome.contains(searchQuery, ignoreCase = true) ||
                        (exercise.gruppo_muscolare?.contains(searchQuery, ignoreCase = true) == true)
            }

            // Filter by muscle group
            val matchesGroup = if (selectedGroup.isBlank()) {
                true
            } else {
                exercise.gruppo_muscolare == selectedGroup
            }

            matchesQuery && matchesGroup
        }
    }

    // Dialog for selecting muscle group
    if (showGroupFilterDialog) {
        AlertDialog(
            onDismissRequest = { showGroupFilterDialog = false },
            title = { Text("Seleziona gruppo muscolare") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "Tutti i gruppi",
                                    fontWeight = if (selectedGroup.isEmpty()) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.clickable {
                                selectedGroup = ""
                                showGroupFilterDialog = false
                            }
                        )
                        Divider()
                    }

                    items(muscleGroups) { group ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = group,
                                    fontWeight = if (selectedGroup == group) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.clickable {
                                selectedGroup = group
                                showGroupFilterDialog = false
                            }
                        )
                        Divider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupFilterDialog = false }) {
                    Text("Chiudi")
                }
            }
        )
    }

// Dialog di creazione esercizio personalizzato
    if (showCreateExerciseDialog && userExerciseViewModel != null && currentUser != null) {
        UserExerciseFormDialog(
            exercise = null, // Nuovo esercizio
            onDismiss = { showCreateExerciseDialog = false },
            onSave = { nome, gruppoMuscolare, descrizione, attrezzatura, isIsometric ->
                // Crea il nuovo esercizio
                userExerciseViewModel.createExercise(
                    nome = nome,
                    gruppoMuscolare = gruppoMuscolare,
                    descrizione = descrizione,
                    attrezzatura = attrezzatura,
                    isIsometric = isIsometric,
                    userId = currentUser.id
                )
                // Non chiudiamo ancora il dialog, aspettiamo che l'operazione sia completata
            },
            viewModel = userExerciseViewModel,
            onSuccess = {
                // Quando l'esercizio è stato creato con successo
                showCreateExerciseDialog = false
                // Aggiorna immediatamente la lista degli esercizi
                onExercisesRefresh()
            }
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with title and close button
                TopAppBar(
                    title = { Text("Seleziona esercizi") },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi"
                            )
                        }
                    },
                    actions = {
                        // Pulsante per creare un nuovo esercizio
                        if (userExerciseViewModel != null && currentUser != null) {
                            IconButton(onClick = { showCreateExerciseDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Crea nuovo esercizio"
                                )
                            }
                        }
                    }
                )

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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

                // Filter by muscle group - clickable button approach
                Button(
                    onClick = { showGroupFilterDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedGroup.isEmpty()) "Filtra per gruppo muscolare" else selectedGroup,
                            modifier = Modifier.weight(1f),
                            color = if (selectedGroup.isEmpty())
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Seleziona gruppo muscolare"
                        )
                    }
                }

                // Chip to show active filter and allow removal
                if (selectedGroup.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.clickable { selectedGroup = "" },
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    start = 12.dp,
                                    end = 8.dp,
                                    top = 6.dp,
                                    bottom = 6.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedGroup,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Rimuovi filtro",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Pulsante "Crea nuovo esercizio" per schermi più piccoli (sempre visibile)
                if (userExerciseViewModel != null && currentUser != null) {
                    Button(
                        onClick = { showCreateExerciseDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Crea nuovo esercizio")
                        }
                    }
                }

                // Exercise list or loading state
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (filteredExercises.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedGroup.isNotBlank())
                                    "Nessun esercizio trovato con i filtri applicati"
                                else
                                    "Nessun esercizio disponibile",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (userExerciseViewModel != null && currentUser != null && searchQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        // Pre-popola il form con la query di ricerca come nome
                                        showCreateExerciseDialog = true
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Crea \"$searchQuery\"")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredExercises) { exercise ->
                            val isSelected = selectedExerciseIds.contains(exercise.id)

                            ExerciseItem(
                                exercise = exercise,
                                isSelected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        onExerciseSelected(exercise)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseItem(
    exercise: ExerciseItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = exercise.nome,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!exercise.gruppo_muscolare.isNullOrBlank()) {
                    Text(
                        text = exercise.gruppo_muscolare,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!exercise.attrezzatura.isNullOrBlank()) {
                    Text(
                        text = exercise.attrezzatura,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelected) {
                // Icon to indicate the exercise is already selected
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Già aggiunto",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                // Icon to add the exercise
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aggiungi",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}