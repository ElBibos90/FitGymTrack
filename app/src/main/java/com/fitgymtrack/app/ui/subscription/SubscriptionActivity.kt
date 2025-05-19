package com.fitgymtrack.app.ui.subscription

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.ui.payment.PayPalPaymentActivity
import com.fitgymtrack.app.ui.theme.FitGymTrackTheme
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.launch

/**
 * Activity per gestire gli abbonamenti e i pagamenti
 */
class SubscriptionActivity : ComponentActivity() {

    private lateinit var viewModel: SubscriptionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[SubscriptionViewModel::class.java]

        setContent {
            FitGymTrackTheme {
                SubscriptionScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }

        // Carica i dati dell'abbonamento
        viewModel.loadSubscription()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SubscriptionScreen(
        viewModel: SubscriptionViewModel,
        onBack: () -> Unit
    ) {
        val subscriptionState by viewModel.subscriptionState.collectAsState()
        val updatePlanState by viewModel.updatePlanState.collectAsState()
        val paymentState by viewModel.paymentState.collectAsState()

        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        // Launcher per l'Activity di pagamento
        val paymentLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val isSuccessful = data?.getBooleanExtra("payment_successful", false) ?: false
                if (isSuccessful) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Pagamento completato con successo")
                        viewModel.loadSubscription()
                    }
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                val data = result.data
                val errorMessage = data?.getStringExtra("error_message")
                val cancelled = data?.getBooleanExtra("cancelled", false) ?: false
                val timeout = data?.getBooleanExtra("timeout", false) ?: false

                coroutineScope.launch {
                    when {
                        errorMessage != null -> snackbarHostState.showSnackbar("Errore: $errorMessage")
                        cancelled -> snackbarHostState.showSnackbar("Pagamento annullato")
                        timeout -> snackbarHostState.showSnackbar("Timeout del pagamento")
                        else -> snackbarHostState.showSnackbar("Pagamento non completato")
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
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
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (subscriptionState) {
                    is SubscriptionViewModel.SubscriptionState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is SubscriptionViewModel.SubscriptionState.Error -> {
                        val errorState = subscriptionState as SubscriptionViewModel.SubscriptionState.Error
                        ErrorMessageContent(
                            message = errorState.message,
                            onRetry = { viewModel.loadSubscription() }
                        )
                    }

                    is SubscriptionViewModel.SubscriptionState.Success -> {
                        val dataState = subscriptionState as SubscriptionViewModel.SubscriptionState.Success
                        val subscription = dataState.subscription

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(16.dp)
                        ) {
                            // Mostra l'abbonamento attuale
                            CurrentSubscriptionCard(
                                subscription = subscription,
                                usageCounts = Pair(
                                    subscription.currentCount ?: 0,
                                    subscription.currentCustomExercises ?: 0
                                )
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "Cambia piano",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Piano Free
                            SubscriptionPlanCard(
                                planName = "Free",
                                planPrice = 0.0,
                                features = mapOf(
                                    "Schede di allenamento" to "3",
                                    "Esercizi personalizzati" to "5",
                                    "Statistiche avanzate" to false,
                                    "Backup cloud" to false,
                                    "Nessuna pubblicità" to false
                                ),
                                isCurrentPlan = subscription.planName == "Free",
                                onSubscribe = {
                                    // Passa al piano Free
                                    viewModel.updatePlan(1)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Piano Premium
                            SubscriptionPlanCard(
                                planName = "Premium",
                                planPrice = 4.99,
                                features = mapOf(
                                    "Schede di allenamento" to "illimitate",
                                    "Esercizi personalizzati" to "illimitati",
                                    "Statistiche avanzate" to true,
                                    "Backup cloud" to true,
                                    "Nessuna pubblicità" to true
                                ),
                                isCurrentPlan = subscription.planName == "Premium",
                                onSubscribe = {
                                    // Inizializza il pagamento per il piano Premium
                                    startPayPalPayment(paymentLauncher, 4.99, 2)
                                }
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Banner donazione
                            DonationBanner(
                                onDonate = {
                                    // Inizializza il pagamento per una donazione
                                    startPayPalPayment(paymentLauncher, 5.0, type = "donation")
                                }
                            )
                        }
                    }

                    else -> { /* Non fare nulla per lo stato iniziale */ }
                }

                // Effetti per gestire gli stati di aggiornamento e pagamento
                LaunchedEffect(updatePlanState) {
                    when (updatePlanState) {
                        is SubscriptionViewModel.UpdatePlanState.Success -> {
                            snackbarHostState.showSnackbar(
                                (updatePlanState as SubscriptionViewModel.UpdatePlanState.Success).message
                            )
                            viewModel.resetUpdatePlanState()
                            viewModel.loadSubscription()
                        }
                        is SubscriptionViewModel.UpdatePlanState.Error -> {
                            snackbarHostState.showSnackbar(
                                (updatePlanState as SubscriptionViewModel.UpdatePlanState.Error).message
                            )
                            viewModel.resetUpdatePlanState()
                        }
                        else -> {}
                    }
                }

                LaunchedEffect(paymentState) {
                    when (paymentState) {
                        is SubscriptionViewModel.PaymentState.Success -> {
                            // Non utilizzato qui, gestito da PayPalPaymentActivity
                            viewModel.resetPaymentState()
                        }
                        is SubscriptionViewModel.PaymentState.Error -> {
                            snackbarHostState.showSnackbar(
                                (paymentState as SubscriptionViewModel.PaymentState.Error).message
                            )
                            viewModel.resetPaymentState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun startPayPalPayment(
        launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
        amount: Double,
        planId: Int? = null,
        type: String = "subscription"
    ) {
        val intent = Intent(this, PayPalPaymentActivity::class.java).apply {
            putExtra("amount", amount)
            putExtra("type", type)
            if (planId != null) {
                putExtra("plan_id", planId)
            }
        }

        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Errore nell'avvio del pagamento: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    fun CurrentSubscriptionCard(
        subscription: Subscription,
        usageCounts: Pair<Int, Int>
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if ((subscription.price ?: 0.0) > 0.0)
                    Color(0xFFEEF2FF)
                else
                    Color(0xFFF8FAFC)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if ((subscription.price ?: 0.0) > 0.0) Indigo600 else Color.Gray.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if ((subscription.price ?: 0.0) > 0.0)
                                Icons.Default.Star
                            else
                                Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if ((subscription.price ?: 0.0) > 0.0) Color.White else Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Piano ${subscription.planName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if ((subscription.price ?: 0.0) > 0.0)
                                "${subscription.price}€ al mese"
                            else
                                "Piano gratuito",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Utilizzo attuale
                Text(
                    text = "Utilizzo attuale:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Barra di progresso schede
                val (currentCount, maxWorkouts) = usageCounts
                val workoutsLimit = subscription.maxWorkouts ?: Int.MAX_VALUE
                val workoutsProgress = if (workoutsLimit > 0)
                    (currentCount.toFloat() / workoutsLimit.toFloat()).coerceIn(0f, 1f)
                else
                    0f

                Column(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Schede di allenamento:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (subscription.maxWorkouts == null)
                                "$currentCount/illimitate"
                            else
                                "$currentCount/${subscription.maxWorkouts}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { workoutsProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(top = 4.dp),
                        color = Indigo600,
                        trackColor = Color.LightGray.copy(alpha = 0.3f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Barra di progresso esercizi personalizzati
                val customExercisesCount = usageCounts.second
                val maxCustomExercises = subscription.maxCustomExercises ?: Int.MAX_VALUE

                val customExercisesProgress = if (maxCustomExercises > 0)
                    (customExercisesCount.toFloat() / maxCustomExercises.toFloat()).coerceIn(0f, 1f)
                else
                    0f

                Column(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Esercizi personalizzati:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (subscription.maxCustomExercises == null)
                                "$customExercisesCount/illimitati"
                            else
                                "$customExercisesCount/${subscription.maxCustomExercises}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { customExercisesProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(top = 4.dp),
                        color = Indigo600,
                        trackColor = Color.LightGray.copy(alpha = 0.3f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Funzionalità del piano
                Text(
                    text = "Il tuo piano include:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mostra le altre caratteristiche
                PlanFeatureItem(
                    name = "Statistiche avanzate",
                    isIncluded = subscription.advancedStats ?: false
                )

                PlanFeatureItem(
                    name = "Backup cloud",
                    isIncluded = subscription.cloudBackup ?: false
                )

                PlanFeatureItem(
                    name = "Nessuna pubblicità",
                    isIncluded = subscription.noAds ?: false
                )
            }
        }
    }

    @Composable
    fun SubscriptionPlanCard(
        planName: String,
        planPrice: Double,
        features: Map<String, Any>,
        isCurrentPlan: Boolean,
        onSubscribe: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (planName == "Premium")
                    Color(0xFFEEF2FF)
                else
                    Color(0xFFF8FAFC)
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
                        text = planName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (planPrice > 0)
                            "${planPrice}€/mese"
                        else
                            "Gratuito",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (planName == "Premium")
                            Indigo600
                        else
                            Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Elenco funzionalità
                features.forEach { (name, value) ->
                    when (value) {
                        is Boolean -> {
                            PlanFeatureItem(
                                name = name,
                                isIncluded = value
                            )
                        }
                        is String -> {
                            PlanFeatureItem(
                                name = name,
                                value = value
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSubscribe,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCurrentPlan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (planName == "Premium")
                            Indigo600
                        else
                            Color.Gray,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (isCurrentPlan)
                            "Piano attuale"
                        else if (planPrice > 0)
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
    fun PlanFeatureItem(
        name: String,
        value: String? = null,
        isIncluded: Boolean = true
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isIncluded)
                    Icons.Default.Check
                else
                    Icons.Default.Close,
                contentDescription = null,
                tint = if (isIncluded)
                    Color(0xFF4CAF50)
                else
                    Color.Gray
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (value != null)
                    "$name ($value)"
                else
                    name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isIncluded)
                    MaterialTheme.colorScheme.onSurface
                else
                    Color.Gray
            )
        }
    }

    @Composable
    fun DonationBanner(
        onDonate: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFDF2F8) // Rosa chiaro
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
                        tint = Color(0xFFEC4899) // Rosa
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Supporta FitGymTrack",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9D174D) // Rosa scuro
                        )

                        Text(
                            text = "Supporta lo sviluppo con una donazione",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFBE185D) // Rosa medio
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onDonate,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEC4899) // Rosa
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dona")
                }
            }
        }
    }

    @Composable
    fun ErrorMessageContent(
        message: String,
        onRetry: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                    text = "Errore",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Indigo600
                    )
                ) {
                    Text("Riprova")
                }
            }
        }
    }
}