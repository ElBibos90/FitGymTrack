// app/src/main/java/com/fitgymtrack/app/ui/components/AutofillTextFields.kt
package com.fitgymtrack.app.ui.components

import android.content.Context
import android.os.Build
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillManager
import android.widget.EditText
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fitgymtrack.app.ui.theme.Indigo600

@Composable
fun AutofillEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Email",
    placeholder: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AutofillEditText(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            leadingIcon = Icons.Default.Email,
            autofillHints = arrayOf(View.AUTOFILL_HINT_EMAIL_ADDRESS),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            imeAction = imeAction,
            enabled = enabled,
            modifier = modifier
        )
    } else {
        // Fallback per Android < 8.0
        RegularTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            imeAction = imeAction,
            enabled = enabled,
            modifier = modifier
        )
    }
}

@Composable
fun AutofillUsernameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Username",
    placeholder: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AutofillEditText(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            leadingIcon = Icons.Default.Person,
            autofillHints = arrayOf(View.AUTOFILL_HINT_USERNAME),
            inputType = InputType.TYPE_CLASS_TEXT,
            imeAction = imeAction,
            enabled = enabled,
            modifier = modifier
        )
    } else {
        // Fallback per Android < 8.0
        RegularTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            leadingIcon = Icons.Default.Person,
            keyboardType = KeyboardType.Text,
            imeAction = imeAction,
            enabled = enabled,
            modifier = modifier
        )
    }
}

@Composable
fun AutofillPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Password",
    placeholder: String? = null,
    isNewPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    enabled: Boolean = true
) {
    var showPassword by remember { mutableStateOf(false) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Usa sempre AUTOFILL_HINT_PASSWORD (più compatibile)
        AutofillPasswordEditText(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            autofillHints = arrayOf(View.AUTOFILL_HINT_PASSWORD),
            showPassword = showPassword,
            onTogglePasswordVisibility = { showPassword = !showPassword },
            imeAction = imeAction,
            enabled = enabled,
            modifier = modifier
        )
    } else {
        // Fallback per Android < 8.0
        RegularPasswordField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            showPassword = showPassword,
            onTogglePasswordVisibility = { showPassword = !showPassword },
            imeAction = imeAction,
            enabled = enabled,
            modifier = modifier
        )
    }
}

@Composable
fun AutofillNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Nome completo",
    placeholder: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    enabled: Boolean = true
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AutofillEditText(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            leadingIcon = Icons.Default.AccountCircle,
            autofillHints = arrayOf(View.AUTOFILL_HINT_NAME),
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
            imeAction = imeAction,
            enabled = enabled,
            modifier = modifier
        )
    } else {
        // Fallback per Android < 8.0
        RegularTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            leadingIcon = Icons.Default.AccountCircle,
            keyboardType = KeyboardType.Text,
            imeAction = imeAction,
            enabled = enabled,
            modifier = modifier
        )
    }
}

// AndroidView wrapper con EditText nativo per autofill
@Composable
private fun AutofillEditText(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String?,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    autofillHints: Array<String>,
    inputType: Int,
    imeAction: ImeAction,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    LocalContext.current

    // Prendi i colori prima di entrare in AndroidView
    MaterialTheme.colorScheme.surface.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f).toArgb()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Leading Icon
            Icon(
                imageVector = leadingIcon,
                contentDescription = label,
                tint = Indigo600,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // AndroidView con EditText nativo
            AndroidView(
                factory = { context ->
                    EditText(context).apply {
                        // Configura autofill
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            setAutofillHints(*autofillHints)
                            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
                        }

                        // Configura input
                        this.inputType = inputType
                        this.isSingleLine = true

                        // Stile Material Design
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setTextColor(onSurfaceColor)
                        textSize = 16f

                        // Placeholder
                        hint = placeholder ?: label
                        setHintTextColor(onSurfaceVariantColor)

                        // Listener per cambiamenti testo
                        addTextChangedListener(object : android.text.TextWatcher {
                            override fun afterTextChanged(s: android.text.Editable?) {
                                val newText = s?.toString() ?: ""
                                if (newText != value) {
                                    onValueChange(newText)
                                }
                            }
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        })

                        isEnabled = enabled
                    }
                },
                update = { editText ->
                    if (editText.text.toString() != value) {
                        editText.setText(value)
                        editText.setSelection(value.length)
                    }
                    editText.isEnabled = enabled
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// AndroidView wrapper specifico per password
@Composable
private fun AutofillPasswordEditText(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String?,
    autofillHints: Array<String>,
    showPassword: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    imeAction: ImeAction,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    LocalContext.current

    // Prendi i colori prima di entrare in AndroidView
    MaterialTheme.colorScheme.surface.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f).toArgb()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Leading Icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = label,
                tint = Indigo600,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // AndroidView con EditText nativo per password
            AndroidView(
                factory = { context ->
                    EditText(context).apply {
                        // Configura autofill
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            setAutofillHints(*autofillHints)
                            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
                        }

                        // Configura input password
                        inputType = if (showPassword) {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        } else {
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        }
                        isSingleLine = true

                        // Stile Material Design
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setTextColor(onSurfaceColor)
                        textSize = 16f

                        // Placeholder
                        hint = placeholder ?: label
                        setHintTextColor(onSurfaceVariantColor)

                        // Listener per cambiamenti testo
                        addTextChangedListener(object : android.text.TextWatcher {
                            override fun afterTextChanged(s: android.text.Editable?) {
                                val newText = s?.toString() ?: ""
                                if (newText != value) {
                                    onValueChange(newText)
                                }
                            }
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        })

                        isEnabled = enabled
                    }
                },
                update = { editText ->
                    if (editText.text.toString() != value) {
                        editText.setText(value)
                        editText.setSelection(value.length)
                    }
                    editText.inputType = if (showPassword) {
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    } else {
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }
                    editText.isEnabled = enabled
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Trailing Icon per toggle password
            IconButton(onClick = onTogglePasswordVisibility) {
                Icon(
                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPassword) "Nascondi password" else "Mostra password",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Fallback per Android < 8.0
@Composable
private fun RegularTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String?,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = label,
                tint = Indigo600
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Indigo600,
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
        ),
        enabled = enabled
    )
}

// Fallback password field per Android < 8.0
@Composable
private fun RegularPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String?,
    showPassword: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    imeAction: ImeAction,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = label,
                tint = Indigo600
            )
        },
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisibility) {
                Icon(
                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPassword) "Nascondi password" else "Mostra password",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Indigo600,
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
        ),
        enabled = enabled
    )
}