package com.fitgymtrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.utils.WeightFormatter

/**
 * Dialog semplificato per la selezione del peso con pulsanti + e -
 */
@Composable
fun WeightPickerDialog(
    initialWeight: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    // Separate il peso in intero e frazione
    val initialWhole = initialWeight.toInt()
    val initialFractionIndex = getFractionIndex(initialWeight - initialWhole)

    var wholeNumber by remember { mutableStateOf(initialWhole) }
    var fractionIndex by remember { mutableStateOf(initialFractionIndex) }

    // Lista delle frazioni disponibili e delle etichette
    val fractions = listOf(0f, 0.125f, 0.25f, 0.375f, 0.5f, 0.625f, 0.75f, 0.875f)
    val fractionLabels = listOf("0", "125", "25", "375", "5", "625", "75", "875")

    // Calcola il peso totale
    val totalWeight = wholeNumber + fractions[fractionIndex]

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seleziona il peso",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Numeri interi
                NumberSelectorWithButtons(
                    label = "Kg",
                    value = wholeNumber,
                    onIncrement = { if (wholeNumber < 200) wholeNumber++ },
                    onDecrement = { if (wholeNumber > 0) wholeNumber-- },
                    valueWidth = 120.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Frazioni
                NumberSelectorWithButtons(
                    label = "Frazione",
                    value = fractionLabels[fractionIndex],
                    onIncrement = { if (fractionIndex < fractions.lastIndex) fractionIndex++ },
                    onDecrement = { if (fractionIndex > 0) fractionIndex-- },
                    valueWidth = 120.dp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Visualizzazione peso totale
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Peso selezionato: ${WeightFormatter.formatWeight(totalWeight)} kg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annulla")
                    }

                    Button(
                        onClick = { onConfirm(totalWeight) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Indigo600
                        )
                    ) {
                        Text("Conferma")
                    }
                }
            }
        }
    }
}

/**
 * Componente per selezionare un numero con pulsanti + e -
 */
@Composable
fun NumberSelectorWithButtons(
    label: String,
    value: Any,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    valueWidth: androidx.compose.ui.unit.Dp = 80.dp
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decrement button
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = Indigo600,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Diminuisci",
                    tint = Indigo600
                )
            }

            // Value display - Increased width for larger numbers
            Box(
                modifier = Modifier
                    .width(valueWidth)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 30.sp,
                        lineHeight = 36.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Indigo600,
                    maxLines = 1
                )
            }

            // Increment button
            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = Indigo600,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aumenta",
                    tint = Indigo600
                )
            }
        }
    }
}

/**
 * Dialog semplificato per la selezione delle ripetizioni con pulsanti + e -
 */
@Composable
fun RepsPickerDialog(
    initialReps: Int,
    isIsometric: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var reps by remember { mutableStateOf(initialReps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isIsometric) "Seleziona secondi" else "Seleziona ripetizioni",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Selettore numerico con bottoni + e -
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bottone -
                    FilledIconButton(
                        onClick = {
                            if (reps > 1) reps--
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Diminuisci"
                        )
                    }

                    // Valore numerico - allarghiamo lo spazio per le 3 cifre
                    Text(
                        text = reps.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            // Assicuriamo uno spazio minimo sufficiente per 3 cifre
                            .widthIn(min = 90.dp),
                        textAlign = TextAlign.Center
                    )

                    // Bottone +
                    FilledIconButton(
                        onClick = { reps++ },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aumenta"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Valori comuni
                Text(
                    text = "Valori comuni:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )

                // Lista di valori comuni come nel design originale
                val commonValues = if (isIsometric) {
                    listOf(10, 20, 30, 45, 60, 90, 120)
                } else {
                    listOf(8, 10, 12, 15, 20, 25, 30)
                }

                // Mostra i valori comuni in formato lista verticale (stile originale)
                commonValues.forEach { value ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .height(48.dp),
                        color = if (reps == value)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        onClick = { reps = value }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (reps == value)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reps) }
            ) {
                Text("Conferma")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Annulla")
            }
        }
    )
}

/**
 * Funzione per ottenere l'indice della frazione
 */
private fun getFractionIndex(fraction: Float): Int {
    return when (fraction) {
        0f -> 0
        0.125f -> 1
        0.25f -> 2
        0.375f -> 3
        0.5f -> 4
        0.625f -> 5
        0.75f -> 6
        0.875f -> 7
        else -> 0
    }
}