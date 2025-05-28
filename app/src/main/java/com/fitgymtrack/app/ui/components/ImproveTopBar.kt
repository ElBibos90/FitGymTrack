package com.fitgymtrack.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.models.User
import com.fitgymtrack.app.repository.NotificationRepository
import com.fitgymtrack.app.ui.theme.Indigo600
import com.fitgymtrack.app.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedTopBar(
    user: User?,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onThemeToggle: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    isScrolled: Boolean = false
) {
    // NUOVO: Context per ViewModel
    val context = LocalContext.current

    // NUOVO: ViewModel per le notifiche
    val notificationViewModel: NotificationViewModel = remember {
        NotificationViewModel(NotificationRepository(context))
    }

    // NUOVO: Stati delle notifiche
    val unreadCount by notificationViewModel.unreadCount.collectAsState()
    val hasUrgent by notificationViewModel.hasUrgentNotifications.collectAsState()

    // Carica le notifiche all'avvio
    LaunchedEffect(Unit) {
        notificationViewModel.loadInitialData()
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isScrolled) {
            if (isDarkTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "backgroundColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            // Aggiungi padding per la status bar
            .statusBarsPadding(),
        color = backgroundColor,
        shadowElevation = if (isScrolled) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App logo - left side
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FitGymTrack",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Indigo600
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Indigo600)
                )
            }

            // Right side actions
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme toggle
                IconButton(
                    onClick = onThemeToggle
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Cambia tema",
                        tint = if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFF5C6BC0)
                    )
                }

                // NUOVO: Notifications con badge
                Box {
                    IconButton(
                        onClick = onNavigateToNotifications
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifiche",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    // Badge per notifiche non lette
                    if (unreadCount > 0) {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd),
                            containerColor = if (hasUrgent)
                                MaterialTheme.colorScheme.error
                            else
                                Indigo600
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // User profile
                if (user != null) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Indigo600, Color(0xFF8B5CF6))
                                )
                            )
                            .clickable { onNavigateToProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.username.firstOrNull()?.uppercase() ?: "U",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImprovedTopBarWithScroll(
    user: User?,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    scrolled: Boolean,
) {
    ImprovedTopBar(
        user = user,
        isDarkTheme = isDarkTheme,
        onThemeToggle = onThemeToggle,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToNotifications = onNavigateToNotifications,
        isScrolled = scrolled
    )
}