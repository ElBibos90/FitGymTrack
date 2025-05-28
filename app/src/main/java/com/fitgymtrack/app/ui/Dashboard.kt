package com.fitgymtrack.app.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.FitGymTrackApplication
import com.fitgymtrack.app.ui.components.DashboardStatsPreview
import com.fitgymtrack.app.ui.components.DashboardSubscriptionCard
import com.fitgymtrack.app.ui.components.DonationDialog
import com.fitgymtrack.app.ui.components.FeedbackCard
import com.fitgymtrack.app.ui.components.SnackbarMessage
import com.fitgymtrack.app.ui.components.SubscriptionExpiredBanner
import com.fitgymtrack.app.ui.components.SubscriptionExpiryWarningBanner
import com.fitgymtrack.app.ui.payment.PaymentHelper
import com.fitgymtrack.app.ui.theme.GradientUtils
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.utils.ThemeManager
import com.fitgymtrack.app.viewmodel.DashboardViewModel
import com.fitgymtrack.app.viewmodel.StatsViewModel
import com.fitgymtrack.app.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.launch

@Composable
fun Dashboard(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToWorkoutPlans: () -> Unit,
    onNavigateToUserExercises: () -> Unit,
    onNavigateToWorkouts: () -> Unit,
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToFeedback: () -> Unit = {},
    dashboardViewModel: DashboardViewModel = viewModel(),
    subscriptionViewModel: SubscriptionViewModel = viewModel(),
    statsViewModel: StatsViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Ottieni il tema direttamente dall'app
    val themeManager = (context.applicationContext as FitGymTrackApplication).themeManager

    // Ottieni la modalità del tema
    val themeMode by themeManager.themeFlow.collectAsState(initial = ThemeManager.ThemeMode.SYSTEM)
    val configuration = LocalConfiguration.current
    // Determina se è dark theme
    val isDarkTheme = when (themeMode) {
        ThemeManager.ThemeMode.LIGHT -> false
        ThemeManager.ThemeMode.DARK -> true
        ThemeManager.ThemeMode.SYSTEM -> {
            (configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES)
        }
    }

    // Stati del DashboardViewModel
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    val user by dashboardViewModel.user.collectAsState()

    // Stati del SubscriptionViewModel
    val subscriptionState by subscriptionViewModel.subscriptionState.collectAsState()
    val updatePlanState by subscriptionViewModel.updatePlanState.collectAsState()

    // NUOVO: Stato per subscription scadute
    val expiredSubscriptionState by subscriptionViewModel.expiredSubscriptionState.collectAsState()

    val subscription by remember { derivedStateOf {
        if (subscriptionState is SubscriptionViewModel.SubscriptionState.Success) {
            (subscriptionState as SubscriptionViewModel.SubscriptionState.Success).subscription
        } else null
    }}

    // Stati del StatsViewModel condiviso
    val statsState by statsViewModel.statsState.collectAsState()
    val userStats by remember { derivedStateOf {
        when (val state = statsState) {
            is StatsViewModel.StatsState.Success -> state.stats
            else -> null
        }
    }}
    val statsLoading = statsState is StatsViewModel.StatsState.Loading

    // Messaggi Snackbar
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(true) }

    // Dialog per upgrade Premium
    var showPremiumDialog by remember { mutableStateOf(false) }

    // NUOVO: Stati per gestire i banner di scadenza
    var showExpiredBanner by remember { mutableStateOf(false) }
    var showExpiryWarningBanner by remember { mutableStateOf(false) }
    var daysRemaining by remember { mutableStateOf<Int?>(null) }

    // Activity Result Launcher per pagamenti PayPal
    val paymentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        PaymentHelper.processPaymentResult(
            resultCode = result.resultCode,
            data = result.data,
            onSuccess = { orderId ->
                coroutineScope.launch {
                    snackbarMessage = "Pagamento completato con successo"
                    isSuccess = true
                    showSnackbar = true
                    // Ricarica l'abbonamento
                    subscriptionViewModel.loadSubscription()
                }
            },
            onFailure = { errorMessage ->
                coroutineScope.launch {
                    snackbarMessage = "Errore nel pagamento: $errorMessage"
                    isSuccess = false
                    showSnackbar = true
                }
            }
        )
    }

    // Carica i dati all'avvio della composable
    LaunchedEffect(key1 = Unit) {
        Log.d("Dashboard", "=== INIZIO CARICAMENTO DASHBOARD ===")
        dashboardViewModel.loadDashboardData(sessionManager)

        // MODIFICATO: Carica subscription con controllo scadenze
        subscriptionViewModel.loadSubscription(checkExpired = true)
    }

    // Carica le statistiche sempre (per tutti gli utenti)
    LaunchedEffect(user) {
        val currentUser = user
        if (currentUser != null) {
            Log.d("Dashboard", "🔄 Caricamento statistiche per utente: ${currentUser.id}")
            statsViewModel.setSessionManager(sessionManager)
            statsViewModel.loadStats(currentUser.id, forceReload = true)
        }
    }

    // RIABILITATO: Osserva gli stati di scadenza subscription
    LaunchedEffect(expiredSubscriptionState) {
        val currentState = expiredSubscriptionState // Salva in una variabile locale
        when (currentState) {
            is SubscriptionViewModel.ExpiredSubscriptionState.Found -> {
                Log.d("Dashboard", "🚨 Trovate ${currentState.expiredCount} subscription scadute")
                showExpiredBanner = true
                snackbarMessage = "Abbonamento scaduto - Sei stato riportato al piano Free"
                isSuccess = false
                showSnackbar = true
            }
            is SubscriptionViewModel.ExpiredSubscriptionState.JustDetected -> {
                Log.d("Dashboard", "🚨 Subscription appena rilevata come scaduta")
                showExpiredBanner = true
            }
            else -> {}
        }
    }
    LaunchedEffect(expiredSubscriptionState) {
        val currentState = expiredSubscriptionState // Salva in una variabile locale
        when (currentState) {
            is SubscriptionViewModel.ExpiredSubscriptionState.Found -> {
                Log.d("Dashboard", "🚨 Trovate ${currentState.expiredCount} subscription scadute")
                showExpiredBanner = true
                snackbarMessage = "Abbonamento scaduto - Sei stato riportato al piano Free"
                isSuccess = false
                showSnackbar = true
            }
            is SubscriptionViewModel.ExpiredSubscriptionState.JustDetected -> {
                Log.d("Dashboard", "🚨 Subscription appena rilevata come scaduta")
                showExpiredBanner = true
            }
            else -> {}
        }
    }

    // NUOVO: Controllo locale della scadenza subscription
    LaunchedEffect(subscription) {
        subscription?.let { sub ->
            // Se è un piano a pagamento, controlla i giorni rimanenti
            if (sub.price > 0.0 && sub.end_date != null) {
                try {
                    // Controllo semplificato della data di scadenza
                    Log.d("Dashboard", "Subscription Premium trovata, end_date: ${sub.end_date}")

                    // Per ora mostriamo un avviso generico se la data è specificata
                    // Sostituisci con un calcolo reale delle date quando necessario
                    val remaining = 5 // Placeholder - implementare calcolo reale
                    daysRemaining = remaining

                    if (remaining in 1..7) {
                        showExpiryWarningBanner = true
                    }
                } catch (e: Exception) {
                    Log.e("Dashboard", "Errore calcolo giorni rimanenti: ${e.message}")
                }
            } else if (sub.planName == "Free") {
                // L'utente è già su piano Free, nascondi eventuali banner
                showExpiryWarningBanner = false
                showExpiredBanner = false
            }
        }
    }

    // Osserva gli stati di aggiornamento piano
    LaunchedEffect(updatePlanState) {
        when (updatePlanState) {
            is SubscriptionViewModel.UpdatePlanState.Success -> {
                snackbarMessage = (updatePlanState as SubscriptionViewModel.UpdatePlanState.Success).message
                isSuccess = true
                showSnackbar = true
                subscriptionViewModel.resetUpdatePlanState()

                // NUOVO: Nascondi i banner dopo un upgrade riuscito
                showExpiredBanner = false
                showExpiryWarningBanner = false
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

    // Dialog per upgrade Premium
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upgrade a Premium")
                }
            },
            text = {
                Column {
                    Text("Le statistiche dettagliate sono disponibili solo per gli utenti Premium.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Passa a Premium per sbloccare:",
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Statistiche dettagliate e grafici")
                    Text("• Analisi dei progressi")
                    Text("• Record personali")
                    Text("• E molto altro...")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPremiumDialog = false
                        onNavigateToSubscription()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Indigo600
                    )
                ) {
                    Text("Upgrade Now")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPremiumDialog = false }
                ) {
                    Text("Più tardi")
                }
            }
        )
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
                // NUOVO: Banner per subscription scaduta
                if (showExpiredBanner) {
                    SubscriptionExpiredBanner(
                        isVisible = showExpiredBanner,
                        onDismiss = {
                            showExpiredBanner = false
                            subscriptionViewModel.dismissExpiredNotification()
                        },
                        onUpgrade = {
                            showExpiredBanner = false
                            onNavigateToSubscription()
                        }
                    )
                }

                // NUOVO: Banner di avviso scadenza (per Premium in scadenza)
                daysRemaining?.let { days ->
                    if (showExpiryWarningBanner && subscription?.price != 0.0) {
                        SubscriptionExpiryWarningBanner(
                            daysRemaining = days,
                            isVisible = showExpiryWarningBanner,
                            onDismiss = {
                                showExpiryWarningBanner = false
                            },
                            onRenew = {
                                showExpiryWarningBanner = false
                                onNavigateToSubscription()
                            }
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header Welcome
                            Text(
                                text = "Benvenuto nella tua Dashboard",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Card Abbonamento nella Dashboard
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

                            // Preview Statistiche per TUTTI gli utenti
                            Log.d("Dashboard", "Mostrando preview statistiche - Loading: $statsLoading, Stats: ${userStats?.totalWorkouts ?: "null"}")

                            DashboardStatsPreview(
                                stats = userStats,
                                isLoading = statsLoading,
                                isDarkTheme = isDarkTheme,
                                onViewAllStats = {
                                    // Controlla se è Premium prima di navigare
                                    val currentSubscription = subscription
                                    if (currentSubscription != null && currentSubscription.price > 0.0) {
                                        // È Premium, naviga alle statistiche
                                        onNavigateToStats()
                                    } else {
                                        // Non è Premium, mostra dialog di upgrade
                                        showPremiumDialog = true
                                    }
                                }
                            )

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
                                        imageVector = Icons.AutoMirrored.Default.ArrowForward,
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
                            CustomExercisesCard(onClick = onNavigateToUserExercises)

                            Spacer(modifier = Modifier.height(24.dp))

                            // Support Banner
                            SupportBanner(
                                onClick = {
                                    PaymentHelper.startPayPalPayment(
                                        context = context,
                                        amount = 5.0,
                                        type = "donation",
                                        message = "Grazie per il tuo supporto!",
                                        resultLauncher = paymentLauncher
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Feedback Card
                            FeedbackCard(
                                onClick = onNavigateToFeedback
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

// Gli altri composable rimangono uguali...
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