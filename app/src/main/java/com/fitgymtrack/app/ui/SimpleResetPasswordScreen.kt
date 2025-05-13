package com.fitgymtrack.app.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.models.PasswordResetConfirmRequest
import com.fitgymtrack.app.ui.theme.Indigo600
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun SimpleResetPasswordScreen(
    token: String,
    navigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var resetCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Reimposta Password",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 32.dp)
            )

            // Campi input
            OutlinedTextField(
                value = resetCode,
                onValueChange = { resetCode = it },
                label = { Text("Codice di verifica") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Nuova password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Conferma password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            // Messaggio errore
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Pulsante reset
            Button(
                onClick = {
                    // Validazione
                    when {
                        resetCode.isBlank() -> {
                            errorMessage = "Inserisci il codice di verifica"
                        }
                        newPassword.isBlank() -> {
                            errorMessage = "Inserisci una nuova password"
                        }
                        newPassword != confirmPassword -> {
                            errorMessage = "Le password non coincidono"
                        }
                        else -> {
                            errorMessage = ""
                            isLoading = true

                            coroutineScope.launch {
                                try {
                                    // Chiamata diretta all'API senza passare per Retrofit
                                    val result = sendDirectResetRequest(token, resetCode, newPassword)
                                    isLoading = false

                                    if (result.first) {
                                        // Successo
                                        Toast.makeText(context, "Password reimpostata con successo", Toast.LENGTH_LONG).show()
                                        navigateToLogin()
                                    } else {
                                        // Errore
                                        errorMessage = result.second
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = "Errore: ${e.message}"
                                    Log.e("ResetPasswordScreen", "Errore: ${e.message}", e)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("REIMPOSTA PASSWORD")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Link per tornare al login
            TextButton(onClick = navigateToLogin) {
                Text("Torna al login")
            }
        }
    }
}

// Funzione per inviare direttamente la richiesta HTTP senza passare per Retrofit
suspend fun sendDirectResetRequest(token: String, code: String, newPassword: String): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            // Prepara i dati da inviare
            val requestData = JSONObject().apply {
                put("token", token)
                put("code", code)
                put("newPassword", newPassword)
            }

            // Log per debug
            Log.d("ResetPasswordDirect", "Invio richiesta con: token=$token, code=$code")

            // Crea la connessione HTTP
            val url = URL("http://104.248.103.182/api/reset_simple.php")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Invia i dati
            connection.outputStream.use { os ->
                val input = requestData.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            // Leggi la risposta
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d("ResetPasswordDirect", "Risposta: $response")

            // Analizza la risposta JSON
            val jsonResponse = JSONObject(response)
            val success = jsonResponse.getBoolean("success")
            val message = jsonResponse.getString("message")

            Pair(success, message)

        } catch (e: Exception) {
            Log.e("ResetPasswordDirect", "Errore nell'invio della richiesta: ${e.message}", e)
            Pair(false, "Errore di connessione: ${e.message}")
        }
    }
}