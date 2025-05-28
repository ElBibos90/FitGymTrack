package com.fitgymtrack.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.enums.NotificationType
import com.fitgymtrack.app.models.Notification
import com.fitgymtrack.app.ui.theme.Indigo600
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility condivise per la gestione delle notifiche
 */
object NotificationUtils {

    /**
     * Gestisce l'azione quando si clicca su una notifica
     */
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

    /**
     * Restituisce l'emoji per il tipo di notifica
     */
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

    /**
     * Restituisce il label per il tipo di notifica
     */
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

    /**
     * Formatta il timestamp in modo user-friendly
     */
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
}

/**
 * Composable per header delle sezioni
 */
@Composable
fun NotificationSectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = Indigo600,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}