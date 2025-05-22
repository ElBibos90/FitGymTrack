package com.fitgymtrack.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.PersonalRecord
import com.fitgymtrack.app.models.UserStats
import com.fitgymtrack.app.ui.theme.*
import com.fitgymtrack.app.ui.components.StatCard
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.StatsViewModel
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scrollState = rememberScrollState()
    val statsState by viewModel.statsState.collectAsState()

    // Imposta il SessionManager nel ViewModel e carica le statistiche
    LaunchedEffect(Unit) {
        viewModel.setSessionManager(sessionManager)

        // Ottieni l'utente dalla sessione per passare l'ID corretto
        val currentUser = sessionManager.getUserData().first()
        currentUser?.let { user ->
            Log.d("StatsScreen", "Caricamento statistiche per utente: ${user.id}")
            viewModel.loadStats(user.id, forceReload = true) // Force reload per dati freschi
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiche Premium") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when (statsState) {
            is StatsViewModel.StatsState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Indigo600,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            is StatsViewModel.StatsState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Errore nel caricamento",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = (statsState as StatsViewModel.StatsState.Error).message,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.loadStats() }
                        ) {
                            Text("Riprova")
                        }
                    }
                }
            }

            is StatsViewModel.StatsState.Success -> {
                val stats = (statsState as StatsViewModel.StatsState.Success).stats

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    // Header con informazioni generali
                    StatsHeaderCard(stats)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Statistiche principali in griglia 2x2
                    Text(
                        text = "Statistiche Generali",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            icon = Icons.Default.FitnessCenter,
                            value = "${stats.totalWorkouts}",
                            label = "Allenamenti totali",
                            gradient = GradientUtils.blueGradient,
                            modifier = Modifier.weight(1f)
                        )

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
                        StatCard(
                            icon = Icons.Default.LocalFireDepartment,
                            value = "${stats.currentStreak}",
                            label = "Streak attuale",
                            gradient = Brush.linearGradient(
                                colors = listOf(Color(0xFFFF6B35), Color(0xFFFF8E53))
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            icon = Icons.Default.TrendingUp,
                            value = "${String.format("%.1f", stats.weeklyAverage)}",
                            label = "Media settimanale",
                            gradient = GradientUtils.purpleGradient,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Statistiche dettagliate
                    DetailedStatsSection(stats)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Record personali
                    if (stats.personalRecords.isNotEmpty()) {
                        PersonalRecordsSection(stats.personalRecords)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Progressi peso
                    if (stats.weightProgress != null && stats.weightProgress > 0) {
                        WeightProgressSection(stats)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Informazioni aggiuntive
                    AdditionalInfoSection(stats)
                }
            }
        }
    }
}

@Composable
fun StatsHeaderCard(stats: UserStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Indigo600, PurplePrimary)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Le tue statistiche",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (stats.firstWorkoutDate != null)
                                "Dal ${stats.firstWorkoutDate}"
                            else
                                "Inizia ad allenarti per vedere i progressi",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (stats.consistencyScore > 0) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Punteggio Consistenza",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "${String.format("%.1f", stats.consistencyScore)}%",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DetailedStatsSection(stats: UserStats) {
    Text(
        text = "Statistiche Dettagliate",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            DetailStatRow(
                label = "Esercizi eseguiti",
                value = "${stats.totalExercisesPerformed}"
            )

            DetailStatRow(
                label = "Serie completate",
                value = "${stats.totalSetsCompleted}"
            )

            DetailStatRow(
                label = "Ripetizioni totali",
                value = "${stats.totalRepsCompleted}"
            )

            DetailStatRow(
                label = "Streak più lungo",
                value = "${stats.longestStreak} giorni"
            )

            DetailStatRow(
                label = "Media mensile",
                value = "${String.format("%.1f", stats.monthlyAverage)} allenamenti"
            )

            if (stats.averageWorkoutDuration > 0) {
                DetailStatRow(
                    label = "Durata media",
                    value = "${stats.averageWorkoutDuration} minuti"
                )
            }

            if (stats.favoriteExercise != null) {
                DetailStatRow(
                    label = "Esercizio preferito",
                    value = stats.favoriteExercise,
                    isLastItem = true
                )
            }
        }
    }
}

@Composable
fun DetailStatRow(
    label: String,
    value: String,
    isLastItem: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }

    if (!isLastItem) {
        Divider(
            color = Color.Gray.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
    }
}

@Composable
fun PersonalRecordsSection(records: List<PersonalRecord>) {
    Text(
        text = "Record Personali",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    records.take(5).forEach { record ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = record.exerciseName,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = record.type.replace("_", " ").replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${record.value}",
                        fontWeight = FontWeight.Bold,
                        color = Indigo600
                    )
                    Text(
                        text = record.dateAchieved,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun WeightProgressSection(stats: UserStats) {
    Text(
        text = "Progressi Peso",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Miglioramento medio",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "+${String.format("%.1f", stats.weightProgress)}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val animatedProgress by animateFloatAsState(
                targetValue = (stats.weightProgress ?: 0f) / 100f,
                animationSpec = tween(durationMillis = 1000),
                label = "weight_progress"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = GreenPrimary,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )

            if (stats.heaviestLift != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Record di peso",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Text(
                    text = "${stats.heaviestLift.exerciseName}: ${stats.heaviestLift.weight}kg",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AdditionalInfoSection(stats: UserStats) {
    Text(
        text = "Informazioni Aggiuntive",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (stats.bestWorkoutTime != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Indigo600,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Orario migliore",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = when (stats.bestWorkoutTime) {
                                "morning" -> "Mattina"
                                "afternoon" -> "Pomeriggio"
                                "evening" -> "Sera"
                                else -> stats.bestWorkoutTime.replaceFirstChar { it.uppercase() }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (stats.mostActiveDay != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Giorno più attivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = when (stats.mostActiveDay) {
                                "monday" -> "Lunedì"
                                "tuesday" -> "Martedì"
                                "wednesday" -> "Mercoledì"
                                "thursday" -> "Giovedì"
                                "friday" -> "Venerdì"
                                "saturday" -> "Sabato"
                                "sunday" -> "Domenica"
                                else -> stats.mostActiveDay.replaceFirstChar { it.uppercase() }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (stats.goalsAchieved > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Obiettivi raggiunti",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = "${stats.goalsAchieved}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}