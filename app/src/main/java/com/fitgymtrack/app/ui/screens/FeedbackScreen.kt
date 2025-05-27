// Sostituisci completamente il file: app/src/main/java/com/fitgymtrack/app/ui/screens/FeedbackScreen.kt
package com.fitgymtrack.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitgymtrack.app.models.FeedbackSeverity
import com.fitgymtrack.app.models.FeedbackType
import com.fitgymtrack.app.models.LocalAttachment
import com.fitgymtrack.app.ui.components.SnackbarMessage
import com.fitgymtrack.app.utils.FileAttachmentManager
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.viewmodel.FeedbackViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    viewModel: FeedbackViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scrollState = rememberScrollState()

    // Inizializza il ViewModel
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
        val userData = sessionManager.getUserData().first()
        userData?.let { user ->
            viewModel.setUserEmailFromUsername(user.username)
        }
    }

    // Stati del ViewModel
    val feedbackType by viewModel.feedbackType.collectAsState()
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val email by viewModel.email.collectAsState()
    val severity by viewModel.severity.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val validationErrors by viewModel.validationErrors.collectAsState()
    val submitState by viewModel.submitState.collectAsState()

    // Stati locali
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (!granted) {
            snackbarMessage = "Permessi necessari per selezionare i file"
            showSnackbar = true
        }
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addAttachment(it) }
    }

    // Funzione per controllare e richiedere permessi
    val checkAndRequestPermissions = {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val hasPermissions = permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermissions) {
            filePickerLauncher.launch("*/*")
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    // Osserva lo stato di invio
    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is FeedbackViewModel.SubmitState.Success -> {
                showSuccessDialog = true
                viewModel.resetSubmitState()
            }
            is FeedbackViewModel.SubmitState.Error -> {
                snackbarMessage = state.message
                showSnackbar = true
                viewModel.resetSubmitState()
            }
            else -> {}
        }
    }

    // Dialog di successo
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF38A169),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Feedback Inviato!")
                }
            },
            text = {
                Text("Grazie per il tuo feedback! Riceverai una risposta il prima possibile.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onBack()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Invia Feedback",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aiutaci a migliorare FitGymTrack",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Errori di validazione
                if (validationErrors.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Errori di validazione:",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            validationErrors.forEach { error ->
                                Text(
                                    text = "• $error",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Tipo di feedback
                Text(
                    text = "Tipo di Feedback",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeedbackType.values().forEach { type ->
                        FeedbackTypeCard(
                            type = type,
                            isSelected = feedbackType == type,
                            onClick = { viewModel.updateFeedbackType(type) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Titolo
                OutlinedTextField(
                    value = title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("Titolo *") },
                    placeholder = { Text("Breve descrizione del problema/suggerimento") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                // Descrizione
                OutlinedTextField(
                    value = description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Descrizione *") },
                    placeholder = { Text("Descrivi in dettaglio il tuo feedback...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    minLines = 4,
                    maxLines = 8
                )

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = viewModel::updateEmail,
                    label = { Text("Email *") },
                    placeholder = { Text("La tua email per ricevere risposta") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                // SEZIONE ALLEGATI - QUESTA È LA PARTE IMPORTANTE!
                Text(
                    text = "📎 Allegati (opzionale)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Allega file (max 3, 5MB ciascuno)",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            OutlinedButton(
                                onClick = checkAndRequestPermissions,
                                enabled = attachments.size < 3
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aggiungi")
                            }
                        }

                        // Mostra allegati esistenti
                        if (attachments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))

                            attachments.forEachIndexed { index, attachment ->
                                AttachmentItem(
                                    attachment = attachment,
                                    onRemove = { viewModel.removeAttachment(index) }
                                )
                                if (index < attachments.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Formati: JPG, PNG, GIF, PDF, DOC, DOCX",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Gravità (solo per bug)
                if (feedbackType == FeedbackType.BUG) {
                    Text(
                        text = "Gravità",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeedbackSeverity.values().forEach { sev ->
                            SeverityChip(
                                severity = sev,
                                isSelected = severity == sev,
                                onClick = { viewModel.updateSeverity(sev) }
                            )
                        }
                    }
                }

                // Informazioni automatiche
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Informazioni Sistema",
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            AssistChip(
                                onClick = { },
                                label = {
                                    Text("Auto-raccolte", fontSize = 10.sp)
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Le informazioni del dispositivo verranno inviate automaticamente.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Pulsante invio
                Button(
                    onClick = { viewModel.submitFeedback() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = submitState !is FeedbackViewModel.SubmitState.Loading
                ) {
                    if (submitState is FeedbackViewModel.SubmitState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Invio in corso...")
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Invia Feedback", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Snackbar
            if (showSnackbar) {
                SnackbarMessage(
                    message = snackbarMessage,
                    isSuccess = false,
                    onDismiss = { showSnackbar = false },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

// Componente per mostrare un attachment
@Composable
fun AttachmentItem(
    attachment: LocalAttachment,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    attachment.mimeType.startsWith("image/") -> Icons.Default.Image
                    attachment.mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
                    else -> Icons.Default.AttachFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = FileAttachmentManager.formatFileSize(attachment.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Rimuovi",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun FeedbackTypeCard(
    type: FeedbackType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (type) {
        FeedbackType.BUG -> Icons.Default.BugReport to Color(0xFFE53E3E)
        FeedbackType.SUGGESTION -> Icons.Default.Lightbulb to Color(0xFFD69E2E)
        FeedbackType.QUESTION -> Icons.AutoMirrored.Filled.Help to Color(0xFF3182CE)
        FeedbackType.APPRECIATION -> Icons.Default.ThumbUp to Color(0xFF38A169)
    }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                color.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, color)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = type.displayName,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
fun SeverityChip(
    severity: FeedbackSeverity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = when (severity) {
        FeedbackSeverity.LOW -> Color(0xFF3182CE)
        FeedbackSeverity.MEDIUM -> Color(0xFFD69E2E)
        FeedbackSeverity.HIGH -> Color(0xFFDD6B20)
        FeedbackSeverity.CRITICAL -> Color(0xFFE53E3E)
    }

    FilterChip(
        onClick = onClick,
        label = { Text(severity.displayName, fontSize = 12.sp) },
        selected = isSelected,
        leadingIcon = if (severity == FeedbackSeverity.CRITICAL) {
            {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color
        )
    )
}