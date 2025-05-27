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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import java.util.Locale

/**
 * Preview compatta delle statistiche per la Dashboard
 */
@Composable
fun DashboardStatsPreview(
    stats: UserStats?,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    isDarkTheme: Boolean = false,
    onViewAllStats: () -> Unit

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
                                fontSize = 16.sp,
                                color = if (isDarkTheme) Color.White else Color.Black
                            )

                            Text(
                                text = "I tuoi progressi",
                                color = if (isDarkTheme) Color.LightGray else Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Badge Premium + Pulsante migliorato
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                    )
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "PREMIUM",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Pulsante migliorato e più visibile
                        ElevatedButton(
                            onClick = onViewAllStats,
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Indigo600,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.elevatedButtonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 8.dp
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .widthIn(min = 100.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Visualizza",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
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
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )

                        // Streak corrente
                        MiniStatCard(
                            icon = Icons.Default.LocalFireDepartment,
                            value = "${stats.currentStreak}",
                            label = "Streak giorni",
                            color = Color(0xFFFF6B35),
                            isDarkTheme = isDarkTheme,
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
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )

                        // Media settimanale
                        MiniStatCard(
                            icon = Icons.AutoMirrored.Default.TrendingUp,
                            value = "${String.format(Locale.getDefault(),"%.1f", stats.weeklyAverage)}",
                            label = "Media/sett.",
                            color = PurplePrimary,
                            isDarkTheme = isDarkTheme,
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
                            tint = if (isDarkTheme) Color.Gray else Color.DarkGray,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Inizia ad allenarti per vedere le tue statistiche!",
                            color = if (isDarkTheme) Color.Gray else Color.DarkGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card mini per le statistiche nella preview - migliorata per leggibilità
 */
@Composable
fun MiniStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                color.copy(alpha = 0.2f)
            } else {
                color.copy(alpha = 0.1f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    color = if (isDarkTheme) color.copy(alpha = 0.9f) else color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = label,
                    color = if (isDarkTheme) {
                        color.copy(alpha = 0.6f)
                    } else {
                        color.copy(alpha = 0.7f)
                    },
                    fontSize = 10.sp
                )
            }
        }
    }
}