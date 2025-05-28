package com.fitgymtrack.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.enums.NotificationPriority
import com.fitgymtrack.app.enums.NotificationSource
import com.fitgymtrack.app.enums.NotificationType
import com.fitgymtrack.app.models.Notification
import com.fitgymtrack.app.models.NotificationStats
import com.fitgymtrack.app.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel per gestione notifiche
 */
class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val TAG = "NotificationViewModel"

    // === STATI PRINCIPALI ===

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    // StateFlow per notifiche
    val allNotifications: StateFlow<List<Notification>> = repository.getAllNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unreadNotifications: StateFlow<List<Notification>> = repository.getUnreadNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unreadCount: StateFlow<Int> = repository.getUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // === STATI DERIVATI ===

    val groupedNotifications: StateFlow<GroupedNotifications> = allNotifications
        .map { notifications ->
            groupNotificationsByDate(notifications)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GroupedNotifications()
        )

    val hasUrgentNotifications: StateFlow<Boolean> = allNotifications
        .map { notifications ->
            notifications.any { it.priority == NotificationPriority.URGENT && !it.isRead }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Per debugging
    private val _stats = MutableStateFlow<NotificationStats?>(null)
    val stats: StateFlow<NotificationStats?> = _stats.asStateFlow()

    init {
        Log.d(TAG, "🎯 NotificationViewModel inizializzato")
        loadInitialData()
    }

    // === AZIONI PUBBLICHE ===

    /**
     * Carica i dati iniziali
     */
    fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.value = NotificationUiState.Loading

                // Cleanup notifiche vecchie
                repository.cleanupOldNotifications()

                // Controlla aggiornamenti app (una volta al giorno)
                checkAppUpdates()

                // Carica statistiche per debugging
                loadStats()

                _uiState.value = NotificationUiState.Success

                Log.d(TAG, "✅ Dati iniziali caricati")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore caricamento dati: ${e.message}", e)
                _uiState.value = NotificationUiState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }

    /**
     * Marca una notifica come letta
     */
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📖 Marcando come letta: $notificationId")
                repository.markAsRead(notificationId)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore mark as read: ${e.message}", e)
            }
        }
    }

    /**
     * Marca tutte le notifiche come lette
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📖 Marcando tutte come lette")
                repository.markAllAsRead()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore mark all as read: ${e.message}", e)
            }
        }
    }

    /**
     * Elimina una notifica
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Eliminando notifica: $notificationId")
                repository.deleteNotification(notificationId)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore delete: ${e.message}", e)
            }
        }
    }

    /**
     * Forza controllo aggiornamenti
     */
    fun forceCheckUpdates() {
        viewModelScope.launch {
            checkAppUpdates(forceCheck = true)
        }
    }

    /**
     * Refresh completo
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Refresh completo")

                // Ricarica tutto
                loadInitialData()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore refresh: ${e.message}", e)
            }
        }
    }

    // === UTILITY TESTING ===

    /**
     * Crea notifica di test
     */
    fun createTestNotification(type: NotificationType) {
        viewModelScope.launch {
            try {
                val testNotification = when (type) {
                    NotificationType.SUBSCRIPTION_EXPIRY -> createTestSubscriptionExpiry()
                    NotificationType.SUBSCRIPTION_EXPIRED -> createTestSubscriptionExpired()
                    NotificationType.LIMIT_REACHED -> createTestLimitReached()
                    NotificationType.APP_UPDATE -> createTestAppUpdate()
                    NotificationType.DIRECT_MESSAGE -> createTestDirectMessage()
                    else -> createGenericTestNotification(type)
                }

                repository.saveNotification(testNotification)
                Log.d(TAG, "🧪 Notifica di test creata: $type")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore creazione test: ${e.message}", e)
            }
        }
    }

    /**
     * Reset completo per testing
     */
    fun clearAllNotifications() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ RESET: Eliminando tutte le notifiche")
                repository.clearAllNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore reset: ${e.message}", e)
            }
        }
    }

    // === METODI PRIVATI ===

    private suspend fun checkAppUpdates(forceCheck: Boolean = false) {
        try {
            Log.d(TAG, "🔄 Controllo aggiornamenti...")

            val result = repository.checkAppUpdates(forceCheck)
            result.fold(
                onSuccess = { updateInfo ->
                    if (updateInfo != null) {
                        Log.d(TAG, "🆕 Aggiornamento trovato: ${updateInfo.newVersion}")
                        // La notifica verrà creata dal NotificationManager
                    } else {
                        Log.d(TAG, "✅ Nessun aggiornamento disponibile")
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Errore controllo aggiornamenti: ${error.message}")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Eccezione controllo aggiornamenti: ${e.message}", e)
        }
    }

    private suspend fun loadStats() {
        try {
            _stats.value = repository.getNotificationStats()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore caricamento stats: ${e.message}", e)
        }
    }

    private fun groupNotificationsByDate(notifications: List<Notification>): GroupedNotifications {
        val now = System.currentTimeMillis()
        val today = getStartOfDay(now)
        val yesterday = today - (24 * 60 * 60 * 1000)
        val thisWeek = today - (7 * 24 * 60 * 60 * 1000)

        val grouped = notifications.groupBy { notification ->
            when {
                notification.timestamp >= today -> "Oggi"
                notification.timestamp >= yesterday -> "Ieri"
                notification.timestamp >= thisWeek -> "Questa settimana"
                else -> "Più vecchie"
            }
        }

        return GroupedNotifications(
            today = grouped["Oggi"] ?: emptyList(),
            yesterday = grouped["Ieri"] ?: emptyList(),
            thisWeek = grouped["Questa settimana"] ?: emptyList(),
            older = grouped["Più vecchie"] ?: emptyList()
        )
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // === FACTORY NOTIFICHE TEST ===

    private fun createTestSubscriptionExpiry(): Notification {
        return Notification(
            type = NotificationType.SUBSCRIPTION_EXPIRY,
            source = NotificationSource.LOCAL,
            title = "Abbonamento in scadenza",
            message = "Il tuo abbonamento Premium scade tra 3 giorni. Rinnova per continuare ad utilizzare tutte le funzionalità.",
            priority = NotificationPriority.HIGH,
            actionData = """{"action": "navigate", "destination": "subscription"}"""
        )
    }

    private fun createTestSubscriptionExpired(): Notification {
        return Notification(
            type = NotificationType.SUBSCRIPTION_EXPIRED,
            source = NotificationSource.LOCAL,
            title = "Abbonamento scaduto",
            message = "Il tuo abbonamento Premium è scaduto. Sei stato riportato al piano Free con funzionalità limitate.",
            priority = NotificationPriority.URGENT,
            actionData = """{"action": "navigate", "destination": "subscription"}"""
        )
    }

    private fun createTestLimitReached(): Notification {
        return Notification(
            type = NotificationType.LIMIT_REACHED,
            source = NotificationSource.LOCAL,
            title = "Limite raggiunto",
            message = "Hai raggiunto il limite di 3 schede per il piano Free. Passa a Premium per schede illimitate.",
            priority = NotificationPriority.HIGH,
            actionData = """{"action": "navigate", "destination": "subscription", "resource": "workouts"}"""
        )
    }

    private fun createTestAppUpdate(): Notification {
        return Notification(
            type = NotificationType.APP_UPDATE,
            source = NotificationSource.REMOTE,
            title = "Aggiornamento disponibile",
            message = "La versione 2.1.0 è disponibile con nuove statistiche avanzate e miglioramenti UI.",
            priority = NotificationPriority.NORMAL,
            actionData = """{"action": "url", "url": "https://play.google.com/store/apps/details?id=com.fitgymtrack"}"""
        )
    }

    private fun createTestDirectMessage(): Notification {
        return Notification(
            type = NotificationType.DIRECT_MESSAGE,
            source = NotificationSource.REMOTE,
            title = "Messaggio dallo sviluppatore",
            message = "Ciao! Stiamo lavorando a nuove funzionalità fantastiche. Resta sintonizzato per i prossimi aggiornamenti!",
            priority = NotificationPriority.NORMAL,
            actionData = """{"action": "dismiss"}"""
        )
    }

    private fun createGenericTestNotification(type: NotificationType): Notification {
        return Notification(
            type = type,
            source = NotificationSource.LOCAL,
            title = "Notifica Test",
            message = "Questa è una notifica di test per il tipo: $type",
            priority = NotificationPriority.NORMAL
        )
    }
}

// === STATI UI ===

sealed class NotificationUiState {
    object Loading : NotificationUiState()
    object Success : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}

data class GroupedNotifications(
    val today: List<Notification> = emptyList(),
    val yesterday: List<Notification> = emptyList(),
    val thisWeek: List<Notification> = emptyList(),
    val older: List<Notification> = emptyList()
) {
    val isEmpty: Boolean
        get() = today.isEmpty() && yesterday.isEmpty() && thisWeek.isEmpty() && older.isEmpty()

    val totalCount: Int
        get() = today.size + yesterday.size + thisWeek.size + older.size
}