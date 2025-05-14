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

    // Garantiamo che l'esercizio abbia sempre un setType valido
    val safeExercise = remember(exercise) {
        if (exercise.setType.isNullOrEmpty()) {
            exercise.copy(setType = "normal")
        } else {
            exercise
        }
    }

    // Stati locali per i valori dell'esercizio
    var series by remember { mutableStateOf(safeExercise.serie.toString()) }
    var reps by remember { mutableStateOf(safeExercise.ripetizioni.toString()) }
    var weight by remember { mutableStateOf(safeExercise.peso.toString()) }
    var restTime by remember { mutableStateOf(safeExercise.tempoRecupero.toString()) }
    var setType by remember { mutableStateOf(safeExercise.setType) }
    var linkedToPrevious by remember { mutableStateOf(safeExercise.linkedToPrevious) }
    var notes by remember { mutableStateOf(safeExercise.note ?: "") }

    // Effetto per aggiornare i valori quando l'esercizio cambia
    LaunchedEffect(safeExercise) {
        series = safeExercise.serie.toString()
        reps = safeExercise.ripetizioni.toString()
        weight = safeExercise.peso.toString()
        restTime = safeExercise.tempoRecupero.toString()
        setType = safeExercise.setType
        linkedToPrevious = safeExercise.linkedToPrevious
        notes = safeExercise.note ?: ""
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
                        text = safeExercise.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (safeExercise.gruppoMuscolare != null) {
                        Text(
                            text = safeExercise.gruppoMuscolare,
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
                                    // SICUREZZA NULL: Non includiamo setType nel copy se non stiamo modificando quel campo
                                    onUpdate(
                                        safeExercise.copy(
                                            serie = it.toIntOrNull() ?: safeExercise.serie
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
                                text = if (safeExercise.isIsometric) "Secondi *" else "Ripetizioni *",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = reps,
                                onValueChange = {
                                    reps = it
                                    // SICUREZZA NULL: Non includiamo setType nel copy se non stiamo modificando quel campo
                                    onUpdate(
                                        safeExercise.copy(
                                            ripetizioni = it.toIntOrNull() ?: safeExercise.ripetizioni
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
                                    // SICUREZZA NULL: Non includiamo setType nel copy se non stiamo modificando quel campo
                                    onUpdate(
                                        safeExercise.copy(
                                            peso = it.toDoubleOrNull() ?: safeExercise.peso
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
                                    // SICUREZZA NULL: Non includiamo setType nel copy se non stiamo modificando quel campo
                                    onUpdate(
                                        safeExercise.copy(
                                            tempoRecupero = it.toIntOrNull() ?: safeExercise.tempoRecupero
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
                            // Garantiamo che il valore sia sempre valido
                            val currentSetTypeLabel = setTypes.find { it.first == setType }?.second ?: "Normale"

                            OutlinedTextField(
                                value = currentSetTypeLabel,
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
                                            // Garantiamo che il valore non sia mai null
                                            val safeValue = value.ifEmpty { "normal" }
                                            setType = safeValue
                                            showDropdown = false

                                            // SICUREZZA NULL: Utilizziamo sempre un valore non-null
                                            onUpdate(
                                                safeExercise.copy(
                                                    setType = safeValue,
                                                    // Se il tipo è normale, rimuovi il collegamento
                                                    linkedToPrevious = if (safeValue == "normal") false else linkedToPrevious
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
                                    // SICUREZZA NULL: Garantiamo che setType non sia mai null, mantenendo il valore attuale
                                    onUpdate(
                                        safeExercise.copy(
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
                                // SICUREZZA NULL: Non includiamo setType nel copy se non stiamo modificando quel campo
                                onUpdate(
                                    safeExercise.copy(
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