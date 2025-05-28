package com.fitgymtrack.app.services

import android.content.Context
import android.util.Log
import com.fitgymtrack.app.enums.NotificationType
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.models.User
import com.fitgymtrack.app.utils.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Servizio per l'integrazione automatica delle notifiche
 * Sostituisce i banner esistenti con notifiche centralizzate
 */
class NotificationIntegrationService private constructor(
    private val context: Context
) {

    private val TAG = "NotificationIntegrationService"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notificationManager = NotificationManager.getInstance(context)

    companion object {
        @Volatile
        private var INSTANCE: NotificationIntegrationService? = null

        fun getInstance(context: Context): NotificationIntegrationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationIntegrationService(context.applicationContext)
                    .also { INSTANCE = it }
            }
        }
    }

    // === SUBSCRIPTION NOTIFICATIONS ===

    /**
     * Controlla e crea notifiche per subscription scadute/in scadenza
     * SOSTITUISCE: SubscriptionExpiredBanner + SubscriptionExpiryWarningBanner
     */
    fun checkSubscriptionStatus(subscription: Subscription?) {
        if (subscription == null) return

        scope.launch {
            try {
                Log.d(TAG, "🔍 Controllo stato subscription: ${subscription.planName}")

                // Solo per piani a pagamento
                if (subscription.price > 0.0 && subscription.end_date != null) {
                    val daysRemaining = calculateDaysRemaining(subscription.end_date)

                    when {
                        daysRemaining < 0 -> {
                            // Subscription SCADUTA
                            Log.d(TAG, "🚨 Subscription scaduta da ${Math.abs(daysRemaining)} giorni")
                            createSubscriptionExpiredNotification(subscription.planName)
                        }
                        daysRemaining == 0 -> {
                            // Scade OGGI
                            Log.d(TAG, "⚠️ Subscription scade oggi")
                            createSubscriptionExpiryNotification(0, subscription.planName)
                        }
                        daysRemaining in 1..7 -> {
                            // Scade tra 1-7 giorni
                            Log.d(TAG, "⚠️ Subscription scade tra $daysRemaining giorni")
                            createSubscriptionExpiryNotification(daysRemaining, subscription.planName)
                        }
                        else -> {
                            Log.d(TAG, "✅ Subscription attiva ($daysRemaining giorni rimanenti)")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore controllo subscription: ${e.message}", e)
            }
        }
    }

    /**
     * Crea notifica per subscription scaduta
     * SOSTITUISCE: SubscriptionExpiredBanner
     */
    private fun createSubscriptionExpiredNotification(planName: String) {
        notificationManager.createNotification(
            type = NotificationType.SUBSCRIPTION_EXPIRED,
            data = mapOf("planName" to planName)
        )
    }

    /**
     * Crea notifica per subscription in scadenza
     * SOSTITUISCE: SubscriptionExpiryWarningBanner
     */
    private fun createSubscriptionExpiryNotification(daysRemaining: Int, planName: String) {
        notificationManager.createNotification(
            type = NotificationType.SUBSCRIPTION_EXPIRY,
            data = mapOf(
                "daysRemaining" to daysRemaining,
                "planName" to planName
            )
        )
    }

    // === LIMIT NOTIFICATIONS ===

    /**
     * Controlla e crea notifiche per limiti raggiunti
     * SOSTITUISCE: SubscriptionLimitBanner
     */
    fun checkResourceLimits(
        resourceType: String,
        currentCount: Int,
        maxAllowed: Int?,
        isLimitReached: Boolean
    ) {
        scope.launch {
            try {
                Log.d(TAG, "🔍 Controllo limiti: $resourceType ($currentCount/${maxAllowed ?: "∞"})")

                if (isLimitReached && maxAllowed != null) {
                    Log.d(TAG, "🚨 Limite raggiunto per $resourceType")
                    createLimitReachedNotification(resourceType, maxAllowed)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore controllo limiti: ${e.message}", e)
            }
        }
    }

    /**
     * Crea notifica per limite raggiunto
     * SOSTITUISCE: SubscriptionLimitBanner
     */
    private fun createLimitReachedNotification(resourceType: String, maxAllowed: Int) {
        notificationManager.createNotification(
            type = NotificationType.LIMIT_REACHED,
            data = mapOf(
                "resourceType" to resourceType,
                "maxAllowed" to maxAllowed
            )
        )
    }

    // === WORKOUT NOTIFICATIONS ===

    /**
     * Crea notifica per allenamento completato
     */
    fun notifyWorkoutCompleted(
        workoutName: String,
        duration: Long,
        exerciseCount: Int
    ) {
        scope.launch {
            try {
                Log.d(TAG, "💪 Allenamento completato: $workoutName")

                notificationManager.createNotification(
                    type = NotificationType.WORKOUT_COMPLETED,
                    data = mapOf(
                        "workoutName" to workoutName,
                        "duration" to duration,
                        "exerciseCount" to exerciseCount
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore notifica workout: ${e.message}", e)
            }
        }
    }

    /**
     * Crea notifica per traguardo raggiunto
     */
    fun notifyAchievement(
        achievementTitle: String,
        achievementDescription: String
    ) {
        scope.launch {
            try {
                Log.d(TAG, "🏆 Traguardo raggiunto: $achievementTitle")

                notificationManager.createNotification(
                    type = NotificationType.ACHIEVEMENT,
                    data = mapOf(
                        "title" to achievementTitle,
                        "description" to achievementDescription
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore notifica achievement: ${e.message}", e)
            }
        }
    }

    // === APP LIFECYCLE NOTIFICATIONS ===

    /**
     * Controlla aggiornamenti app e crea notifica se disponibile
     */
    fun checkAppUpdates() {
        scope.launch {
            try {
                Log.d(TAG, "🔄 Controllo aggiornamenti app")
                notificationManager.startPeriodicChecks()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore controllo app updates: ${e.message}", e)
            }
        }
    }

    /**
     * Crea notifica di benvenuto per nuovi utenti
     */
    fun notifyWelcomeMessage(user: User) {
        scope.launch {
            try {
                Log.d(TAG, "👋 Benvenuto: ${user.username}")

                notificationManager.createNotification(
                    type = NotificationType.DIRECT_MESSAGE,
                    data = mapOf(
                        "title" to "Benvenuto in FitGymTrack!",
                        "message" to "Ciao ${user.username}! Siamo felici che tu sia qui. Inizia subito a creare la tua prima scheda di allenamento!",
                        "priority" to "NORMAL"
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore notifica benvenuto: ${e.message}", e)
            }
        }
    }

    // === CLEANUP & MAINTENANCE ===

    /**
     * Avvia pulizia automatica delle notifiche
     */
    fun startPeriodicCleanup() {
        scope.launch {
            try {
                Log.d(TAG, "🧹 Avvio pulizia periodica")
                notificationManager.cleanup()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore cleanup: ${e.message}", e)
            }
        }
    }

    /**
     * Inizializza il servizio con controlli periodici
     */
    fun initialize() {
        scope.launch {
            try {
                Log.d(TAG, "🚀 Inizializzazione NotificationIntegrationService")

                // Avvia controlli periodici
                checkAppUpdates()
                startPeriodicCleanup()

                Log.d(TAG, "✅ NotificationIntegrationService inizializzato")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore inizializzazione: ${e.message}", e)
            }
        }
    }

    // === UTILITY METHODS ===

    /**
     * Calcola giorni rimanenti da una data string
     */
    private fun calculateDaysRemaining(endDateString: String): Int {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val endDate = dateFormat.parse(endDateString)
            val currentDate = Date()

            if (endDate != null) {
                val diffInMillis = endDate.time - currentDate.time
                TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
            } else {
                Int.MAX_VALUE // Se non riesco a parsare, considero come "mai scade"
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore calcolo giorni rimanenti: ${e.message}")
            Int.MAX_VALUE
        }
    }

    /**
     * Verifica se una notifica di un certo tipo esiste già
     */
    private suspend fun notificationExists(type: NotificationType): Boolean {
        return try {
            // Implementazione per verificare se esiste già una notifica di questo tipo
            // Per ora ritorna false per permettere sempre nuove notifiche
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore verifica notifica esistente: ${e.message}")
            false
        }
    }

    /**
     * Logging dettagliato per debugging
     */
    fun logStatus() {
        scope.launch {
            try {
                Log.d(TAG, "📊 Status NotificationIntegrationService:")
                Log.d(TAG, "   - Servizio attivo: ✅")
                Log.d(TAG, "   - Context: ${context::class.simpleName}")
                Log.d(TAG, "   - NotificationManager: ${notificationManager::class.simpleName}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore log status: ${e.message}")
            }
        }
    }
}