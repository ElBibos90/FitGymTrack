package com.fitgymtrack.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.ui.components.SnackbarMessage
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.viewmodel.PasswordResetViewModel
import kotlinx.coroutines.delay

@Composable
fun ForgotPasswordScreen(
    navigateBack: () -> Unit,
    navigateToResetPassword: (String) -> Unit,
    viewModel: PasswordResetViewModel = viewModel()
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    val resetRequestState by viewModel.resetRequestState.collectAsState()

    var snackbarMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(true) }

    // Stato per rendere possibile una validazione lato client
    var emailError by remember { mutableStateOf<String?>(null) }

    // Funzione di validazione per l'email
    fun validateEmail(email: String): Boolean {
        if (email.isBlank()) {
            emailError = "L'email non può essere vuota"
            return false
        }

        if (!email.contains("@") || !email.contains(".") || email.length < 6) {
            emailError = "Inserisci un indirizzo email valido"
            return false
        }

        emailError = null
        return true
    }

    LaunchedEffect(resetRequestState) {
        when (resetRequestState) {
            is PasswordResetViewModel.ResetRequestState.Success -> {
                val token = (resetRequestState as PasswordResetViewModel.ResetRequestState.Success).token
                snackbarMessage = "Ti abbiamo inviato un'email con le istruzioni"
                isSuccess = true
                // Delay briefly before navigation to allow snackbar to be seen
                delay(1500)
                navigateToResetPassword(token)
            }
            is PasswordResetViewModel.ResetRequestState.Error -> {
                val errorMsg = (resetRequestState as PasswordResetViewModel.ResetRequestState.Error).message
                snackbarMessage = errorMsg
                isSuccess = false
            }
            else -> { /* Do nothing for other states */ }
        }
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
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = "Password dimenticata",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Spazio vuoto per bilanciare il layout
                    Box(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Icona email stilizzata
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
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = Indigo600,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Reimposta la tua password",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ti invieremo un'email con le istruzioni per reimpostare la tua password",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        // Rimuovi l'errore quando l'utente inizia a digitare
                        if (emailError != null) {
                            validateEmail(it)
                        }
                    },
                    label = { Text("Email") },
                    placeholder = { Text("Inserisci la tua email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = Indigo600
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (emailError != null) 4.dp else 16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (emailError != null) MaterialTheme.colorScheme.error else Indigo600,
                        unfocusedBorderColor = if (emailError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    ),
                    isError = emailError != null
                )

                // Messaggio d'errore per la validazione dell'email
                if (emailError != null) {
                    Text(
                        text = emailError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, bottom = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (validateEmail(email)) {
                            viewModel.requestPasswordReset(email)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Indigo600
                    ),
                    enabled = email.isNotBlank() &&
                            resetRequestState !is PasswordResetViewModel.ResetRequestState.Loading
                ) {
                    if (resetRequestState is PasswordResetViewModel.ResetRequestState.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Invia istruzioni",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Back to Login Button
                TextButton(
                    onClick = navigateBack,
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
                    onDismiss = {
                        snackbarMessage = ""
                        viewModel.resetRequestState()
                    }
                )
            }
        }
    }
}