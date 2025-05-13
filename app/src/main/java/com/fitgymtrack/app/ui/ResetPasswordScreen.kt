package com.fitgymtrack.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.ui.components.SnackbarMessage
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.viewmodel.PasswordResetViewModel
import kotlinx.coroutines.delay

@Composable
fun ResetPasswordScreen(
    token: String,
    navigateToLogin: () -> Unit,
    viewModel: PasswordResetViewModel = viewModel()
) {
    val context = LocalContext.current
    var resetCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    var snackbarMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(true) }

    // Stati per la validazione
    var codeError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val resetState by viewModel.resetState.collectAsState()

    // Funzioni di validazione
    fun validateCode(code: String): Boolean {
        if (code.isBlank()) {
            codeError = "Il codice di verifica non può essere vuoto"
            return false
        }

        if (code.length < 4 || !code.all { it.isDigit() }) {
            codeError = "Inserisci un codice di verifica valido"
            return false
        }

        codeError = null
        return true
    }

    fun validatePassword(password: String): Boolean {
        if (password.isBlank()) {
            passwordError = "La password non può essere vuota"
            return false
        }

        if (password.length < 8) {
            passwordError = "La password deve essere di almeno 8 caratteri"
            return false
        }

        // Verifica che la password abbia almeno un numero e una lettera maiuscola
        val hasNumber = password.any { it.isDigit() }
        val hasUpperCase = password.any { it.isUpperCase() }

        if (!hasNumber || !hasUpperCase) {
            passwordError = "La password deve contenere almeno un numero e una lettera maiuscola"
            return false
        }

        passwordError = null
        return true
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): Boolean {
        if (confirmPassword.isBlank()) {
            confirmPasswordError = "La conferma password non può essere vuota"
            return false
        }

        if (password != confirmPassword) {
            confirmPasswordError = "Le password non coincidono"
            return false
        }

        confirmPasswordError = null
        return true
    }

    LaunchedEffect(resetState) {
        when (resetState) {
            is PasswordResetViewModel.ResetState.Success -> {
                snackbarMessage = "Password reimpostata con successo"
                isSuccess = true
                // Delay briefly before navigation to allow snackbar to be seen
                delay(1500)
                navigateToLogin()
            }
            is PasswordResetViewModel.ResetState.Error -> {
                val errorMsg = (resetState as PasswordResetViewModel.ResetState.Error).message
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
                        if (codeError != null) {
                            validateCode(it)
                        }
                    },
                    label = { Text("Codice di verifica") },
                    placeholder = { Text("Inserisci il codice ricevuto via email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (codeError != null) 4.dp else 16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
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
                        if (passwordError != null) {
                            validatePassword(it)
                        }
                        // Ricontrolla anche la conferma password se cambiamo la password
                        if (confirmPassword.isNotEmpty()) {
                            validateConfirmPassword(it, confirmPassword)
                        }
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
                        if (confirmPasswordError != null) {
                            validateConfirmPassword(newPassword, it)
                        }
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
                        val isCodeValid = validateCode(resetCode)
                        val isPasswordValid = validatePassword(newPassword)
                        val isConfirmPasswordValid = validateConfirmPassword(newPassword, confirmPassword)

                        if (isCodeValid && isPasswordValid && isConfirmPasswordValid) {
                            viewModel.resetPassword(token, resetCode, newPassword)
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
                            resetState !is PasswordResetViewModel.ResetState.Loading
                ) {
                    if (resetState is PasswordResetViewModel.ResetState.Loading) {
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
                    onDismiss = {
                        snackbarMessage = ""
                        viewModel.resetResetState()
                    }
                )
            }
        }
    }
}