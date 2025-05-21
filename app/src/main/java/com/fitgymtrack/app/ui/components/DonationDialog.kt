package com.fitgymtrack.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fitgymtrack.app.ui.theme.Indigo600

@Composable
fun DonationDialog(
    onDismiss: () -> Unit,
    onDonate: (amount: Double, message: String, showName: Boolean) -> Unit
) {
    var selectedAmount by remember { mutableStateOf(5.0) }
    var customAmount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showName by remember { mutableStateOf(true) }

    // Usa il valore personalizzato se inserito, altrimenti usa quello selezionato
    val finalAmount = if (customAmount.isNotEmpty()) {
        customAmount.toDoubleOrNull() ?: selectedAmount
    } else {
        selectedAmount
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Titolo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fai una donazione",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Importo (€)
                Text(
                    text = "Importo (€)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Opzioni di importo predefinite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Applicare il modificatore weight nel contesto di RowScope
                    Button(
                        onClick = {
                            selectedAmount = 2.0
                            customAmount = ""
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedAmount == 2.0 && customAmount.isEmpty())
                                Indigo600 else Color.LightGray.copy(alpha = 0.3f),
                            contentColor = if (selectedAmount == 2.0 && customAmount.isEmpty())
                                Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Text(text = "€2")
                    }

                    Button(
                        onClick = {
                            selectedAmount = 5.0
                            customAmount = ""
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedAmount == 5.0 && customAmount.isEmpty())
                                Indigo600 else Color.LightGray.copy(alpha = 0.3f),
                            contentColor = if (selectedAmount == 5.0 && customAmount.isEmpty())
                                Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Text(text = "€5")
                    }

                    Button(
                        onClick = {
                            selectedAmount = 10.0
                            customAmount = ""
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedAmount == 10.0 && customAmount.isEmpty())
                                Indigo600 else Color.LightGray.copy(alpha = 0.3f),
                            contentColor = if (selectedAmount == 10.0 && customAmount.isEmpty())
                                Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Text(text = "€10")
                    }

                    Button(
                        onClick = {
                            selectedAmount = 20.0
                            customAmount = ""
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedAmount == 20.0 && customAmount.isEmpty())
                                Indigo600 else Color.LightGray.copy(alpha = 0.3f),
                            contentColor = if (selectedAmount == 20.0 && customAmount.isEmpty())
                                Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        Text(text = "€20")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Campo personalizzato
                OutlinedTextField(
                    value = customAmount,
                    onValueChange = {
                        // Accetta solo numeri con al massimo un punto decimale
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            customAmount = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("€") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Indigo600,
                        cursorColor = Indigo600
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Messaggio opzionale
                Text(
                    text = "Messaggio (opzionale)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("Lascia un messaggio di supporto...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Indigo600,
                        cursorColor = Indigo600
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Checkbox per mostrare il nome
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showName,
                        onCheckedChange = { showName = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Indigo600
                        )
                    )

                    Text(
                        text = "Mostra il mio nome nella lista dei sostenitori",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pulsante Dona
                Button(
                    onClick = {
                        if (finalAmount > 0) {
                            onDonate(finalAmount, message, showName)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = finalAmount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEC4899)
                    )
                ) {
                    Text(
                        text = "Dona",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}