package com.fitgymtrack.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.models.User
import com.fitgymtrack.app.models.UserProfile
import com.fitgymtrack.app.ui.components.*
import com.fitgymtrack.app.ui.payment.PaymentHelper
import com.fitgymtrack.app.ui.theme.*
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.DashboardViewModel
import com.fitgymtrack.app.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.fitgymtrack.app.FitGymTrackApplication
import com.fitgymtrack.app.utils.ThemeManager

@Composable
fun Dashboard(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToWorkoutPlans: () -> Unit,
    onNavigateToUserExercises: () -> Unit,
    onNavigateToWorkouts: () -> Unit,
    onNavigateToSubscription: () -> Unit = {},
    dashboardViewModel: DashboardViewModel = viewModel(),
    subscriptionViewModel: SubscriptionViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Ottieni il tema direttamente dall'app
    val themeManager = (context.applicationContext as FitGymTrackApplication).themeManager

    // Ottieni la modalità del tema
    val themeMode by themeManager.themeFlow.collectAsState(initial = ThemeManager.ThemeMode.SYSTEM)

    // Determina se è dark theme
    val isDarkTheme = when (themeMode) {
        ThemeManager.ThemeMode.LIGHT -> false
        ThemeManager.ThemeMode.DARK -> true
        ThemeManager.ThemeMode.SYSTEM -> {
            (context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES)
        }
    }

    // Calcola se la schermata è stata scrollata
    val isScrolled = scrollState.value > 10

    // Stati del DashboardViewModel
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    val user by dashboardViewModel.user.collectAsState()
    val userProfile by dashboardViewModel.userProfile.collectAsState()

    // Stati del SubscriptionViewModel
    val subscriptionState by subscriptionViewModel.subscriptionState.collectAsState()
    val updatePlanState by subscriptionViewModel.updatePlanState.collectAsState()
    val subscription by remember { derivedStateOf {
        if (subscriptionState is SubscriptionViewModel.SubscriptionState.Success) {
            (subscriptionState as SubscriptionViewModel.SubscriptionState.Success).subscription
        } else null
    }}

    // Messaggi Snackbar
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(true) }

    // Activity Result Launcher per pagamenti PayPal
    val paymentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        PaymentHelper.processPaymentResult(
            resultCode = result.resultCode,
            data = result.data,
            onSuccess = { orderId ->
                // Pagamento completato con successo
                coroutineScope.launch {
                    snackbarMessage = "Pagamento completato con successo"
                    isSuccess = true
                    showSnackbar = true
                    // Ricarica l'abbonamento
                    subscriptionViewModel.loadSubscription()
                }
            },
            onFailure = { errorMessage ->
                // Pagamento fallito
                coroutineScope.launch {
                    snackbarMessage = "Errore nel pagamento: $errorMessage"
                    isSuccess = false
                    showSnackbar = true
                }
            }
        )
    }

    // Carica i dati all'avvio della composable
    LaunchedEffect(Unit) {
        dashboardViewModel.loadDashboardData(sessionManager)
        subscriptionViewModel.loadSubscription()  // Carica l'abbonamento
    }

    // Osserva gli stati di aggiornamento piano
    LaunchedEffect(updatePlanState) {
        when (updatePlanState) {
            is SubscriptionViewModel.UpdatePlanState.Success -> {
                snackbarMessage = (updatePlanState as SubscriptionViewModel.UpdatePlanState.Success).message
                isSuccess = true
                showSnackbar = true
                subscriptionViewModel.resetUpdatePlanState()
            }
            is SubscriptionViewModel.UpdatePlanState.Error -> {
                snackbarMessage = (updatePlanState as SubscriptionViewModel.UpdatePlanState.Error).message
                isSuccess = false
                showSnackbar = true
                subscriptionViewModel.resetUpdatePlanState()
            }
            else -> {}
        }
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
        Box(modifier = Modifier.fillMaxSize()) {
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

                            // NUOVO: Card Abbonamento nella Dashboard
                            if (subscription != null) {
                                DashboardSubscriptionCard(
                                    subscription = subscription,
                                    isDarkTheme = isDarkTheme,
                                    onViewDetails = onNavigateToSubscription
                                )
                            } else if (subscriptionState is SubscriptionViewModel.SubscriptionState.Loading) {
                                // Mostra placeholder durante il caricamento
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .padding(bottom = 16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = Indigo600
                                        )
                                    }
                                }
                            }

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
                                                .clip(RoundedCornerShape(24.dp))
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

                            // Custom Exercises Card (pulsante ora passa onNavigateToUserExercises)
                            CustomExercisesCard(onClick = onNavigateToUserExercises)

                            Spacer(modifier = Modifier.height(24.dp))

                            // Support Banner - Modificato per mostrare PayPal demo
                            SupportBanner(
                                onClick = {
                                    // NUOVO: Avvia una donazione di test con PayPal
                                    PaymentHelper.startPayPalPayment(
                                        context = context,
                                        amount = 5.0,  // 5 euro di donazione
                                        type = "donation",
                                        message = "Grazie per il tuo supporto!",
                                        resultLauncher = paymentLauncher
                                    )
                                }
                            )

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

            // Mostra Snackbar
            if (showSnackbar) {
                SnackbarMessage(
                    message = snackbarMessage,
                    isSuccess = isSuccess,
                    onDismiss = { showSnackbar = false },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

// Versione aggiornata per accettare onClick
@Composable
fun CustomExercisesCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GradientUtils.purpleGradient)
            .clickable { onClick() },
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
                    onClick = onClick,
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
fun SupportBanner(onClick: () -> Unit = {}) {
    var showDonationDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val paymentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        PaymentHelper.processPaymentResult(
            resultCode = result.resultCode,
            data = result.data,
            onSuccess = { orderId ->
                Toast.makeText(context, "Grazie per la tua donazione!", Toast.LENGTH_SHORT).show()
            },
            onFailure = { errorMessage ->
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Mostra il dialogo di donazione quando richiesto
    if (showDonationDialog) {
        DonationDialog(
            onDismiss = { showDonationDialog = false },
            onDonate = { amount, message, showName ->
                PaymentHelper.startPayPalPayment(
                    context = context,
                    amount = amount,
                    type = "donation",
                    message = message,
                    displayName = showName,
                    resultLauncher = paymentLauncher
                )
                showDonationDialog = false
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
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
                    brush = Brush.horizontalGradient(
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
                    onClick = { showDonationDialog = true },
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