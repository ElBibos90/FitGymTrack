package com.fitgymtrack.app.models

import com.fitgymtrack.app.enums.NotificationPriority
import com.fitgymtrack.app.enums.NotificationSource
import com.fitgymtrack.app.enums.NotificationType
import java.util.UUID

/**
 * Modello principale per le notifiche
 */
data class Notification(
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationType,
    val source: NotificationSource,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val actionData: String? = null,        // JSON con dati per azioni specifiche
    val expiryDate: Long? = null,          // Timestamp di scadenza (null = non scade mai)
    val imageUrl: String? = null,          // URL immagine per messaggi rich
    val deepLink: String? = null           // Deep link per azioni dirette
) {
    /**
     * Verifica se la notifica è scaduta
     */
    fun isExpired(): Boolean {
        return expiryDate?.let { expiry ->
            System.currentTimeMillis() > expiry
        } ?: false
    }

    /**
     * Verifica se la notifica è recente (meno di 24 ore)
     */
    fun isRecent(): Boolean {
        val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return timestamp > twentyFourHoursAgo
    }

    /**
     * Restituisce il colore della priorità per l'UI
     */
    fun getPriorityColor(): String {
        return when (priority) {
            NotificationPriority.LOW -> "#6B7280"      // Gray
            NotificationPriority.NORMAL -> "#3B82F6"   // Blue
            NotificationPriority.HIGH -> "#F59E0B"     // Orange
            NotificationPriority.URGENT -> "#EF4444"   // Red
        }
    }
}

/**
 * Informazioni su aggiornamenti app
 */
data class AppUpdateInfo(
    val newVersion: String,
    val newVersionCode: Int,
    val currentVersion: String = android.os.Build.VERSION.RELEASE,
    val currentVersionCode: Int = 0,
    val changelog: String,
    val isCritical: Boolean = false,
    val playStoreUrl: String,
    val downloadUrl: String? = null
) {
    /**
     * Verifica se è disponibile un aggiornamento
     */
    fun hasUpdate(): Boolean {
        return newVersionCode > currentVersionCode
    }

    /**
     * Restituisce una lista delle features del changelog
     */
    fun getChangelogItems(): List<String> {
        return changelog.split("\n")
            .filter { it.isNotBlank() }
            .map { it.trim().removePrefix("•").removePrefix("-").trim() }
    }
}

/**
 * Messaggio diretto dallo sviluppatore
 */
data class DirectMessage(
    val id: String,
    val title: String,
    val message: String,
    val isMarkdown: Boolean = false,
    val senderName: String = "FitGymTrack Team",
    val timestamp: Long,
    val expiryDate: Long? = null,
    val targetUserId: Int? = null,         // null = broadcast message
    val imageUrl: String? = null,
    val actionButton: ActionButton? = null
) {
    /**
     * Verifica se è un messaggio broadcast (per tutti gli utenti)
     */
    fun isBroadcast(): Boolean = targetUserId == null
}

/**
 * Bottone di azione per messaggi
 */
data class ActionButton(
    val text: String,
    val action: String,                    // "url", "deeplink", "dismiss"
    val data: String                       // URL, deep link, o dati azione
)

/**
 * Risposta API per check versione
 */
data class AppVersionResponse(
    val success: Boolean,
    val update_available: Boolean,
    val latest_version: String,
    val latest_version_code: Int,
    val is_critical: Boolean = false,
    val changelog: String,
    val play_store_url: String,
    val message: String? = null
)

/**
 * Risposta API per messaggi utente
 */
data class UserMessagesResponse(
    val success: Boolean,
    val messages: List<DirectMessage>,
    val unread_count: Int,
    val message: String? = null
)

/**
 * Richiesta per marcare messaggio come letto
 */
data class MarkMessageReadRequest(
    val message_id: String
)

/**
 * Statistiche notifiche per debugging
 */
data class NotificationStats(
    val totalNotifications: Int,
    val unreadCount: Int,
    val byType: Map<NotificationType, Int>,
    val byPriority: Map<NotificationPriority, Int>,
    val expiredCount: Int,
    val lastCleanup: Long?
) {
    fun getTypeBreakdown(): String {
        return byType.entries.joinToString("\n") { "${it.key}: ${it.value}" }
    }

    fun getPriorityBreakdown(): String {
        return byPriority.entries.joinToString("\n") { "${it.key}: ${it.value}" }
    }
}