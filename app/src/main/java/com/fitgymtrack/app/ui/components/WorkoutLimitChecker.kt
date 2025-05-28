package com.fitgymtrack.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitgymtrack.app.extensions.WorkoutNotificationExtensions
import com.fitgymtrack.app.ui.theme.Indigo600

/**
 * Componente helper per verificare se il piano attuale consente di creare altre schede.
 * AGGIORNATO: Usa il sistema di notifiche invece dei banner.
 */
@Composable
fun WorkoutLimitChecker(
    onCreateWorkout: () -> Unit,
    onUpgradePlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Stato per il fallback banner (solo per casi critici)
    var showFallbackBanner by remember { mutableStateOf(false) }
    var maxAllowed by remember { mutableIntStateOf(0) }
    var currentCount by remember { mutableIntStateOf(0) }

    // NUOVO: Usa il sistema di notifiche invece dei banner
    WorkoutNotificationExtensions.checkLimitsBeforeCreation(
        context = context,
        resourceType = "workouts",
        onLimitReached = {
            // OPZIONE 1: Solo notifica (raccomandato)
            // La notifica è già stata creata da WorkoutNotificationExtensions
            // L'utente la vedrà nell'icona campana

            // OPZIONE 2: Notifica + fallback banner per UX immediata
            // Decommentare se vuoi anche il banner come fallback
            /*
            showFallbackBanner = true
            maxAllowed = 3 // Valore di default per piano Free
            currentCount = 3
            */
        },
        onCanProceed = {
            // L'utente può creare la scheda, procedi
            onCreateWorkout()
        }
    )

    // Fallback banner (opzionale, solo se decommentato sopra)
    if (showFallbackBanner) {
        WorkoutLimitBanner(
            currentCount = currentCount,
            maxAllowed = maxAllowed,
            onDismiss = {
                showFallbackBanner = false
            },
            onUpgrade = onUpgradePlan,
            modifier = modifier
        )
    }
}

/**
 * Banner di fallback per casi critici (mantenuto per compatibilità)
 * Normalmente non verrà mostrato se usi solo le notifiche
 */
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
                        text = "Controlla le notifiche per maggiori dettagli. Passa al piano Premium per creare schede illimitate.",
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

/**
 * Versione semplificata per testing rapido
 */
@Composable
fun SimpleWorkoutLimitChecker(
    onCreateWorkout: () -> Unit,
    onUpgradePlan: () -> Unit = {}
) {
    WorkoutLimitChecker(
        onCreateWorkout = onCreateWorkout,
        onUpgradePlan = onUpgradePlan
    )
}