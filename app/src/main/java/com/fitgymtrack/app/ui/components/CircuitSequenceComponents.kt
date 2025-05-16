package com.fitgymtrack.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitgymtrack.app.models.CompletedSeries
import com.fitgymtrack.app.models.WorkoutExercise
import com.fitgymtrack.app.ui.theme.BluePrimary
import com.fitgymtrack.app.ui.theme.PurplePrimary
import com.fitgymtrack.app.utils.WeightFormatter

/**
 * Componente che visualizza una sequenza di circuito o superset
 */
@Composable
fun CircuitSequence(
    exercises: List<WorkoutExercise>,
    completedSeries: Map<Int, List<CompletedSeries>>,
    modifier: Modifier = Modifier
) {
    val setType = exercises.firstOrNull()?.setType ?: "normal"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Titolo
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (setType == "superset") Icons.Default.SwapHoriz else Icons.Default.Sync,
                contentDescription = null,
                tint = if (setType == "superset") PurplePrimary else BluePrimary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (setType == "superset") "Sequenza Superset" else "Sequenza Circuit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = if (setType == "superset") PurplePrimary else BluePrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sequenza orizzontale di esercizi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((-4).dp)
        ) {
            exercises.forEachIndexed { index, exercise ->
                val completedExerciseSeries = completedSeries[exercise.id] ?: emptyList()
                val isCompleted = completedExerciseSeries.size >= exercise.serie

                // Se non è il primo, aggiungiamo una freccia connettiva
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = if (setType == "superset") PurplePrimary.copy(alpha = 0.7f) else BluePrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Elemento dell'esercizio
                SequenceExerciseItem(
                    name = exercise.nome,
                    completedSeries = completedExerciseSeries.size,
                    totalSeries = exercise.serie,
                    isCompleted = isCompleted,
                    setType = setType
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Barra di progresso del circuito
        CircuitProgressBar(
            exercises = exercises,
            completedSeries = completedSeries,
            setType = setType
        )
    }
}

/**
 * Visualizza un singolo elemento nella sequenza del circuito
 */
@Composable
fun SequenceExerciseItem(
    name: String,
    completedSeries: Int,
    totalSeries: Int,
    isCompleted: Boolean,
    setType: String
) {
    Surface(
        modifier = Modifier
            .height(40.dp)
            .widthIn(min = 80.dp),
        shape = RoundedCornerShape(20.dp),
        color = when {
            isCompleted -> if (setType == "superset")
                PurplePrimary.copy(alpha = 0.2f)
            else
                BluePrimary.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) {
                            if (setType == "superset") PurplePrimary else BluePrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                    )
            )

            // Exercise name
            Text(
                text = "$name ($completedSeries/$totalSeries)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isCompleted) FontWeight.Medium else FontWeight.Normal,
                color = if (isCompleted) {
                    if (setType == "superset") PurplePrimary else BluePrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Barra di progresso che mostra l'avanzamento dell'intero circuito
 */
@Composable
fun CircuitProgressBar(
    exercises: List<WorkoutExercise>,
    completedSeries: Map<Int, List<CompletedSeries>>,
    setType: String
) {
    // Calcola il progresso totale
    val totalSets = exercises.sumOf { it.serie }
    val completedSets = exercises.sumOf { exercise ->
        (completedSeries[exercise.id]?.size ?: 0)
    }

    val progress = if (totalSets > 0) {
        completedSets.toFloat() / totalSets.toFloat()
    } else {
        0f
    }

    // Animazione del progresso
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "circuitProgress"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Progresso complessivo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "$completedSets/$totalSets serie",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (setType == "superset") PurplePrimary else BluePrimary
            )
        }

        // Updated to use lambda progress
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (setType == "superset") PurplePrimary else BluePrimary,
            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
        )
    }
}