package com.fitgymtrack.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.models.UserStats
import com.fitgymtrack.app.ui.theme.*
import java.util.Locale

/**
 * Card delle statistiche per utenti Premium nella Dashboard
 */
@Composable
fun DashboardStatsCard(
    stats: UserStats?,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isDarkTheme: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme)
                Color(0xFF1E293B)
            else
                Color(0xFFF0F4FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        if (isLoading) {
            // Stato di caricamento
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Indigo600,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header con icona premium
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Indigo600, PurplePrimary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Statistiche",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Statistiche Premium",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )

                            Text(
                                text = "I tuoi progressi",
                                color = if (isDarkTheme) Color.LightGray else Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Badge Premium
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Indigo600)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PREMIUM",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (stats != null) {
                    // Griglia delle statistiche principali
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Allenamenti totali
                        StatCard(
                            icon = Icons.Default.FitnessCenter,
                            value = "${stats.totalWorkouts}",
                            label = "Allenamenti",
                            gradient = GradientUtils.blueGradient,
                            modifier = Modifier.weight(1f)
                        )

                        // Tempo totale
                        StatCard(
                            icon = Icons.Default.Schedule,
                            value = "${stats.totalHours}h",
                            label = "Ore totali",
                            gradient = GradientUtils.greenGradient,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Streak corrente
                        StatCard(
                            icon = Icons.Default.LocalFireDepartment,
                            value = "${stats.currentStreak}",
                            label = "Streak giorni",
                            gradient = Brush.linearGradient(
                                colors = listOf(Color(0xFFFF6B35), Color(0xFFFF8E53))
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Media settimanale
                        StatCard(
                            icon = Icons.AutoMirrored.Default.TrendingUp,
                            value = "${String.format(Locale.getDefault(),"%.1f", stats.weeklyAverage)}",
                            label = "Media/sett.",
                            gradient = GradientUtils.purpleGradient,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progressi peso (se disponibili)
                    if (stats.weightProgress != null && stats.weightProgress > 0) {
                        WeightProgressIndicator(
                            progress = stats.weightProgress,
                            label = "Progressi peso medio",
                            isDarkTheme = isDarkTheme
                        )
                    }

                    // Esercizio preferito
                    if (!stats.favoriteExercise.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700), // Oro
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = "Esercizio preferito",
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color.LightGray else Color.Gray
                                )
                                Text(
                                    text = stats.favoriteExercise,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    // Stato vuoto
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Inizia ad allenarti per vedere le tue statistiche!",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card singola statistica
 */
@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    Text(
                        text = value,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Indicatore di progresso per il peso
 */
@Composable
fun WeightProgressIndicator(
    progress: Float,
    label: String,
    isDarkTheme: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "weight_progress"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = if (isDarkTheme) Color.LightGray else Color.Gray
            )
            Text(
                text = "+${String.format(Locale.getDefault(),"%.1f", progress)}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = GreenPrimary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = GreenPrimary,
            trackColor = if (isDarkTheme)
                Color.DarkGray.copy(alpha = 0.3f)
            else
                Color.LightGray.copy(alpha = 0.3f)
        )
    }
}