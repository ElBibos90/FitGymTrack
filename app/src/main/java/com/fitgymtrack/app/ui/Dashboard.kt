// Dashboard.kt
package com.fitgymtrack.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.ui.theme.*
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.DashboardViewModel
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowForward


@Composable
fun Dashboard(
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

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
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FitGymTrack",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Indigo600
                    )

                    Text(
                        text = "Benvenuto, ${user?.name ?: user?.username ?: "Utente"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                // Profile icon or initials
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Indigo600),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (user?.name?.firstOrNull() ?: user?.username?.firstOrNull() ?: "U").toString().uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
                    // Subscription Card
                    SubscriptionCard(subscription = subscription)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Main Feature Cards
                    MainFeatureCards()

                    Spacer(modifier = Modifier.height(24.dp))

                    // Custom Exercises Card
                    CustomExercisesCard()

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

@Composable
fun SubscriptionCard(subscription: Subscription?) {
    if (subscription != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (subscription.price > 0)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon/Badge - Qui è il fix corretto
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    brush = if (subscription.price > 0)
                                        GradientUtils.purpleGradient
                                    else
                                    // Creazione di un brush con un colore solido
                                        Brush.verticalGradient(listOf(Color.Gray.copy(alpha = 0.2f), Color.Gray.copy(alpha = 0.2f)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (subscription.price > 0)
                                    Icons.Default.Star
                                else
                                    Icons.Default.StarBorder,
                                contentDescription = "Plan",
                                tint = if (subscription.price > 0) Color.White else Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

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
                    }

                    Button(
                        onClick = { /* Naviga alla gestione abbonamento */ },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (subscription.price > 0)
                                Indigo600
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    ) {
                        Text("Gestisci")
                    }
                }

                // Il resto del codice è uguale
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
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Indigo600,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
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
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Indigo600,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        }
                    }

                    // Call to action for free plan
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { /* Navigate to subscription upgrade */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Indigo600
                        )
                    ) {
                        Text("Passa al piano Premium")
                    }
                }
            }
        }
    }
}

@Composable
fun MainFeatureCards() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Workout Plans Card
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = GradientUtils.blueGradient,
                        shape = RoundedCornerShape(16.dp)
                    )
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

                    Button(
                        onClick = { /* Naviga alle schede */ },
                        colors = ButtonDefaults.buttonColors(
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

        // Workouts Card
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
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

                    Button(
                        onClick = { /* Naviga agli allenamenti */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(8.dp)
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
}

@Composable
fun CustomExercisesCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
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

                Button(
                    onClick = { /* Naviga agli esercizi personalizzati */ },
                    colors = ButtonDefaults.buttonColors(
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
fun SupportBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
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