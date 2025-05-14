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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectionDialog(
    exercises: List<ExerciseItem>,
    selectedExerciseIds: List<Int>, // Nuova proprietà per tracciare gli esercizi già selezionati
    isLoading: Boolean,
    onExerciseSelected: (ExerciseItem) -> Unit,
    onDismissRequest: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("") }
    var filteredExercises by remember { mutableStateOf(exercises) }

    // Stato per il dialogo del filtro gruppi muscolari
    var showGroupFilterDialog by remember { mutableStateOf(false) }

    // Estrai i gruppi muscolari unici dalla lista di esercizi
    val muscleGroups = remember(exercises) {
        exercises.mapNotNull { it.gruppo_muscolare }
            .filter { it.isNotBlank() }
            .toSet()
            .sorted()
    }

    // Filtra esercizi quando cambia la query di ricerca o il gruppo muscolare
    LaunchedEffect(searchQuery, selectedGroup, exercises) {
        filteredExercises = exercises.filter { exercise ->
            // Filtra per testo di ricerca
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                exercise.nome.contains(searchQuery, ignoreCase = true) ||
                        (exercise.gruppo_muscolare?.contains(searchQuery, ignoreCase = true) == true)
            }

            // Filtra per gruppo muscolare
            val matchesGroup = if (selectedGroup.isBlank()) {
                true
            } else {
                exercise.gruppo_muscolare == selectedGroup
            }

            matchesQuery && matchesGroup
        }
    }

    // Dialog per la selezione del gruppo muscolare
    if (showGroupFilterDialog) {
        AlertDialog(
            onDismissRequest = { showGroupFilterDialog = false },
            title = { Text("Seleziona gruppo muscolare") },
            text = {
                LazyColumn {
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
            confirmButton = { }
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
                // Header con titolo e chiusura
                TopAppBar(
                    title = { Text("Seleziona esercizi") },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi"
                            )
                        }
                    }
                )

                // Barra di ricerca
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

                // Filtro per gruppo muscolare
                OutlinedTextField(
                    value = if (selectedGroup.isEmpty()) "" else selectedGroup,
                    onValueChange = { /* Controllato manualmente */ },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Seleziona gruppo muscolare"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { showGroupFilterDialog = true },
                    placeholder = { Text("Filtra per gruppo muscolare") },
                    label = { Text("Gruppo muscolare") }
                )

                // Chip per mostrare il filtro attivo e permettere di rimuoverlo
                if (selectedGroup.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuggestionChip(
                            onClick = { selectedGroup = "" },
                            label = { Text(selectedGroup) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Rimuovi filtro",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                // Lista esercizi o stato di caricamento
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
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedGroup.isNotBlank())
                                "Nessun esercizio trovato con i filtri applicati"
                            else
                                "Nessun esercizio disponibile",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                // Icona per indicare che l'esercizio è già stato selezionato
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Già aggiunto",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                // Icona per aggiungere l'esercizio
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aggiungi",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}