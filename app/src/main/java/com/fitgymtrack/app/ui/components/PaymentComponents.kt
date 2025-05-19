package com.fitgymtrack.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.PaymentViewModel
import kotlinx.coroutines.launch

/**
 * UI component for upgrading subscription via PayPal
 */
@Composable
fun SubscriptionUpgradeCard(
    sessionManager: SessionManager,
    currentSubscription: Subscription,
    planId: Int,
    planName: String,
    planPrice: Double,
    onUpgradeSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val viewModel: PaymentViewModel = viewModel()
    val paymentState by viewModel.paymentState.collectAsState()

    // Get current user ID
    var userId by remember { mutableStateOf<Int?>(null) }

    // Load user ID from session
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            sessionManager.getUserData().collect { user ->
                userId = user?.id
            }
        }
    }

    // Handle payment state changes
    LaunchedEffect(paymentState) {
        when (paymentState) {
            is PaymentViewModel.PaymentState.Success -> {
                // Payment initiated successfully, but we still need to wait for the user to complete
                // the payment in the browser. The actual subscription update will happen on the server
                // after the payment is completed.
            }
            else -> {}
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Abbonamento Premium",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sblocca funzionalità illimitate e supporta lo sviluppo!",
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "€$planPrice al mese",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    userId?.let {
                        viewModel.initiateSubscriptionPayment(
                            userId = it,
                            planId = planId,
                            amount = planPrice
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = userId != null &&
                        paymentState !is PaymentViewModel.PaymentState.Loading &&
                        (currentSubscription.planId != planId)
            ) {
                if (paymentState is PaymentViewModel.PaymentState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Elaborazione...")
                } else {
                    Text("Aggiorna con PayPal")
                }
            }

            // Show error if there is one
            if (paymentState is PaymentViewModel.PaymentState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (paymentState as PaymentViewModel.PaymentState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * UI component for donation via PayPal
 */
@Composable
fun DonationCard(
    sessionManager: SessionManager,
    onDonationSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val viewModel: PaymentViewModel = viewModel()
    val paymentState by viewModel.paymentState.collectAsState()

    // Get current user ID
    var userId by remember { mutableStateOf<Int?>(null) }

    // Donation form state
    var donationAmount by remember { mutableStateOf("5.00") }
    var donationMessage by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf(true) }

    // Load user ID from session
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            sessionManager.getUserData().collect { user ->
                userId = user?.id
            }
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Supporta lo sviluppo",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Fai una donazione per supportare FitGymTrack e aiutarci a sviluppare nuove funzionalità!",
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = donationAmount,
                onValueChange = {
                    // Allow only valid monetary values
                    if (it.isEmpty() || it.matches(Regex("^\\d+(\\.\\d{0,2})?\$"))) {
                        donationAmount = it
                    }
                },
                label = { Text("Importo (€)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = paymentState !is PaymentViewModel.PaymentState.Loading
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = donationMessage,
                onValueChange = { donationMessage = it },
                label = { Text("Messaggio (opzionale)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
                enabled = paymentState !is PaymentViewModel.PaymentState.Loading
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = displayName,
                    onCheckedChange = { displayName = it },
                    enabled = paymentState !is PaymentViewModel.PaymentState.Loading
                )

                Text(
                    text = "Mostra il mio nome nella lista dei donatori",
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    userId?.let {
                        val amount = donationAmount.toDoubleOrNull() ?: 5.0
                        if (amount > 0) {
                            viewModel.initiateDonation(
                                userId = it,
                                amount = amount,
                                message = donationMessage.takeIf { msg -> msg.isNotBlank() },
                                displayName = displayName
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = userId != null &&
                        paymentState !is PaymentViewModel.PaymentState.Loading &&
                        donationAmount.toDoubleOrNull() != null &&
                        (donationAmount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                if (paymentState is PaymentViewModel.PaymentState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Elaborazione...")
                } else {
                    Text("Dona con PayPal")
                }
            }

            // Show error if there is one
            if (paymentState is PaymentViewModel.PaymentState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (paymentState as PaymentViewModel.PaymentState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
        }
    }
}