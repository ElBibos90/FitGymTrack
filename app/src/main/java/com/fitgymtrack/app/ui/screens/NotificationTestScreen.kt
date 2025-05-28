package com.fitgymtrack.app.ui.screens

import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.models.AppUpdateInfo
import com.fitgymtrack.app.models.Notification
import com.fitgymtrack.app.repository.NotificationRepository
import com.fitgymtrack.app.ui.components.NotificationSectionHeader
import com.fitgymtrack.app.ui.components.NotificationUtils
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.utils.NotificationManager
import com.fitgymtrack.app.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Schermata di test per il sistema notifiche - STEP 1
 * Questa schermata sarà rimossa dopo i test
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTestScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // CORRETTO: ViewModel creato dentro @Composable
    val viewModel: NotificationViewModel = remember {
        NotificationViewModel(NotificationRepository(context))
    }

    val notificationManager = remember { NotificationManager.getInstance(context) }

    // Stati del ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val allNotifications by viewModel.allNotifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val groupedNotifications by viewModel.groupedNotifications.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val hasUrgent by viewModel.hasUrgentNotifications.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Notifiche", "Test Actions", "Debug Info")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notification Test")
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = if (hasUrgent) Color.Red else Indigo600
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = { viewModel.refresh() }
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }

                    // Mark all as read
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = { viewModel.markAllAsRead() }
                        ) {
                            Icon(Icons.Default.DoneAll, "Mark all read")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> NotificationsTab(
                    uiState = uiState,
                    groupedNotifications = groupedNotifications,
                    allNotifications = allNotifications,
                    onMarkAsRead = { viewModel.markAsRead(it) },
                    onDeleteNotification = { viewModel.deleteNotification(it) }
                )
                1 -> TestActionsTab(
                    context = context,
                    notificationManager = notificationManager,
                    viewModel = viewModel
                )
                2 -> DebugInfoTab(
                    stats = stats,
                    uiState = uiState
                )
            }
        }
    }
}

