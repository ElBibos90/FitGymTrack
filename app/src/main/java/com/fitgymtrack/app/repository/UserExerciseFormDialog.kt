package com.fitgymtrack.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fitgymtrack.app.models.UserExercise
import com.fitgymtrack.app.viewmodel.UserExerciseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserExerciseFormDialog(
    exercise: UserExercise? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String?, Boolean) -> Unit,
    viewModel: UserExerciseViewModel,
    onSuccess: () -> Unit = {} // Parametro aggiunto per notificare il successo
) {
    val isEditing = exercise != null

    // Stato del form
    var nome by remember { mutableStateOf(exercise?.nome ?: "") }
    var gruppoMuscolare by remember { mutableStateOf(exercise?.gruppoMuscolare ?: "") }
    var descrizione by remember { mutableStateOf(exercise?.descrizione ?: "") }
    var attrezzatura by remember { mutableStateOf(exercise?.attrezzatura ?: "") }
    var isIsometric by remember { mutableStateOf(exercise?.isIsometric == true) }

    // Raccogli gli stati delle operazioni
    val createState by viewModel.createState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    // Determina lo stato corrente in base all'operazione
    val currentState = if (isEditing) updateState else createState

    // Effetto per gestire il successo delle operazioni
    LaunchedEffect(key1 = currentState) {
        if (currentState is UserExerciseViewModel.OperationState.Success) {
            // Se l'operazione ha avuto successo, chiama onSuccess invece di onDismiss
            onSuccess()

            // Resetta lo stato
            if (isEditing) {
                viewModel.resetUpdateState()
            } else {
                viewModel.resetCreateState()
            }
        }
    }

    // Lista di gruppi muscolari disponibili
    val gruppiMuscolari = listOf(
        "Petto", "Schiena", "Spalle", "Tricipiti", "Bicipiti",
        "Quadricipiti", "Femorali", "Polpacci", "Addominali",
        "Glutei", "Avambracci", "Cardio", "Altro"
    )

    // Stato per mostrare il dropdown
    var showGruppiDropdown by remember { mutableStateOf(false) }

    // Errori di validazione
    var nomeError by remember { mutableStateOf("") }
    var gruppoError by remember { mutableStateOf("") }

    // Validazione
    fun validateForm(): Boolean {
        var isValid = true

        if (nome.isBlank()) {
            nomeError = "Il nome è obbligatorio"
            isValid = false
        } else {
            nomeError = ""
        }

        if (gruppoMuscolare.isBlank()) {
            gruppoError = "Il gruppo muscolare è obbligatorio"
            isValid = false
        } else {
            gruppoError = ""
        }

        return isValid
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header
                TopAppBar(
                    title = {
                        Text(if (isEditing) "Modifica esercizio" else "Nuovo esercizio")
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi"
                            )
                        }
                    }
                )

                // Form content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Messaggio di errore dell'operazione
                    if (currentState is UserExerciseViewModel.OperationState.Error) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = (currentState as UserExerciseViewModel.OperationState.Error).message,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Nome esercizio
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Nome esercizio *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = nome,
                            onValueChange = { nome = it; nomeError = "" },
                            modifier = Modifier.fillMaxWidth(),
                            isError = nomeError.isNotEmpty(),
                            supportingText = {
                                if (nomeError.isNotEmpty()) {
                                    Text(text = nomeError, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            placeholder = { Text("Nome dell'esercizio") },
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gruppo muscolare
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Gruppo muscolare *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Customized ExposedDropdownMenuBox
                        Box {
                            OutlinedTextField(
                                value = gruppoMuscolare,
                                onValueChange = { /* Readonly */ },
                                modifier = Modifier.fillMaxWidth(),
                                isError = gruppoError.isNotEmpty(),
                                supportingText = {
                                    if (gruppoError.isNotEmpty()) {
                                        Text(text = gruppoError, color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                placeholder = { Text("Seleziona gruppo muscolare") },
                                trailingIcon = {
                                    IconButton(onClick = { showGruppiDropdown = true }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Seleziona"
                                        )
                                    }
                                },
                                readOnly = true,
                                singleLine = true
                            )

                            DropdownMenu(
                                expanded = showGruppiDropdown,
                                onDismissRequest = { showGruppiDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                gruppiMuscolari.forEach { gruppo ->
                                    DropdownMenuItem(
                                        text = { Text(gruppo) },
                                        onClick = {
                                            gruppoMuscolare = gruppo
                                            gruppoError = ""
                                            showGruppiDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Descrizione
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Descrizione",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = descrizione,
                            onValueChange = { descrizione = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Descrivi l'esercizio (opzionale)") },
                            minLines = 3
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Attrezzatura
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Attrezzatura",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = attrezzatura,
                            onValueChange = { attrezzatura = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Es. manubri, bilanciere, macchina (opzionale)") },
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Checkbox isometrico
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isIsometric,
                                onClick = { isIsometric = !isIsometric }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isIsometric,
                            onCheckedChange = { isIsometric = it }
                        )
                        Text(
                            text = "Esercizio isometrico",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // Info sull'esercizio isometrico
                    if (isIsometric) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Gli esercizi isometrici mantengono una posizione statica. In fase di allenamento, le 'ripetizioni' rappresenteranno i secondi da mantenere la posizione.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Pulsanti footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsante annulla
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annulla")
                    }

                    // Pulsante salva
                    Button(
                        onClick = {
                            if (validateForm()) {
                                onSave(nome, gruppoMuscolare, descrizione, attrezzatura, isIsometric)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = currentState !is UserExerciseViewModel.OperationState.Loading
                    ) {
                        if (currentState is UserExerciseViewModel.OperationState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (isEditing) "Aggiorna" else "Salva")
                        }
                    }
                }
            }
        }
    }
}