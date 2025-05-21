package com.fitgymtrack.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.FitGymTrackApplication
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.ui.components.DonationDialog
import com.fitgymtrack.app.ui.components.SnackbarMessage
import com.fitgymtrack.app.ui.payment.PaymentHelper
import com.fitgymtrack.app.ui.theme.*
import com.fitgymtrack.app.utils.ThemeManager
import com.fitgymtrack.app.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionViewModel = viewModel(),
    themeManager: ThemeManager? = null // Aggiungiamo il parametro ThemeManager
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val appThemeManager = themeManager ?: remember {
        (context.applicationContext as? FitGymTrackApplication)?.themeManager
    }

    // Collezioniamo il tema corrente
    val themeMode by appThemeManager?.themeFlow?.collectAsState(initial = ThemeManager.ThemeMode.SYSTEM)
        ?: remember { mutableStateOf(ThemeManager.ThemeMode.SYSTEM) }

    // Determiniamo se il tema è dark
    val isDarkTheme = when (themeMode) {
        ThemeManager.ThemeMode.LIGHT -> false
        ThemeManager.ThemeMode.DARK -> true
        ThemeManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        else -> isSystemInDarkTheme()
    }

    // Stati del ViewModel
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    val updatePlanState by viewModel.updatePlanState.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()

    // Messaggio Snackbar
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
                    viewModel.loadSubscription()
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

    // Carica l'abbonamento all'avvio
    LaunchedEffect(key1 = Unit) {
        viewModel.loadSubscription()
    }

    // Gestisce gli stati di aggiornamento del piano
    LaunchedEffect(updatePlanState) {
        when (updatePlanState) {
            is SubscriptionViewModel.UpdatePlanState.Success -> {
                snackbarMessage = (updatePlanState as SubscriptionViewModel.UpdatePlanState.Success).message
                isSuccess = true
                showSnackbar = true
                viewModel.resetUpdatePlanState()
            }
            is SubscriptionViewModel.UpdatePlanState.Error -> {
                snackbarMessage = (updatePlanState as SubscriptionViewModel.UpdatePlanState.Error).message
                isSuccess = false
                showSnackbar = true
                viewModel.resetUpdatePlanState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Abbonamento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Contenuto principale
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                when (subscriptionState) {
                    is SubscriptionViewModel.SubscriptionState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is SubscriptionViewModel.SubscriptionState.Error -> {
                        val errorMsg = (subscriptionState as SubscriptionViewModel.SubscriptionState.Error).message
                        ErrorMessage(errorMsg) {
                            viewModel.loadSubscription()
                        }
                    }

                    is SubscriptionViewModel.SubscriptionState.Success -> {
                        val subscription = (subscriptionState as SubscriptionViewModel.SubscriptionState.Success).subscription

                        // Abbonamento corrente
                        CurrentSubscriptionCard(subscription, isDarkTheme)

                        Spacer(modifier = Modifier.height(24.dp))

                        // Piani disponibili
                        Text(
                            text = "Piani disponibili",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CurrentSubscriptionCard(subscription = subscription, isDarkTheme = isDarkTheme)

                        // Piano Free
                        SubscriptionPlanCard(
                            name = "Free",
                            price = 0.0,
                            features = listOf(
                                SubscriptionFeature("Salvataggio allenamenti", true, maxDetail = "max 3"),
                                SubscriptionFeature("Esercizi personalizzati", true, maxDetail = "max 5"),
                                SubscriptionFeature("Statistiche avanzate", false),
                                SubscriptionFeature("Backup cloud", false),
                                SubscriptionFeature("Nessuna pubblicità", false)
                            ),
                            isCurrentPlan = subscription.price == 0.0,
                            isDarkTheme = isDarkTheme,
                            onSubscribe = {
                                // Per il piano Free, aggiorna direttamente senza passare da PayPal
                                viewModel.updatePlan(1) // Assumiamo che 1 sia l'ID del piano Free
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Piano Premium
                        SubscriptionPlanCard(
                            name = "Premium",
                            price = 4.99,
                            features = listOf(
                                SubscriptionFeature("Salvataggio allenamenti", true, "illimitati"),
                                SubscriptionFeature("Esercizi personalizzati", true, "illimitati"),
                                SubscriptionFeature("Statistiche avanzate", true),
                                SubscriptionFeature("Backup cloud", true),
                                SubscriptionFeature("Nessuna pubblicità", true)
                            ),
                            isCurrentPlan = subscription.price > 0.0,
                            isDarkTheme = isDarkTheme,
                            onSubscribe = {
                                // MODIFICATO: Usa PaymentHelper per avviare il pagamento
                                PaymentHelper.startPayPalPayment(
                                    context = context,
                                    amount = 4.99,
                                    planId = 2, // ID del piano Premium
                                    resultLauncher = paymentLauncher
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Banner donazione (modificato per usare PaymentHelper)
                        DonationBanner(
                            onDonate = {
                                PaymentHelper.startPayPalPayment(
                                    context = context,
                                    amount = 5.0,
                                    type = "donation",
                                    message = "Grazie per il tuo supporto!",
                                    resultLauncher = paymentLauncher
                                )
                            }
                        )
                    }

                    else -> { /* Stato iniziale, non fare nulla */ }
                }
            }

            // Snackbar per i messaggi
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

@Composable
fun CurrentSubscriptionCard(subscription: Subscription, isDarkTheme: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (subscription.price > 0) {
                if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFEEF2FF)
            } else {
                if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (subscription.price > 0)
                                    Indigo600
                                else
                                    Color.Gray.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (subscription.price > 0)
                                Icons.Default.Star
                            else
                                Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (subscription.price > 0) Color.White else Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Piano ${subscription.planName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Text(
                            text = if (subscription.price > 0)
                                "${subscription.price}€ al mese"
                            else
                                "Piano gratuito",
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Funzionalità del piano
            Text(
                text = "Il tuo piano include:",
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Elenco funzionalità
            FeatureItem(
                text = if (subscription.maxWorkouts == null)
                    "Schede di allenamento illimitate"
                else
                    "Fino a ${subscription.maxWorkouts} schede di allenamento",
                isIncluded = true
            )

            FeatureItem(
                text = if (subscription.maxCustomExercises == null)
                    "Esercizi personalizzati illimitati"
                else
                    "Fino a ${subscription.maxCustomExercises} esercizi personalizzati",
                isIncluded = true
            )

            FeatureItem(
                text = "Statistiche avanzate",
                isIncluded = subscription.advancedStats
            )

            FeatureItem(
                text = "Backup cloud",
                isIncluded = subscription.cloudBackup
            )

            FeatureItem(
                text = "Nessuna pubblicità",
                isIncluded = subscription.noAds
            )
        }
    }
}

@Composable
fun SubscriptionPlanCard(
    name: String,
    price: Double,
    features: List<SubscriptionFeature>,
    isCurrentPlan: Boolean,
    isDarkTheme: Boolean,
    onSubscribe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (name == "Premium") {
                if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFEEF2FF)
            } else {
                if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
            }
        )
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
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Text(
                    text = if (price > 0) "${price}€/mese" else "Gratuito",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (name == "Premium") Indigo600 else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Elenco funzionalità
            features.forEach { feature ->
                FeatureItem(
                    text = feature.name + if (feature.maxDetail != null) " (${feature.maxDetail})" else "",
                    isIncluded = feature.isIncluded
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCurrentPlan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (name == "Premium")
                        Indigo600
                    else if (isDarkTheme)
                        Color.DarkGray
                    else
                        Color.Gray,
                    disabledContainerColor = if (isDarkTheme)
                        Color.DarkGray.copy(alpha = 0.3f)
                    else
                        Color.Gray.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = if (isCurrentPlan)
                        "Piano attuale"
                    else if (price > 0)
                        "Abbonati"
                    else
                        "Passa a Free",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}


@Composable
fun FeatureItem(
    text: String,
    isIncluded: Boolean
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isIncluded) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isIncluded) Color(0xFF34D399) else Color.Gray
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            color = if (isIncluded) MaterialTheme.colorScheme.onSurface else Color.Gray
        )
    }
}

@Composable
fun DonationBanner(
    isDarkTheme: Boolean = false,
    onDonate: () -> Unit = {}
) {
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Supporta FitGymTrack",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )

                        Text(
                            text = "Supporta lo sviluppo con una donazione",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showDonationDialog = true },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFEC4899)
                    )
                ) {
                    Text(text = "Dona")
                }
            }
        }
    }
}

@Composable
fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Errore",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry
        ) {
            Text("Riprova")
        }
    }
}

data class SubscriptionFeature(
    val name: String,
    val isIncluded: Boolean,
    val maxDetail: String? = null
)