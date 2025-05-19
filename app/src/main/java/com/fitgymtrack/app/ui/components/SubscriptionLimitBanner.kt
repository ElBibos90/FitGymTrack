package com.fitgymtrack.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.ui.theme.Indigo600

@Composable
fun SubscriptionLimitBanner(
    resourceType: String,
    maxAllowed: Int,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    isVisible: Boolean = true
) {
    var visible by remember { mutableStateOf(isVisible) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFFBE6)
            )
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
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = getTitle(resourceType),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                fontSize = 16.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = getMessage(resourceType, maxAllowed),
                                color = Color(0xFF92400E),
                                fontSize = 14.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            visible = false
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Chiudi",
                            tint = Color(0xFF92400E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onUpgrade,
                    modifier = Modifier.align(Alignment.End),
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

private fun getTitle(resourceType: String): String {
    return when (resourceType) {
        "max_workouts" -> "Limite di schede raggiunto"
        "max_custom_exercises" -> "Limite di esercizi personalizzati raggiunto"
        else -> "Limite piano raggiunto"
    }
}

private fun getMessage(resourceType: String, maxAllowed: Int): String {
    return when (resourceType) {
        "max_workouts" -> "Hai raggiunto il limite di $maxAllowed schede disponibili con il piano Free. Passa al piano Premium per avere schede illimitate."
        "max_custom_exercises" -> "Hai raggiunto il limite di $maxAllowed esercizi personalizzati disponibili con il piano Free. Passa al piano Premium per avere esercizi illimitati."
        else -> "Hai raggiunto un limite del tuo piano corrente. Passa al piano Premium per sbloccare funzionalità illimitate."
    }
}