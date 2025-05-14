package com.fitgymtrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitgymtrack.app.models.WorkoutExercise

/**
 * Componente per modificare i dettagli di un esercizio all'interno di una scheda
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExerciseEditor(
    exercise: WorkoutExercise,
    onUpdate: (WorkoutExercise) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    // Stati locali per i valori dell'esercizio
    var series by remember { mutableStateOf(exercise.serie.toString()) }
    var reps by remember { mutableStateOf(exercise.ripetizioni.toString()) }
    var weight by remember { mutableStateOf(exercise.peso.toString()) }
    var restTime by remember { mutableStateOf(exercise.tempoRecupero.toString()) }
    var setType by remember { mutableStateOf(exercise.setType) }
    var linkedToPrevious by remember { mutableStateOf(exercise.linkedToPrevious) }
    var notes by remember { mutableStateOf(exercise.note ?: "") }

    // Effetto per aggiornare i valori quando l'esercizio cambia
    LaunchedEffect(exercise) {
        series = exercise.serie.toString()
        reps = exercise.ripetizioni.toString()
        weight = exercise.peso.toString()
        restTime = exercise.tempoRecupero.toString()
        setType = exercise.setType
        linkedToPrevious = exercise.linkedToPrevious
        notes = exercise.note ?: ""
    }

    // Lista di tipi di serie disponibili
    val setTypes = listOf(
        "normal" to "Normale",
        "superset" to "Superset",
        "dropset" to "Dropset",
        "circuit" to "Circuito",
        "giant_set" to "Giant Set"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Header dell'esercizio
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when (setType) {
                        "superset" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        "dropset" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        "circuit", "giant_set" -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        else -> Color.Transparent
                    }
                )
        ) {
            // Intestazione con nome e controlli
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = exercise.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (exercise.gruppoMuscolare != null) {
                        Text(
                            text = exercise.gruppoMuscolare,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Badge per esercizio collegato
                    if (linkedToPrevious) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Collegato al precedente",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Pulsanti controllo
                Row {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Sposta su",
                            tint = if (isFirst)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isLast
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Sposta giù",
                            tint = if (isLast)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { expanded = !expanded }
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Chiudi" else "Espandi"
                        )
                    }

                    IconButton(
                        onClick = onDelete
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Elimina",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Dettagli dell'esercizio
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Prima riga di campi: Serie e Ripetizioni
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Campo Serie
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Serie *",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = series,
                                onValueChange = {
                                    series = it
                                    // Aggiorna l'esercizio dopo la modifica
                                    onUpdate(
                                        exercise.copy(
                                            serie = it.toIntOrNull() ?: exercise.serie
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                singleLine = true
                            )
                        }

                        // Campo Ripetizioni
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (exercise.isIsometric) "Secondi *" else "Ripetizioni *",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = reps,
                                onValueChange = {
                                    reps = it
                                    // Aggiorna l'esercizio dopo la modifica
                                    onUpdate(
                                        exercise.copy(
                                            ripetizioni = it.toIntOrNull() ?: exercise.ripetizioni
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seconda riga di campi: Peso e Tempo recupero
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Campo Peso
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Peso (kg)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = weight,
                                onValueChange = {
                                    weight = it
                                    // Aggiorna l'esercizio dopo la modifica
                                    onUpdate(
                                        exercise.copy(
                                            peso = it.toDoubleOrNull() ?: exercise.peso
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                ),
                                singleLine = true
                            )
                        }

                        // Campo Tempo recupero
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recupero (sec)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = restTime,
                                onValueChange = {
                                    restTime = it
                                    // Aggiorna l'esercizio dopo la modifica
                                    onUpdate(
                                        exercise.copy(
                                            tempoRecupero = it.toIntOrNull() ?: exercise.tempoRecupero
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Terza riga: Tipo di serie
                    Column {
                        Text(
                            text = "Tipo di serie",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Simple dropdown replacement
                        var showDropdown by remember { mutableStateOf(false) }

                        Box {
                            OutlinedTextField(
                                value = setTypes.find { it.first == setType }?.second ?: "Normale",
                                onValueChange = { },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { showDropdown = !showDropdown }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Seleziona tipo"
                                        )
                                    }
                                }
                            )

                            DropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { showDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                setTypes.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            setType = value
                                            showDropdown = false
                                            onUpdate(
                                                exercise.copy(
                                                    setType = value,
                                                    // Se il tipo è normale, rimuovi il collegamento
                                                    linkedToPrevious = if (value == "normal") false else linkedToPrevious
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Opzione per collegare esercizi
                    if (setType != "normal" && !isFirst) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = linkedToPrevious,
                                onCheckedChange = { isChecked ->
                                    linkedToPrevious = isChecked
                                    onUpdate(
                                        exercise.copy(
                                            linkedToPrevious = isChecked
                                        )
                                    )
                                }
                            )
                            Text(
                                text = "Collega all'esercizio precedente",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Note
                    Column {
                        Text(
                            text = "Note",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = {
                                notes = it
                                onUpdate(
                                    exercise.copy(
                                        note = it.ifEmpty { null }
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}