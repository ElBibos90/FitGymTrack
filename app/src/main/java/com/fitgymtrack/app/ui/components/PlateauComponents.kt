package com.fitgymtrack.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.utils.PlateauInfo
import com.fitgymtrack.app.utils.PlateauType
import com.fitgymtrack.app.utils.ProgressionSuggestion
import com.fitgymtrack.app.utils.SuggestionType
import com.fitgymtrack.app.utils.WeightFormatter

/**
 * Indicatore visivo per segnalare un plateau
 */
@Composable
fun PlateauIndicator(
    plateauInfo: PlateauInfo,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}

) {
    var isExpanded by remember { mutableStateOf(false) }

    val plateauColor = when (plateauInfo.plateauType) {
        PlateauType.LIGHT_WEIGHT -> Color(0xFFFF9800) // Arancione
        PlateauType.HEAVY_WEIGHT -> Color(0xFFF44336) // Rosso
        PlateauType.LOW_REPS -> Color(0xFF2196F3)     // Blue
        PlateauType.HIGH_REPS -> Color(0xFF9C27B0)    // Viola
        PlateauType.MODERATE -> Color(0xFFFF5722)     // Deep Orange
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = plateauColor.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = plateauColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header del plateau
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icona animata di warning
                    PlateauWarningIcon(
                        color = plateauColor,
                        isAnimated = true
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "⚡ Plateau Rilevato",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = plateauColor
                        )

                        Text(
                            text = "Stessi valori per ${plateauInfo.sessionsInPlateau} allenamenti",
                            style = MaterialTheme.typography.bodySmall,
                            color = plateauColor.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                            tint = plateauColor
                        )
                    }

                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = plateauColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Contenuto espandibile
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // Informazioni attuali
                    PlateauCurrentInfo(
                        weight = plateauInfo.currentWeight,
                        reps = plateauInfo.currentReps,
                        plateauColor = plateauColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Suggerimenti
                    Text(
                        text = "💡 Suggerimenti per Progredire:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = plateauColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    plateauInfo.suggestions.take(2).forEach { suggestion ->
                        ProgressionSuggestionCard(
                            suggestion = suggestion,
                            plateauColor = plateauColor
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Icona animata di warning per il plateau
 */
@Composable
private fun PlateauWarningIcon(
    color: Color,
    isAnimated: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.1f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Plateau",
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Mostra le informazioni attuali del plateau
 */
@Composable
private fun PlateauCurrentInfo(
    weight: Float,
    reps: Int,
    plateauColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = plateauColor.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CurrentValueItem(
                label = "Peso Attuale",
                value = "${WeightFormatter.formatWeight(weight)} kg",
                icon = Icons.Default.FitnessCenter,
                color = plateauColor
            )

            CurrentValueItem(
                label = "Ripetizioni Attuali",
                value = reps.toString(),
                icon = Icons.Default.Repeat,
                color = plateauColor
            )
        }
    }
}

/**
 * Item per mostrare un valore attuale
 */
@Composable
private fun CurrentValueItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Card per un suggerimento di progressione - SENZA pulsante applica
 */
@Composable
private fun ProgressionSuggestionCard(
    suggestion: ProgressionSuggestion,
    plateauColor: Color
) {
    val suggestionIcon = when (suggestion.type) {
        SuggestionType.INCREASE_WEIGHT -> Icons.Default.ArrowUpward
        SuggestionType.INCREASE_REPS -> Icons.Default.Add
        SuggestionType.ADVANCED_TECHNIQUE -> Icons.Default.Psychology
        SuggestionType.REDUCE_REST -> Icons.Default.Timer
        SuggestionType.CHANGE_TEMPO -> Icons.Default.Speed
    }

    val confidenceColor = when {
        suggestion.confidence >= 0.8f -> Color(0xFF4CAF50) // Verde
        suggestion.confidence >= 0.6f -> Color(0xFFFF9800) // Arancione
        else -> Color(0xFFF44336) // Rosso
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = plateauColor.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = suggestionIcon,
                contentDescription = null,
                tint = plateauColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Indicatore di confidenza
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Confidenza: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${(suggestion.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = confidenceColor
                    )
                }
            }
        }
    }
}

/**
 * Semplice badge per indicare plateau in modo discreto
 */
@Composable
fun PlateauBadge(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFF5722).copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                contentDescription = "Plateau",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Plateau",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Dialog dettagliato per plateau con SOLO visualizzazione dei suggerimenti
 */
@Composable
fun PlateauDetailDialog(
    plateauInfo: PlateauInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                contentDescription = "Plateau",
                tint = Color(0xFFFF5722)
            )
        },
        title = {
            Text(
                text = "Plateau Rilevato!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Nome dell'esercizio
                Text(
                    text = "Esercizio: ${plateauInfo.exerciseName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF5722)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Hai usato gli stessi valori per ${plateauInfo.sessionsInPlateau} allenamenti consecutivi. È il momento di progredire!",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Valori attuali:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "• Peso: ${WeightFormatter.formatWeight(plateauInfo.currentWeight)} kg",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "• Ripetizioni: ${plateauInfo.currentReps}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Suggerimenti per progredire:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                plateauInfo.suggestions.take(3).forEach { suggestion ->
                    Text(
                        text = "• ${suggestion.description}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                if (plateauInfo.suggestions.size > 3) {
                    Text(
                        text = "• E altri ${plateauInfo.suggestions.size - 3} suggerimenti...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            // Nessun pulsante di conferma/applica
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Indietro")
            }
        }
    )
}

/**
 * Dialog specifico per plateau nei gruppi (superset/circuit)
 */
@Composable
fun GroupPlateauDialog(
    groupTitle: String,
    plateauList: List<PlateauInfo>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                contentDescription = "Plateau Gruppo",
                tint = Color(0xFFFF5722)
            )
        },
        title = {
            Text(
                text = "Plateau nel $groupTitle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sono stati rilevati ${plateauList.size} plateau in questo gruppo:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                plateauList.forEach { plateau ->
                    // Card per ogni plateau
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF5722).copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = Color(0xFFFF5722).copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // Nome dell'esercizio
                            Text(
                                text = plateau.exerciseName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5722)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Valori attuali
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Peso: ${WeightFormatter.formatWeight(plateau.currentWeight)} kg",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Reps: ${plateau.currentReps}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Miglior suggerimento
                            val bestSuggestion = plateau.suggestions.firstOrNull()
                            if (bestSuggestion != null) {
                                Text(
                                    text = "💡 ${bestSuggestion.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Indicatore sessioni
                            Text(
                                text = "${plateau.sessionsInPlateau} allenamenti con stessi valori",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Suggerimento generale
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "💪 Considera di variare i carichi e le ripetizioni per superare questi plateau!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            // Nessun pulsante di conferma/applica
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Indietro")
            }
        }
    )
}