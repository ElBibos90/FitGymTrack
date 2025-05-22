package com.fitgymtrack.app.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.models.UserStats
import com.fitgymtrack.app.ui.theme.*

/**
 * Preview compatta delle statistiche per la Dashboard
 */
@Composable
fun DashboardStatsPreview(
    stats: UserStats?,
    isLoading: Boolean = false,
    isDarkTheme: Boolean = false,
    onViewAllStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable { onViewAllStats() },
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
                    .height(140.dp),
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
                // Header con badge Premium
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
                                fontSize = 16.sp
                            )

                            Text(
                                text = "I tuoi progressi",
                                color = if (isDarkTheme) Color.LightGray else Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Badge Premium + Pulsante
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Indigo600)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "PREMIUM",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onViewAllStats,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo600
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .widthIn(min = 90.dp) // Larghezza minima garantita
                        ) {
                            Text(
                                text = "Visualizza",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (stats != null) {
                    Log.d("DashboardStatsPreview", "Mostrando statistiche: ${stats.totalWorkouts} allenamenti, ${stats.currentStreak} streak")

                    // Statistiche principali in formato compatto (2x2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Allenamenti totali
                        MiniStatCard(
                            icon = Icons.Default.FitnessCenter,
                            value = "${stats.totalWorkouts}",
                            label = "Allenamenti",
                            color = BluePrimary,
                            modifier = Modifier.weight(1f)
                        )

                        // Streak corrente
                        MiniStatCard(
                            icon = Icons.Default.LocalFireDepartment,
                            value = "${stats.currentStreak}",
                            label = "Streak giorni",
                            color = Color(0xFFFF6B35),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tempo totale
                        MiniStatCard(
                            icon = Icons.Default.Schedule,
                            value = "${stats.totalHours}h",
                            label = "Ore totali",
                            color = GreenPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        // Media settimanale
                        MiniStatCard(
                            icon = Icons.Default.TrendingUp,
                            value = "${String.format("%.1f", stats.weeklyAverage)}",
                            label = "Media/sett.",
                            color = PurplePrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Log.d("DashboardStatsPreview", "Statistiche null - mostrando stato vuoto")

                    // Stato vuoto
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Inizia ad allenarti per vedere le tue statistiche!",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card mini per le statistiche nella preview
 */
@Composable
fun MiniStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )

            Column {
                Text(
                    text = value,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = label,
                    color = color.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}