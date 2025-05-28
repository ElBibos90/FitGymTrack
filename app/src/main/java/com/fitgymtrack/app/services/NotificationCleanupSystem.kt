package com.fitgymtrack.app.services

import android.content.Context
import android.util.Log
import com.fitgymtrack.app.enums.NotificationPriority
import com.fitgymtrack.app.enums.NotificationType
import com.fitgymtrack.app.models.Notification
import com.fitgymtrack.app.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Sistema avanzato per pulizia e ottimizzazione notifiche
 */
class NotificationCleanupSystem private constructor(
    private val context: Context,
    private val repository: NotificationRepository
) {

    private val TAG = "NotificationCleanup"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        @Volatile
        private var INSTANCE: NotificationCleanupSystem? = null

        fun getInstance(context: Context): NotificationCleanupSystem {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationCleanupSystem(
                    context.applicationContext,
                    NotificationRepository(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }

        // Configurazione cleanup
        private const val MAX_NOTIFICATIONS = 100
        private const val MAX_AGE_DAYS = 30L
        private const val CLEANUP_INTERVAL_HOURS = 6L
        private const val MAX_DUPLICATES_SAME_TYPE = 3
    }

    private var isRunning = false

    /**
     * Avvia il sistema di cleanup periodico
     */
    fun startPeriodicCleanup() {
        if (isRunning) {
            Log.d(TAG, "⚠️ Cleanup già in esecuzione")
            return
        }

        scope.launch {
            isRunning = true
            Log.d(TAG, "🚀 Avvio sistema cleanup periodico")

            try {
                while (isRunning) {
                    performCleanup()
                    delay(TimeUnit.HOURS.toMillis(CLEANUP_INTERVAL_HOURS))
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore cleanup periodico: ${e.message}", e)
            } finally {
                isRunning = false
            }
        }
    }

    /**
     * Esegue cleanup completo
     */
    suspend fun performCleanup() {
        try {
            Log.d(TAG, "🧹 Inizio cleanup completo")

            val notifications = repository.getAllNotifications().first()
            val initialCount = notifications.size

            Log.d(TAG, "📊 Notifiche iniziali: $initialCount")

            // 1. Rimuovi notifiche scadute
            val afterExpiredCleanup = removeExpiredNotifications(notifications)
            Log.d(TAG, "🗑️ Dopo rimozione scadute: ${afterExpiredCleanup.size}")

            // 2. Rimuovi notifiche troppo vecchie
            val afterAgeCleanup = removeOldNotifications(afterExpiredCleanup)
            Log.d(TAG, "📅 Dopo rimozione vecchie: ${afterAgeCleanup.size}")

            // 3. Limita numero totale
            val afterCountLimit = limitTotalNotifications(afterAgeCleanup)
            Log.d(TAG, "📝 Dopo limite count: ${afterCountLimit.size}")

            // 4. Rimuovi duplicati
            val afterDeduplication = removeDuplicates(afterCountLimit)
            Log.d(TAG, "🔗 Dopo deduplicazione: ${afterDeduplication.size}")

            // 5. Ottimizza per performance
            val optimized = optimizeForPerformance(afterDeduplication)
            Log.d(TAG, "⚡ Dopo ottimizzazione: ${optimized.size}")

            // 6. Salva risultati se ci sono cambiamenti
            if (optimized.size != initialCount) {
                saveCleanedNotifications(optimized)
                Log.d(TAG, "✅ Cleanup completato: $initialCount → ${optimized.size}")
            } else {
                Log.d(TAG, "✅ Nessuna pulizia necessaria")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore durante cleanup: ${e.message}", e)
        }
    }

    /**
     * Rimuovi notifiche scadute
     */
    private fun removeExpiredNotifications(notifications: List<Notification>): List<Notification> {
        val now = System.currentTimeMillis()
        return notifications.filter { notification ->
            notification.expiryDate?.let { expiry ->
                now <= expiry
            } ?: true // Se non ha scadenza, mantieni
        }
    }

    /**
     * Rimuovi notifiche troppo vecchie
     */
    private fun removeOldNotifications(notifications: List<Notification>): List<Notification> {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(MAX_AGE_DAYS)

        return notifications.filter { notification ->
            // Mantieni se:
            // 1. È più recente del cutoff
            // 2. È URGENT e non letta (anche se vecchia)
            // 3. È SUBSCRIPTION_EXPIRED (importante)
            notification.timestamp > cutoffTime ||
                    (notification.priority == NotificationPriority.URGENT && !notification.isRead) ||
                    notification.type == NotificationType.SUBSCRIPTION_EXPIRED
        }
    }

    /**
     * Limita numero totale di notifiche
     */
    private fun limitTotalNotifications(notifications: List<Notification>): List<Notification> {
        if (notifications.size <= MAX_NOTIFICATIONS) {
            return notifications
        }

        // Ordina per priorità e timestamp
        val sorted = notifications.sortedWith(
            compareByDescending<Notification> { it.priority.level }
                .thenByDescending { it.timestamp }
        )

        return sorted.take(MAX_NOTIFICATIONS)
    }

    /**
     * Rimuovi duplicati dello stesso tipo
     */
    private fun removeDuplicates(notifications: List<Notification>): List<Notification> {
        val result = mutableListOf<Notification>()
        val typeCount = mutableMapOf<NotificationType, Int>()

        // Ordina per timestamp decrescente (più recenti prima)
        val sorted = notifications.sortedByDescending { it.timestamp }

        for (notification in sorted) {
            val currentCount = typeCount[notification.type] ?: 0

            if (currentCount < MAX_DUPLICATES_SAME_TYPE) {
                result.add(notification)
                typeCount[notification.type] = currentCount + 1
            } else {
                Log.d(TAG, "🔗 Rimossa duplicata: ${notification.type}")
            }
        }

        return result
    }

    /**
     * Ottimizza per performance
     */
    private fun optimizeForPerformance(notifications: List<Notification>): List<Notification> {
        // Raggruppa notifiche simili se troppe
        return if (notifications.size > 50) {
            // Mantieni solo le più importanti per performance
            notifications.sortedWith(
                compareByDescending<Notification> { it.priority.level }
                    .thenByDescending { !it.isRead }
                    .thenByDescending { it.timestamp }
            ).take(50)
        } else {
            notifications
        }
    }

    /**
     * Salva notifiche pulite
     */
    private suspend fun saveCleanedNotifications(notifications: List<Notification>) {
        try {
            // Implementazione specifica per salvare le notifiche pulite
            // Per ora, utilizziamo il repository esistente
            repository.clearAllNotifications()

            // Salva una per una (non ottimale, ma funziona)
            notifications.forEach { notification ->
                repository.saveNotification(notification)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore salva notifiche pulite: ${e.message}", e)
        }
    }

    /**
     * Cleanup mirato per tipo specifico
     */
    suspend fun cleanupByType(type: NotificationType) {
        try {
            Log.d(TAG, "🎯 Cleanup per tipo: $type")

            val notifications = repository.getAllNotifications().first()
            val filtered = notifications.filter { it.type != type }

            if (filtered.size != notifications.size) {
                saveCleanedNotifications(filtered)
                Log.d(TAG, "✅ Rimosse ${notifications.size - filtered.size} notifiche $type")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore cleanup by type: ${e.message}", e)
        }
    }

    /**
     * Cleanup notifiche lette vecchie
     */
    suspend fun cleanupReadNotifications(olderThanDays: Long = 7) {
        try {
            Log.d(TAG, "📖 Cleanup notifiche lette vecchie")

            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(olderThanDays)
            val notifications = repository.getAllNotifications().first()

            val filtered = notifications.filter { notification ->
                // Mantieni se non è letta O è più recente del cutoff
                !notification.isRead || notification.timestamp > cutoffTime
            }

            if (filtered.size != notifications.size) {
                saveCleanedNotifications(filtered)
                Log.d(TAG, "✅ Rimosse ${notifications.size - filtered.size} notifiche lette vecchie")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore cleanup read notifications: ${e.message}", e)
        }
    }

    /**
     * Statistiche sistema
     */
    suspend fun getCleanupStats(): CleanupStats {
        return try {
            val notifications = repository.getAllNotifications().first()
            val now = System.currentTimeMillis()

            CleanupStats(
                totalNotifications = notifications.size,
                readNotifications = notifications.count { it.isRead },
                unreadNotifications = notifications.count { !it.isRead },
                expiredNotifications = notifications.count { it.isExpired() },
                urgentNotifications = notifications.count { it.priority == NotificationPriority.URGENT },
                oldNotifications = notifications.count {
                    (now - it.timestamp) > TimeUnit.DAYS.toMillis(MAX_AGE_DAYS)
                },
                byType = notifications.groupingBy { it.type }.eachCount(),
                averageAge = if (notifications.isNotEmpty()) {
                    notifications.map { now - it.timestamp }.average().toLong()
                } else 0L,
                lastCleanup = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore stats: ${e.message}", e)
            CleanupStats()
        }
    }

    /**
     * Stop cleanup system
     */
    fun stopCleanup() {
        isRunning = false
        Log.d(TAG, "🛑 Sistema cleanup fermato")
    }
}

/**
 * Statistiche del sistema di cleanup
 */
data class CleanupStats(
    val totalNotifications: Int = 0,
    val readNotifications: Int = 0,
    val unreadNotifications: Int = 0,
    val expiredNotifications: Int = 0,
    val urgentNotifications: Int = 0,
    val oldNotifications: Int = 0,
    val byType: Map<NotificationType, Int> = emptyMap(),
    val averageAge: Long = 0L,
    val lastCleanup: Long = 0L
) {
    fun getHealthScore(): Int {
        // Calcola un punteggio di "salute" del sistema notifiche
        val score = when {
            totalNotifications == 0 -> 100
            totalNotifications > 100 -> 20
            expiredNotifications > totalNotifications / 2 -> 30
            oldNotifications > totalNotifications / 3 -> 50
            urgentNotifications > 5 -> 60
            else -> 85
        }
        return score.coerceIn(0, 100)
    }

    fun needsCleanup(): Boolean {
        return getHealthScore() < 70
    }
}