@Composable
fun NotificationsTab(
    uiState: com.fitgymtrack.app.viewmodel.NotificationUiState,
    groupedNotifications: com.fitgymtrack.app.viewmodel.GroupedNotifications,
    allNotifications: List<Notification>,
    onMarkAsRead: (String) -> Unit,
    onDeleteNotification: (String) -> Unit
) {
    when (uiState) {
        is com.fitgymtrack.app.viewmodel.NotificationUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is com.fitgymtrack.app.viewmodel.NotificationUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Errore: ${uiState.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        is com.fitgymtrack.app.viewmodel.NotificationUiState.Success -> {
            if (groupedNotifications.isEmpty) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nessuna notifica",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Tutto pulito! 🎉",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Today
                    if (groupedNotifications.today.isNotEmpty()) {
                        item {
                            NotificationSectionHeader("Oggi (${groupedNotifications.today.size})")
                        }
                        items(groupedNotifications.today) { notification ->
                            NotificationCard(
                                notification = notification,
                                onMarkAsRead = onMarkAsRead,
                                onDelete = onDeleteNotification
                            )
                        }
                    }

                    // Yesterday
                    if (groupedNotifications.yesterday.isNotEmpty()) {
                        item {
                            NotificationSectionHeader("Ieri (${groupedNotifications.yesterday.size})")
                        }
                        items(groupedNotifications.yesterday) { notification ->
                            NotificationCard(
                                notification = notification,
                                onMarkAsRead = onMarkAsRead,
                                onDelete = onDeleteNotification
                            )
                        }
                    }

                    // This week
                    if (groupedNotifications.thisWeek.isNotEmpty()) {
                        item {
                            NotificationSectionHeader("Questa settimana (${groupedNotifications.thisWeek.size})")
                        }
                        items(groupedNotifications.thisWeek) { notification ->
                            NotificationCard(
                                notification = notification,
                                onMarkAsRead = onMarkAsRead,
                                onDelete = onDeleteNotification
                            )
                        }
                    }

                    // Older
                    if (groupedNotifications.older.isNotEmpty()) {
                        item {
                            NotificationSectionHeader("Più vecchie (${groupedNotifications.older.size})")
                        }
                        items(groupedNotifications.older) { notification ->
                            NotificationCard(
                                notification = notification,
                                onMarkAsRead = onMarkAsRead,
                                onDelete = onDeleteNotification
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TestActionsTab(
    context: Context,
    notificationManager: NotificationManager,
    viewModel: com.fitgymtrack.app.viewmodel.NotificationViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Test Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Text(
                text = "Crea notifiche di test per verificare il funzionamento:",
                color = Color.Gray
            )
        }

        // Test buttons
        item {
            TestActionCard(
                title = "Subscription Expiry",
                description = "Notifica scadenza abbonamento (3 giorni)",
                icon = Icons.Default.Star,
                onClick = {
                    NotificationManager.createSubscriptionExpiry(
                        context = context,
                        daysRemaining = 3
                    )
                }
            )
        }

        item {
            TestActionCard(
                title = "Subscription Expired",
                description = "Notifica abbonamento scaduto",
                icon = Icons.Default.Warning,
                onClick = {
                    NotificationManager.createSubscriptionExpired(
                        context = context
                    )
                }
            )
        }

        item {
            TestActionCard(
                title = "Limit Reached",
                description = "Limite schede raggiunto (3/3)",
                icon = Icons.Default.Block,
                onClick = {
                    NotificationManager.createLimitReached(
                        context = context,
                        resourceType = "workouts",
                        maxAllowed = 3
                    )
                }
            )
        }

        item {
            TestActionCard(
                title = "App Update",
                description = "Aggiornamento app disponibile",
                icon = Icons.Default.SystemUpdate,
                onClick = {
                    val mockUpdate = AppUpdateInfo(
                        newVersion = "2.1.0",
                        newVersionCode = com.fitgymtrack.app.BuildConfig.VERSION_CODE + 1,
                        currentVersion = com.fitgymtrack.app.BuildConfig.VERSION_NAME,
                        currentVersionCode = com.fitgymtrack.app.BuildConfig.VERSION_CODE,
                        changelog = "• Test changelog\n• New features\n• Bug fixes",
                        isCritical = false,
                        playStoreUrl = "https://play.google.com/store/apps/details?id=com.fitgymtrack"
                    )
                    NotificationManager.createAppUpdate(
                        context = context,
                        updateInfo = mockUpdate
                    )
                }
            )
        }

        item {
            TestActionCard(
                title = "Create Multiple",
                description = "Crea tutte le notifiche di test",
                icon = Icons.Default.AddCircle,
                onClick = {
                    notificationManager.createTestNotifications()
                }
            )
        }

        item {
            Divider()
        }

        item {
            TestActionCard(
                title = "Clear All",
                description = "Elimina tutte le notifiche (RESET)",
                icon = Icons.Default.DeleteSweep,
                backgroundColor = Color.Red.copy(alpha = 0.1f),
                onClick = {
                    viewModel.clearAllNotifications()
                }
            )
        }
    }
}

@Composable
fun DebugInfoTab(
    stats: com.fitgymtrack.app.models.NotificationStats?,
    uiState: com.fitgymtrack.app.viewmodel.NotificationUiState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Debug Information",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        // UI State
        DebugInfoCard(
            title = "UI State",
            content = "Stato corrente: ${uiState::class.simpleName}"
        )

        // Statistics
        if (stats != null) {
            DebugInfoCard(
                title = "Statistiche Notifiche",
                content = buildString {
                    appendLine("Totale: ${stats.totalNotifications}")
                    appendLine("Non lette: ${stats.unreadCount}")
                    appendLine("Scadute: ${stats.expiredCount}")
                    appendLine()
                    appendLine("Per tipo:")
                    appendLine(stats.getTypeBreakdown())
                    appendLine()
                    appendLine("Per priorità:")
                    appendLine(stats.getPriorityBreakdown())
                    appendLine()
                    appendLine("Ultimo cleanup: ${
                        stats.lastCleanup?.let {
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
                        } ?: "Mai"
                    }")
                }
            )
        }

        // BuildConfig info
        DebugInfoCard(
            title = "App Version Info",
            content = buildString {
                appendLine("Current Version: ${com.fitgymtrack.app.BuildConfig.VERSION_NAME}")
                appendLine("Version Code: ${com.fitgymtrack.app.BuildConfig.VERSION_CODE}")
                appendLine("Build Type: ${com.fitgymtrack.app.BuildConfig.BUILD_TYPE}")
                appendLine("Package: ${com.fitgymtrack.app.BuildConfig.APPLICATION_ID}")
            }
        )
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onMarkAsRead: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val priorityColor = Color(android.graphics.Color.parseColor(notification.getPriorityColor()))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!notification.isRead) {
                    onMarkAsRead(notification.id)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
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
                            fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!notification.isRead) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Indigo600)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = notification.message,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = NotificationUtils.getNotificationIcon(notification.type),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = notification.type.name,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(notification.timestamp)),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Actions
                Column {
                    if (!notification.isRead) {
                        IconButton(
                            onClick = { onMarkAsRead(notification.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = "Mark as read",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onDelete(notification.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Red
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TestActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DebugInfoCard(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 14.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}