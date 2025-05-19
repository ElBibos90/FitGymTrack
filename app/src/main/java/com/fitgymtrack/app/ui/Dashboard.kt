package com.fitgymtrack.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.ui.theme.*
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.ui.theme.Indigo600

@Composable
fun Dashboard(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToWorkoutPlans: () -> Unit,
    onNavigateToUserExercises: () -> Unit,
    onNavigateToWorkouts: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Calcola se la schermata è stata scrollata
    val isScrolled = scrollState.value > 10

    val dashboardState by viewModel.dashboardState.collectAsState()
    val user by viewModel.user.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val subscription by viewModel.subscription.collectAsState()

    // Carica i dati all'avvio della composable
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData(sessionManager)
    }

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
                .verticalScroll(scrollState)
        ) {
            // Stato del caricamento
            when (dashboardState) {
                is DashboardViewModel.DashboardState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Indigo600)
                    }
                }

                is DashboardViewModel.DashboardState.Error -> {
                    val errorState = dashboardState as DashboardViewModel.DashboardState.Error
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Errore: ${errorState.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is DashboardViewModel.DashboardState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Header Welcome - è già gestito nella TopBar, ma potresti volerne uno secondario qui
                        Text(
                            text = "Benvenuto nella tua Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Subscription Card
                        SimplifiedSubscriptionCard(subscription = subscription)

                        Spacer(modifier = Modifier.height(24.dp))

                        // Profilo Utente Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clickable { onNavigateToProfile() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Indigo600),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Profilo Utente",
                                            tint = Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        Text(
                                            text = "Profilo Utente",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Gestisci le tue informazioni personali",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Vai al profilo",
                                    tint = Indigo600
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Main Feature Cards
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Workout Plans Card con sfumatura blu
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GradientUtils.blueGradient)
                                    .clickable { onNavigateToWorkoutPlans() },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(GradientUtils.blueGradient)
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = "Workout Plans",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Schede",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Gestisci le tue schede di allenamento",
                                            color = Color.White.copy(alpha = 0.9f),
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        FilledTonalButton(
                                            onClick = { onNavigateToWorkoutPlans() },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Visualizza",
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Workouts Card con sfumatura verde
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GradientUtils.greenGradient)
                                    .clickable { onNavigateToWorkouts() },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(GradientUtils.greenGradient)
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Icon(
                                            imageVector = Icons.Default.FitnessCenter,
                                            contentDescription = "Workouts",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Allenamenti",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Inizia un allenamento o visualizza lo storico",
                                            color = Color.White.copy(alpha = 0.9f),
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        FilledTonalButton(
                                            onClick = { onNavigateToWorkouts() },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Visualizza",
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Custom Exercises Card
                        CustomExercisesCard(onClick = { onNavigateToUserExercises() })

                        Spacer(modifier = Modifier.height(24.dp))

                        // Support Banner
                        SupportBanner()

                        Spacer(modifier = Modifier.height(32.dp))

                        // Logout Button
                        Button(
                            onClick = logoutAction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "Logout",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Funzione aggiornata per accettare l'azione di click
@Composable
fun CustomExercisesCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GradientUtils.purpleGradient)
            .clickable(onClick = onClick), // Aggiunto click listener
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GradientUtils.purpleGradient)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Custom Exercises",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Esercizi Personalizzati",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Crea e gestisci i tuoi esercizi",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                FilledTonalButton(
                    onClick = onClick, // Usa lo stesso onClick per coerenza
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Gestisci",
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CustomExercisesCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GradientUtils.purpleGradient),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GradientUtils.purpleGradient)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Custom Exercises",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Esercizi Personalizzati",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Crea e gestisci i tuoi esercizi",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                FilledTonalButton(
                    onClick = { /* Naviga agli esercizi personalizzati */ },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Gestisci",
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SimplifiedSubscriptionCard(
    subscription: Subscription?,
    onUpgradeClick: () -> Unit = {}
) {
    if (subscription == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF7F5FF)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Piano info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (subscription.price > 0) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Piano",
                        tint = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Piano ${subscription.planName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEAE6FF)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Gestisci",
                        color = Color(0xFF4F46E5),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            Divider(
                color = Color.LightGray.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Limiti Piano Free
            Text(
                text = "Limiti Piano Free:",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Allenamenti
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Allenamenti:",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${subscription.currentCount}/${subscription.maxWorkouts ?: "∞"}",
                        fontSize = 14.sp
                    )
                }

                LinearProgressIndicator(
                    progress = if (subscription.maxWorkouts != null)
                        (subscription.currentCount.toFloat() / subscription.maxWorkouts.toFloat()).coerceIn(0f, 1f)
                    else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Indigo600,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
            }

            // Esercizi personalizzati
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Esercizi personalizzati:",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${subscription.currentCustomExercises}/${subscription.maxCustomExercises ?: "∞"}",
                        fontSize = 14.sp
                    )
                }

                LinearProgressIndicator(
                    progress = if (subscription.maxCustomExercises != null)
                        (subscription.currentCustomExercises.toFloat() / subscription.maxCustomExercises.toFloat()).coerceIn(0f, 1f)
                    else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Indigo600,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
            }

            // Upgrade button
            Button(
                onClick = onUpgradeClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Indigo600
                )
            ) {
                Text(
                    text = "Passa al piano Premium",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SupportBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                )
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Support",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Supporta FitGymTrack",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Contribuisci allo sviluppo",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Button(
                    onClick = { /* Naviga alla pagina di supporto */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Dona",
                        color = Color(0xFFEC4899)
                    )
                }
            }
        }
    }
}