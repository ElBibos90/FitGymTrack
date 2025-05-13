package com.fitgymtrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.DashboardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun Dashboard(
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val dashboardState by viewModel.dashboardState.collectAsState()
    val user by viewModel.user.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val subscription by viewModel.subscription.collectAsState()

    // Carica i dati all'avvio della composable
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData(sessionManager)
    }

    // Crea la funzione di logout qui
    val logoutAction: () -> Unit = {
        coroutineScope.launch {
            sessionManager.clearSession()
            onLogout()
        }

    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "FitGymTrack",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Benvenuto, ${user?.name ?: user?.username ?: "Utente"}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stato del caricamento
            when (dashboardState) {
                is DashboardViewModel.DashboardState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is DashboardViewModel.DashboardState.Error -> {
                    val errorState = dashboardState as DashboardViewModel.DashboardState.Error
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Errore: ${errorState.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is DashboardViewModel.DashboardState.Success -> {
                    // Contenuto della dashboard con il logout semplificato
                    DashboardContent(
                        subscription = subscription,
                        onLogout = logoutAction
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    subscription: Subscription?,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Sezione Abbonamento
        if (subscription != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Piano ${subscription.planName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = if (subscription.price > 0)
                                    "Piano a pagamento: ${subscription.price}€"
                                else
                                    "Piano gratuito",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { /* Naviga alla gestione abbonamento */ }
                        ) {
                            Text("Gestisci")
                        }
                    }

                    // Mostra i limiti per il piano gratuito
                    if (subscription.price == 0.0) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Limiti Piano Free:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Allenamenti
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Allenamenti:")
                                Text(
                                    text = if (subscription.maxWorkouts == null)
                                        "Illimitati"
                                    else
                                        "${subscription.currentCount}/${subscription.maxWorkouts}"
                                )
                            }

                            if (subscription.maxWorkouts != null) {
                                LinearProgressIndicator(
                                    progress = (subscription.currentCount.toFloat() /
                                            subscription.maxWorkouts.toFloat()).coerceIn(0f, 1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .padding(top = 4.dp)
                                )
                            }
                        }

                        // Esercizi personalizzati
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Esercizi personalizzati:")
                                Text(
                                    text = if (subscription.maxCustomExercises == null)
                                        "Illimitati"
                                    else
                                        "${subscription.currentCustomExercises}/${subscription.maxCustomExercises}"
                                )
                            }

                            if (subscription.maxCustomExercises != null) {
                                LinearProgressIndicator(
                                    progress = (subscription.currentCustomExercises.toFloat() /
                                            subscription.maxCustomExercises.toFloat()).coerceIn(0f, 1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Schede Principali
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Workout Plans Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF3949AB),
                                    Color(0xFF1E88E5)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Schede",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Gestisci le tue schede di allenamento",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { /* Naviga alle schede */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "Visualizza",
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Workouts Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF43A047),
                                    Color(0xFF26A69A)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Allenamenti",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Inizia un allenamento o visualizza lo storico",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { /* Naviga agli allenamenti */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "Inizia",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Esercizi Personalizzati Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF8E24AA),
                                Color(0xFF5E35B1)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Esercizi Personalizzati",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Crea e gestisci i tuoi esercizi",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = { /* Naviga agli esercizi personalizzati */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = "Gestisci",
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Pulsante Logout
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Logout")
        }
    }
}