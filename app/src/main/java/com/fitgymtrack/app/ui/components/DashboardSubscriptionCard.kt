package com.fitgymtrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.ui.theme.Indigo600

/**
 * Card per mostrare un riepilogo dell'abbonamento corrente nella dashboard
 */
@Composable
fun DashboardSubscriptionCard(
    subscription: Subscription?,
    onViewDetails: () -> Unit
) {
    if (subscription == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable(onClick = onViewDetails),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (subscription.price > 0)
                Color(0xFFF7F5FF)  // Colore più chiaro per Premium
            else
                Color(0xFFF8FAFC)  // Colore neutro per Free
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header con informazioni piano
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (subscription.price > 0)
                                Indigo600
                            else
                                Color.Gray.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (subscription.price > 0)
                            Icons.Default.Star
                        else
                            Icons.Default.StarBorder,
                        contentDescription = "Piano",
                        tint = if (subscription.price > 0) Color.White else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Piano ${subscription.planName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = if (subscription.price > 0)
                            "${subscription.price}€ al mese"
                        else
                            "Piano gratuito",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (subscription.price > 0)
                            Indigo600
                        else
                            Color(0xFFEAE6FF)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Dettagli",
                        color = if (subscription.price > 0)
                            Color.White
                        else
                            Color(0xFF4F46E5),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            Divider(
                color = Color.LightGray.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Utilizo delle risorse
            if (subscription.maxWorkouts != null || subscription.maxCustomExercises != null) {
                // Mostra progress bars solo per piano Free con limiti
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                    // Allenamenti
                    if (subscription.maxWorkouts != null) {
                        UsageProgressBar(
                            label = "Schede di allenamento",
                            current = subscription.currentCount,
                            max = subscription.maxWorkouts,
                            isPremium = subscription.price > 0
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Esercizi personalizzati
                    if (subscription.maxCustomExercises != null) {
                        UsageProgressBar(
                            label = "Esercizi personalizzati",
                            current = subscription.currentCustomExercises,
                            max = subscription.maxCustomExercises,
                            isPremium = subscription.price > 0
                        )
                    }
                }
            } else {
                // Piano Premium senza limiti
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Piano Premium con risorse illimitate",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Indigo600
                    )
                }
            }
        }
    }
}

@Composable
fun UsageProgressBar(
    label: String,
    current: Int,
    max: Int,
    isPremium: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = "$current/$max",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress Bar
        LinearProgressIndicator(
            progress = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (isPremium) Indigo600 else Indigo600.copy(alpha = 0.7f),
            trackColor = Color.LightGray.copy(alpha = 0.3f)
        )
    }
}