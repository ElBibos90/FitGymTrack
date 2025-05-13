package com.fitgymtrack.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.UserProfile
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.viewmodel.UserProfileViewModel

@Composable
fun UserProfileScreen(
    navigateBack: () -> Unit,
    viewModel: UserProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val profileState by viewModel.profileState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    // Stato locale del form
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("male") }
    var experienceLevel by remember { mutableStateOf("beginner") }
    var fitnessGoals by remember { mutableStateOf("general_fitness") }
    var injuries by remember { mutableStateOf("") }
    var preferences by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Carica i dati all'avvio della schermata
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    // Aggiorna i campi del form quando arrivano i dati del profilo
    LaunchedEffect(userProfile) {
        userProfile?.let { profile ->
            height = profile.height?.toString() ?: "175"
            weight = profile.weight?.toString() ?: "70"
            age = profile.age?.toString() ?: "30"
            gender = profile.gender ?: "male"
            experienceLevel = profile.experienceLevel ?: "beginner"
            fitnessGoals = profile.fitnessGoals ?: "general_fitness"
            injuries = profile.injuries ?: ""
            preferences = profile.preferences ?: ""
            notes = profile.notes ?: ""
        }
    }

    // Funzione per salvare il profilo
    val saveProfile = {
        val updatedProfile = UserProfile(
            height = height.toIntOrNull() ?: 175,
            weight = weight.toDoubleOrNull() ?: 70.0,
            age = age.toIntOrNull() ?: 30,
            gender = gender,
            experienceLevel = experienceLevel,
            fitnessGoals = fitnessGoals,
            injuries = injuries.ifBlank { null },
            preferences = preferences.ifBlank { null },
            notes = notes.ifBlank { null }
        )
        viewModel.updateUserProfile(updatedProfile)
    }

    // Mostra toast di successo quando il profilo viene salvato
    LaunchedEffect(profileState) {
        if (profileState is UserProfileViewModel.ProfileState.Success) {
            Toast.makeText(context, "Profilo aggiornato con successo", Toast.LENGTH_SHORT).show()
        } else if (profileState is UserProfileViewModel.ProfileState.Error) {
            val errorMessage = (profileState as UserProfileViewModel.ProfileState.Error).message
            Toast.makeText(context, "Errore: $errorMessage", Toast.LENGTH_LONG).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header con titolo e pulsante indietro
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profilo Utente",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = navigateBack,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Indietro",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Indietro")
                    }
                }
            }

            // Contenuto principale
            when (profileState) {
                is UserProfileViewModel.ProfileState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Indigo600)
                    }
                }

                is UserProfileViewModel.ProfileState.Error -> {
                    val error = (profileState as UserProfileViewModel.ProfileState.Error).message
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Errore: $error",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadUserProfile() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Indigo600
                                )
                            ) {
                                Text("Riprova")
                            }
                        }
                    }
                }

                else -> {
                    // Form principale
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Sezione Informazioni Personali
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Intestazione sezione
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Informazioni Personali",
                                        tint = Indigo600,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Informazioni Personali",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Altezza
                                OutlinedTextField(
                                    value = height,
                                    onValueChange = { height = it },
                                    label = { Text("Altezza (cm)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    singleLine = true
                                )

                                // Peso
                                OutlinedTextField(
                                    value = weight,
                                    onValueChange = { weight = it },
                                    label = { Text("Peso (kg)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    singleLine = true
                                )

                                // Età
                                OutlinedTextField(
                                    value = age,
                                    onValueChange = { age = it },
                                    label = { Text("Età") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    singleLine = true
                                )

                                // Genere
                                Text(
                                    text = "Genere",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                RadioButtonGroup(
                                    options = listOf("male", "female", "other"),
                                    selectedOption = gender,
                                    onOptionSelected = { gender = it },
                                    labels = mapOf(
                                        "male" to "Maschile",
                                        "female" to "Femminile",
                                        "other" to "Altro"
                                    )
                                )
                            }
                        }

                        // Sezione Esperienza Fitness
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Intestazione sezione
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = "Esperienza Fitness",
                                        tint = Indigo600,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Esperienza Fitness",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Livello di esperienza
                                Text(
                                    text = "Livello di esperienza",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                RadioButtonGroup(
                                    options = listOf("beginner", "intermediate", "advanced"),
                                    selectedOption = experienceLevel,
                                    onOptionSelected = { experienceLevel = it },
                                    labels = mapOf(
                                        "beginner" to "Principiante",
                                        "intermediate" to "Intermedio",
                                        "advanced" to "Avanzato"
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Obiettivi Fitness
                                ExposedDropdownMenuBoxField(
                                    label = "Obiettivi Fitness",
                                    options = listOf(
                                        "general_fitness" to "Fitness Generale",
                                        "muscle_gain" to "Aumento Massa Muscolare",
                                        "strength" to "Forza",
                                        "weight_loss" to "Perdita di Peso",
                                        "endurance" to "Resistenza",
                                        "flexibility" to "Flessibilità"
                                    ),
                                    selectedValue = fitnessGoals,
                                    onValueSelected = { fitnessGoals = it }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Preferenze di allenamento
                                OutlinedTextField(
                                    value = preferences,
                                    onValueChange = { preferences = it },
                                    label = { Text("Preferenze di allenamento") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 5
                                )
                            }
                        }

                        // Sezione Salute e Infortuni
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Intestazione sezione
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Salute e Infortuni",
                                        tint = Color.Red,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Salute e Infortuni",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Campo infortuni
                                OutlinedTextField(
                                    value = injuries,
                                    onValueChange = { injuries = it },
                                    label = { Text("Infortuni o limitazioni") },
                                    placeholder = { Text("Descrivi eventuali problemi di salute o infortuni") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 5
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Queste informazioni aiuteranno l'app a personalizzare meglio i tuoi allenamenti",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Sezione Note
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Intestazione sezione
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmarks,
                                        contentDescription = "Note",
                                        tint = Indigo600,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Note",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Campo note
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("Note aggiuntive") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 5
                                )
                            }
                        }

                        // Pulsante Salva
                        Button(
                            onClick = saveProfile,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo600
                            ),
                            enabled = profileState !is UserProfileViewModel.ProfileState.Saving
                        ) {
                            if (profileState is UserProfileViewModel.ProfileState.Saving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "Salva",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Salva Profilo",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RadioButtonGroup(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    labels: Map<String, String>
) {
    Column {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Indigo600
                    )
                )
                Text(
                    text = labels[option] ?: option,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxField(
    label: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onValueSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selectedValue }?.second ?: options.firstOrNull()?.second ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onValueSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}