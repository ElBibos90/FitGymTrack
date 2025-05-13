package com.fitgymtrack.app.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.ui.components.SnackbarMessage
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
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Stati per la validazione
    var codeError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // Snackbar message
    var snackbarMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(true) }

    // Funzioni di validazione semplificate (più simili all'originale)
    fun validateCode(code: String): Boolean {
        if (code.isBlank()) {
            codeError = "Inserisci il codice di verifica"
            return false
        }
        codeError = null
        return true
    }

    fun validatePassword(password: String): Boolean {
        if (password.isBlank()) {
            passwordError = "Inserisci una nuova password"
            return false
        }
        passwordError = null
        return true
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): Boolean {
        if (confirmPassword.isBlank()) {
            confirmPasswordError = "Conferma la password"
            return false
        }

        if (password != confirmPassword) {
            confirmPasswordError = "Le password non coincidono"
            return false
        }

        confirmPasswordError = null
        return true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header con titolo e pulsante indietro
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = navigateToLogin) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Indietro",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = "Reimposta Password",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Spazio vuoto per bilanciare il layout
                    Box(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Icona chiave stilizzata
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(40.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Indigo600.copy(alpha = 0.1f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Key",
                                tint = Indigo600,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Imposta una nuova password",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Inserisci il codice di verifica ricevuto via email e crea una nuova password sicura",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Reset Code Field
                OutlinedTextField(
                    value = resetCode,
                    onValueChange = {
                        resetCode = it
                        codeError = null  // Resetta l'errore quando l'utente digita
                    },
                    label = { Text("Codice di verifica") },
                    placeholder = { Text("Inserisci il codice ricevuto via email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (codeError != null) 4.dp else 16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,  // Cambiato da Number a Text per permettere codici alfanumerici
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (codeError != null) MaterialTheme.colorScheme.error else Indigo600,
                        unfocusedBorderColor = if (codeError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    ),
                    isError = codeError != null
                )

                // Messaggio errore codice
                if (codeError != null) {
                    Text(
                        text = codeError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, bottom = 16.dp)
                    )
                }

                // New Password Field
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        passwordError = null  // Resetta l'errore quando l'utente digita
                    },
                    label = { Text("Nuova password") },
                    placeholder = { Text("Inserisci la nuova password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = Indigo600
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Nascondi password" else "Mostra password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (passwordError != null) 4.dp else 16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (passwordError != null) MaterialTheme.colorScheme.error else Indigo600,
                        unfocusedBorderColor = if (passwordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    ),
                    isError = passwordError != null
                )

                // Messaggio errore password
                if (passwordError != null) {
                    Text(
                        text = passwordError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, bottom = 16.dp)
                    )
                }

                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        confirmPasswordError = null  // Resetta l'errore quando l'utente digita
                    },
                    label = { Text("Conferma password") },
                    placeholder = { Text("Conferma la nuova password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Conferma Password",
                            tint = Indigo600
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showConfirmPassword) "Nascondi password" else "Mostra password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (confirmPasswordError != null) 4.dp else 24.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (confirmPasswordError != null) MaterialTheme.colorScheme.error else Indigo600,
                        unfocusedBorderColor = if (confirmPasswordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    ),
                    isError = confirmPasswordError != null
                )

                // Messaggio errore conferma password
                if (confirmPasswordError != null) {
                    Text(
                        text = confirmPasswordError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, bottom = 16.dp)
                    )
                }

                // Submit Button
                Button(
                    onClick = {
                        // Validazione molto semplificata, come nell'originale
                        if (resetCode.isBlank()) {
                            codeError = "Inserisci il codice di verifica"
                        } else if (newPassword.isBlank()) {
                            passwordError = "Inserisci una nuova password"
                        } else if (newPassword != confirmPassword) {
                            confirmPasswordError = "Le password non coincidono"
                        } else {
                            // Resetta tutti gli errori
                            codeError = null
                            passwordError = null
                            confirmPasswordError = null

                            isLoading = true

                            coroutineScope.launch {
                                try {
                                    // Chiamata diretta all'API senza passare per Retrofit (esattamente come l'originale)
                                    val result = sendDirectResetRequest(token, resetCode, newPassword)
                                    isLoading = false

                                    if (result.first) {
                                        // Successo
                                        Toast.makeText(context, "Password reimpostata con successo", Toast.LENGTH_LONG).show()
                                        navigateToLogin()
                                    } else {
                                        // Errore
                                        snackbarMessage = result.second
                                        isSuccess = false
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    snackbarMessage = "Errore: ${e.message}"
                                    isSuccess = false
                                    Log.e("ResetPasswordScreen", "Errore: ${e.message}", e)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Indigo600
                    ),
                    enabled = resetCode.isNotBlank() &&
                            newPassword.isNotBlank() &&
                            confirmPassword.isNotBlank() &&
                            !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Reimposta Password",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Back to Login Button
                TextButton(
                    onClick = navigateToLogin,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Torna al login",
                        color = Indigo600,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Snackbar message
            if (snackbarMessage.isNotEmpty()) {
                SnackbarMessage(
                    message = snackbarMessage,
                    isSuccess = isSuccess,
                    onDismiss = { snackbarMessage = "" }
                )
            }
        }
    }
}

// Funzione per inviare direttamente la richiesta HTTP senza passare per Retrofit (mantenuta dalla versione originale)
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