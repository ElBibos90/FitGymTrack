package com.fitgymtrack.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.ui.theme.Indigo600

/**
 * Banner che mostra quando la subscription dell'utente è scaduta
 */
@Composable
fun SubscriptionExpiredBanner(
    isVisible: Boolean = true,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    expiredCount: Int = 1
) {
    // CORRETTO: Usa direttamente isVisible invece di remember
    // per reagire immediatamente ai cambiamenti di stato
    AnimatedVisibility(
        visible = isVisible, // Rimuoviamo il remember che causava problemi
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFEF2F2) // Rosso molto chiaro
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626), // Rosso
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Abbonamento Scaduto",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B), // Rosso scuro
                                fontSize = 16.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Il tuo abbonamento Premium è scaduto. Sei stato automaticamente riportato al piano Free con funzionalità limitate.",
                                color = Color(0xFF991B1B),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Piano Free include:",
                                color = Color(0xFF991B1B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = "• Massimo 3 schede di allenamento\n• Massimo 5 esercizi personalizzati\n• Funzionalità di base",
                                color = Color(0xFF991B1B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss // Rimuoviamo la logica di nascondimento locale
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Chiudi",
                            tint = Color(0xFF991B1B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss, // Chiamata diretta senza logica locale
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF991B1B)
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("OK")
                    }

                    Button(
                        onClick = onUpgrade, // Solo upgrade, niente dismiss locale
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Indigo600
                        )
                    ) {
                        Text("Vai a Premium")
                    }
                }
            }
        }
    }
}

/**
 * Banner più compatto per notificare la scadenza
 */
@Composable
fun SubscriptionExpiryWarningBanner(
    daysRemaining: Int,
    isVisible: Boolean = true,
    onDismiss: () -> Unit,
    onRenew: () -> Unit
) {
    // CORRETTO: Cambiata la condizione per includere anche daysRemaining = 0 (oggi)
    AnimatedVisibility(
        visible = isVisible && daysRemaining <= 7 && daysRemaining >= 0, // CAMBIATO: >= 0 invece di > 0
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFEF3C7) // Giallo chiaro
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFD97706), // Arancione
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // CORRETTO: Gestisce il caso speciale quando daysRemaining = 0
                    Text(
                        text = when (daysRemaining) {
                            0 -> "Il tuo abbonamento scade oggi!"
                            1 -> "Il tuo abbonamento scade domani"
                            else -> "Il tuo abbonamento scade tra $daysRemaining giorni"
                        },
                        color = Color(0xFF92400E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Solo pulsante OK - rimuoviamo il pulsante Rinnova
                TextButton(
                    onClick = onDismiss // Chiamata diretta senza logica locale
                ) {
                    Text(
                        "OK",
                        color = Color(0xFF92400E),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}