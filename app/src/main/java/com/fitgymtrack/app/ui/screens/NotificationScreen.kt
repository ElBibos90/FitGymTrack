package com.fitgymtrack.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.enums.NotificationPriority
import com.fitgymtrack.app.enums.NotificationType
import com.fitgymtrack.app.models.Notification
import com.fitgymtrack.app.repository.NotificationRepository
import com.fitgymtrack.app.ui.components.NotificationSectionHeader
import com.fitgymtrack.app.ui.components.NotificationUtils
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Schermata notifiche finale - STEP 2
 * MODIFICATO: Rimosso pulsante elimina notifiche
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToWorkouts: () -> Unit = {},
    onNavigateToStats: () -> Unit = {}
) {
    val context = LocalContext.current

    // CORRETTO: ViewModel creato dentro @Composable
    val viewModel: NotificationViewModel = remember {
        NotificationViewModel(NotificationRepository(context))
    }

    // Stati del ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val groupedNotifications by viewModel.groupedNotifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val hasUrgent by viewModel.hasUrgentNotifications.collectAsState()

    // Stato per refresh
    var isRefreshing by remember { mutableStateOf(false) }

    // Carica dati all'avvio
    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notifiche",
                            fontWeight = FontWeight.Bold
                        )
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = if (hasUrgent)
                                    MaterialTheme.colorScheme.error
                                else
                                    Indigo600
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                },
                actions = {
                    // Refresh
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            viewModel.refresh()
                        }
                    ) {
                        Icon(
                            imageVector = if (isRefreshing) Icons.Default.HourglassEmpty else Icons.Default.Refresh,
                            contentDescription = "Aggiorna"
                        )
                    }

                    // Mark all as read
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = { viewModel.markAllAsRead() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Segna tutte come lette"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        // Reset refresh quando UI state cambia
        LaunchedEffect(uiState) {
            if (uiState is com.fitgymtrack.app.viewmodel.NotificationUiState.Success) {
                isRefreshing = false
            }
        }

        NotificationContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            uiState = uiState,
            groupedNotifications = groupedNotifications,
            onMarkAsRead = { viewModel.markAsRead(it) },
            // RIMOSSO: onDeleteNotification - non più necessario
            onNotificationClick = { notification ->
                NotificationUtils.handleNotificationAction(
                    context = context,
                    notification = notification,
                    onNavigateToSubscription = onNavigateToSubscription,
                    onNavigateToWorkouts = onNavigateToWorkouts,
                    onNavigateToStats = onNavigateToStats,
                    onMarkAsRead = { viewModel.markAsRead(it) }
                )
            }
        )
    }
}

