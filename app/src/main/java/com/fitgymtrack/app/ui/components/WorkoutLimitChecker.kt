package com.fitgymtrack.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.viewmodel.SubscriptionViewModel
import com.fitgymtrack.app.viewmodel.WorkoutViewModel

/**
 * Componente helper per verificare se il piano attuale consente di creare altre schede,
 * e mostrare un messaggio appropriato.
 *
 * Da usare nella schermata WorkoutPlansScreen.
 */
@Composable
fun WorkoutLimitChecker(
    onCreateWorkout: () -> Unit,
    onUpgradePlan: () -> Unit,
    modifier: Modifier = Modifier,
    subscriptionViewModel: SubscriptionViewModel = viewModel(),
    workoutViewModel: WorkoutViewModel = viewModel()
) {
    val subscriptionState by subscriptionViewModel.subscriptionState.collectAsState()
    val limitsState by workoutViewModel.limitsState.collectAsState()

    remember(subscriptionState) {
        if (subscriptionState is SubscriptionViewModel.SubscriptionState.Success) {
            (subscriptionState as SubscriptionViewModel.SubscriptionState.Success).subscription
        } else null
    }

    LaunchedEffect(Unit) {
        // Controlla se l'utente può creare una nuova scheda
        workoutViewModel.checkLimitsBeforeCreate()
    }

    // Stato per mostrare il banner di limite
    var showLimitBanner by remember { mutableStateOf(false) }
    var maxAllowed by remember { mutableStateOf(0) }
    var currentCount by remember { mutableStateOf(0) }

    // Monitora limitsState
    LaunchedEffect(limitsState) {
        when (limitsState) {
            is WorkoutViewModel.LimitsState.LimitReached -> {
                val state = limitsState as WorkoutViewModel.LimitsState.LimitReached
                showLimitBanner = true
                maxAllowed = state.maxAllowed ?: 0
                currentCount = state.currentCount
            }
            is WorkoutViewModel.LimitsState.CanProceed -> {
                // L'utente può procedere, nascondi il banner
                showLimitBanner = false
                onCreateWorkout()  // Vai direttamente alla creazione
            }
            else -> { /* Non fare nulla per altri stati */ }
        }
    }

    // Mostra il banner di limite se necessario
    if (showLimitBanner) {
        WorkoutLimitBanner(
            currentCount = currentCount,
            maxAllowed = maxAllowed,
            onDismiss = {
                showLimitBanner = false
                workoutViewModel.resetLimitsState()
            },
            onUpgrade = onUpgradePlan,
            modifier = modifier
        )
    }
}

@Composable
fun WorkoutLimitBanner(
    currentCount: Int,
    maxAllowed: Int,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFBE6)  // Colore giallo chiaro per warning
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Limite di schede raggiunto",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)  // Marrone scuro
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progresso
                    val progress = currentCount.toFloat() / maxAllowed
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress.coerceIn(0f, 1f),
                        label = "progressAnimation"
                    )

                    Text(
                        text = "Hai $currentCount su $maxAllowed schede disponibili nel tuo piano attuale.",
                        color = Color(0xFF92400E)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFFF59E0B),
                        trackColor = Color(0xFFFEF3C7)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Passa al piano Premium per creare schede illimitate.",
                        color = Color(0xFF92400E)
                    )
                }
            }

            // Bottoni
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                // Bottone annulla
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF92400E)
                    )
                ) {
                    Text("Annulla")
                }

                // Bottone upgrade
                Button(
                    onClick = onUpgrade,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Indigo600
                    )
                ) {
                    Text("Passa a Premium")
                }
            }
        }
    }
}