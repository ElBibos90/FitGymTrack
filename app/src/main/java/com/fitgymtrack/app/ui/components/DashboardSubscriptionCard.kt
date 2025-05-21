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
    isDarkTheme: Boolean = false, // Aggiungiamo questo parametro
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
            containerColor = if (subscription.price > 0) {
                if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF7F5FF)  // Adattato per tema dark
            } else {
                if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)  // Adattato per tema dark
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header con informazioni piano
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                // Contenuto a sinistra: icona e info piano
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.align(Alignment.CenterStart)
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
                                    if (isDarkTheme) Color.DarkGray else Color.Gray.copy(alpha = 0.2f)
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

                    // Informazioni piano su due righe
                    Column {
                        Text(
                            text = "Piano ${subscription.planName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isDarkTheme) Color.White else Color.Black
                        )

                        Text(
                            text = if (subscription.price > 0)
                                "${subscription.price}€ al mese"
                            else
                                "Piano gratuito",
                            color = if (isDarkTheme) Color.LightGray else Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }

                // Pulsante "Dettagli" a destra
                Button(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (subscription.price > 0)
                            Indigo600
                        else
                            if (isDarkTheme) Color(0xFF312E81) else Color(0xFFEAE6FF)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        text = "Dettagli",
                        color = if (subscription.price > 0 || isDarkTheme)
                            Color.White
                        else
                            Color(0xFF4F46E5),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            // Descrizione del piano sotto al layout principale
            Text(
                text = "Piano ${subscription.planName} con risorse illimitate",
                color = if (isDarkTheme) Indigo600.copy(alpha = 0.8f) else Indigo600,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
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