@Composable
fun NotificationContent(
    modifier: Modifier = Modifier,
    uiState: com.fitgymtrack.app.viewmodel.NotificationUiState,
    groupedNotifications: com.fitgymtrack.app.viewmodel.GroupedNotifications,
    onMarkAsRead: (String) -> Unit,
    // RIMOSSO: onDeleteNotification: (String) -> Unit,
    onNotificationClick: (Notification) -> Unit
) {
    when (uiState) {
        is com.fitgymtrack.app.viewmodel.NotificationUiState.Loading -> {
            LoadingState(modifier = modifier)
        }

        is com.fitgymtrack.app.viewmodel.NotificationUiState.Error -> {
            ErrorState(
                modifier = modifier,
                message = uiState.message
            )
        }

        is com.fitgymtrack.app.viewmodel.NotificationUiState.Success -> {
            if (groupedNotifications.isEmpty) {
                EmptyState(modifier = modifier)
            } else {
                NotificationList(
                    modifier = modifier,
                    groupedNotifications = groupedNotifications,
                    onMarkAsRead = onMarkAsRead,
                    // RIMOSSO: onDeleteNotification = onDeleteNotification,
                    onNotificationClick = onNotificationClick
                )
            }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Indigo600,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Caricamento notifiche...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    message: String
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Errore di caricamento",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Tutto pulito! 🎉",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Non hai notifiche al momento",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NotificationList(
    modifier: Modifier = Modifier,
    groupedNotifications: com.fitgymtrack.app.viewmodel.GroupedNotifications,
    onMarkAsRead: (String) -> Unit,
    // RIMOSSO: onDeleteNotification: (String) -> Unit,
    onNotificationClick: (Notification) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Oggi
        if (groupedNotifications.today.isNotEmpty()) {
            item {
                NotificationSectionHeader("Oggi")
            }
            items(
                items = groupedNotifications.today,
                key = { it.id }
            ) { notification ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = onMarkAsRead,
                        // RIMOSSO: onDelete = onDeleteNotification,
                        onClick = { onNotificationClick(notification) }
                    )
                }
            }
        }

        // Ieri
        if (groupedNotifications.yesterday.isNotEmpty()) {
            item {
                NotificationSectionHeader("Ieri")
            }
            items(
                items = groupedNotifications.yesterday,
                key = { it.id }
            ) { notification ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = onMarkAsRead,
                        // RIMOSSO: onDelete = onDeleteNotification,
                        onClick = { onNotificationClick(notification) }
                    )
                }
            }
        }

        // Questa settimana
        if (groupedNotifications.thisWeek.isNotEmpty()) {
            item {
                NotificationSectionHeader("Questa settimana")
            }
            items(
                items = groupedNotifications.thisWeek,
                key = { it.id }
            ) { notification ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = onMarkAsRead,
                        // RIMOSSO: onDelete = onDeleteNotification,
                        onClick = { onNotificationClick(notification) }
                    )
                }
            }
        }

        // Più vecchie
        if (groupedNotifications.older.isNotEmpty()) {
            item {
                NotificationSectionHeader("Più vecchie")
            }
            items(
                items = groupedNotifications.older,
                key = { it.id }
            ) { notification ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = onMarkAsRead,
                        // RIMOSSO: onDelete = onDeleteNotification,
                        onClick = { onNotificationClick(notification) }
                    )
                }
            }
        }

        // Footer spacer
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onMarkAsRead: (String) -> Unit,
    // RIMOSSO: onDelete: (String) -> Unit,
    onClick: () -> Unit
) {
    val priorityColor = Color(android.graphics.Color.parseColor(notification.getPriorityColor()))
    val isUrgent = notification.priority == NotificationPriority.URGENT

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else if (isUrgent) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isUrgent) 6.dp else 2.dp
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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Header con priorità e titolo
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Priority indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(priorityColor)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = notification.title,
                            fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (notification.isRead)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface
                        )

                        if (!notification.isRead) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Indigo600)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Message
                    Text(
                        text = notification.message,
                        color = if (notification.isRead)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer con tipo e timestamp
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = NotificationUtils.getNotificationIcon(notification.type),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = NotificationUtils.getNotificationTypeLabel(notification.type),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = NotificationUtils.formatTimestamp(notification.timestamp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // MODIFICATO: Solo pulsante "Mark as read" - rimosso pulsante elimina
                if (!notification.isRead) {
                    IconButton(
                        onClick = { onMarkAsRead(notification.id) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = "Segna come letta",
                            modifier = Modifier.size(20.dp),
                            tint = Indigo600
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = Indigo600,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

// === UTILITY FUNCTIONS ===

fun handleNotificationAction(
    context: Context,
    notification: Notification,
    onNavigateToSubscription: () -> Unit,
    onNavigateToWorkouts: () -> Unit,
    onNavigateToStats: () -> Unit,
    onMarkAsRead: (String) -> Unit
) {
    // Marca come letta se non lo è già
    if (!notification.isRead) {
        onMarkAsRead(notification.id)
    }

    // Gestisci azione basata sul tipo
    when (notification.type) {
        NotificationType.SUBSCRIPTION_EXPIRY,
        NotificationType.SUBSCRIPTION_EXPIRED,
        NotificationType.LIMIT_REACHED -> {
            onNavigateToSubscription()
        }

        NotificationType.APP_UPDATE -> {
            // Apri Play Store
            try {
                val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=com.fitgymtrack")
                    setPackage("com.android.vending")
                }
                context.startActivity(playStoreIntent)
            } catch (e: Exception) {
                // Fallback a browser
                val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=com.fitgymtrack")
                }
                context.startActivity(browserIntent)
            }
        }

        NotificationType.WORKOUT_COMPLETED -> {
            onNavigateToWorkouts()
        }

        NotificationType.ACHIEVEMENT -> {
            onNavigateToStats()
        }

        else -> {
            // Per altri tipi, nessuna azione specifica
        }
    }
}

fun getNotificationIcon(type: NotificationType): String {
    return when (type) {
        NotificationType.SUBSCRIPTION_EXPIRY, NotificationType.SUBSCRIPTION_EXPIRED -> "👑"
        NotificationType.LIMIT_REACHED -> "⚠️"
        NotificationType.APP_UPDATE -> "📱"
        NotificationType.DIRECT_MESSAGE -> "💬"
        NotificationType.WORKOUT_COMPLETED -> "💪"
        NotificationType.ACHIEVEMENT -> "🏆"
        NotificationType.REMINDER -> "⏰"
    }
}

fun getNotificationTypeLabel(type: NotificationType): String {
    return when (type) {
        NotificationType.SUBSCRIPTION_EXPIRY -> "Abbonamento"
        NotificationType.SUBSCRIPTION_EXPIRED -> "Abbonamento"
        NotificationType.LIMIT_REACHED -> "Limite piano"
        NotificationType.APP_UPDATE -> "Aggiornamento"
        NotificationType.DIRECT_MESSAGE -> "Messaggio"
        NotificationType.WORKOUT_COMPLETED -> "Allenamento"
        NotificationType.ACHIEVEMENT -> "Traguardo"
        NotificationType.REMINDER -> "Promemoria"
    }
}

fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "Ora"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m fa"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h fa"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}