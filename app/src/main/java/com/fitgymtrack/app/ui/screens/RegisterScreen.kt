package com.fitgymtrack.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.ui.components.AutofillEmailField
import com.fitgymtrack.app.ui.components.AutofillNameField
import com.fitgymtrack.app.ui.components.AutofillPasswordField
import com.fitgymtrack.app.ui.components.AutofillUsernameField
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.viewmodel.AuthViewModel
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.OutlinedTextFieldDefaults

@Composable
fun RegisterScreen(
    navigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    val registerState by viewModel.registerState.collectAsState()

    // Gestione stato registrazione
    LaunchedEffect(registerState) {
        when (registerState) {
            is AuthViewModel.RegisterState.Success -> {
                navigateToLogin()
            }
            else -> {}
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.2f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Registrazione",
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Indigo600
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Crea il tuo account personale",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            // Form Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Username Field
                AutofillUsernameField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    placeholder = "Scegli un username",
                    imeAction = ImeAction.Next,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Password Field
                AutofillPasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    placeholder = "Crea una password sicura",
                    isNewPassword = true,
                    imeAction = ImeAction.Next,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Email Field
                AutofillEmailField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    placeholder = "Inserisci la tua email",
                    imeAction = ImeAction.Next,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Name Field
                AutofillNameField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nome completo",
                    placeholder = "Inserisci il tuo nome completo",
                    imeAction = ImeAction.Done,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )

                // Error Message
                if (registerState is AuthViewModel.RegisterState.Error) {
                    Text(
                        text = (registerState as AuthViewModel.RegisterState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Register Button
                Button(
                    onClick = {
                        viewModel.register(username, password, email, name)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Indigo600
                    ),
                    enabled = username.isNotBlank() && password.isNotBlank() &&
                            email.isNotBlank() && name.isNotBlank() &&
                            registerState !is AuthViewModel.RegisterState.Loading
                ) {
                    if (registerState is AuthViewModel.RegisterState.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Registrati",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Login Link
                TextButton(
                    onClick = navigateToLogin,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Hai già un account? Accedi",
                        color = Indigo600,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}