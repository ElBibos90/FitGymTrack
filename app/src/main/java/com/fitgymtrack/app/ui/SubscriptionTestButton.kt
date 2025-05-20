package com.fitgymtrack.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fitgymtrack.app.ui.test.TestActivity
import com.fitgymtrack.app.ui.theme.FitGymTrackTheme
import com.fitgymtrack.app.ui.theme.Indigo600

/**
 * Funzione che può essere aggiunta alla Dashboard per
 * accedere alla schermata di test delle funzionalità di abbonamento
 */
@Composable
fun SubscriptionTestButton() {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Test Abbonamenti",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Apri la schermata di test per abbonamenti e pagamenti",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    try {
                        val intent = Intent(context, TestActivity::class.java)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Errore nell'apertura della schermata di test: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Indigo600
                )
            ) {
                Text("Apri Test")
            }
        }
    }
}

/**
 * Questa è una semplice Activity indipendente che può essere usata
 * per testare rapidamente il pulsante di test
 */
class TestLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FitGymTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SubscriptionTestButton()
                    }
                }
            }
        }
    }
}