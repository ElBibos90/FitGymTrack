package com.fitgymtrack.app.utils

import android.content.Context
import android.util.Log
import com.fitgymtrack.app.enums.NotificationPriority
import com.fitgymtrack.app.enums.NotificationSource
import com.fitgymtrack.app.enums.NotificationType
import com.fitgymtrack.app.models.AppUpdateInfo
import com.fitgymtrack.app.models.Notification
import com.fitgymtrack.app.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Manager centrale per la gestione delle notifiche
 * Sostituisce gradualmente i banner esistenti
 * MODIFICATO: Aggiunto controllo anti-duplicati
 */
class NotificationManager private constructor(
    private val context: Context,
    private val repository: NotificationRepository
) {

    private val TAG = "NotificationManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        @Volatile
        private var INSTANCE: NotificationManager? = null

        fun getInstance(context: Context): NotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationManager(
                    context.applicationContext,
                    NotificationRepository(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }

        // Convenience methods per accesso diretto
        fun create(context: Context, type: NotificationType, data: Map<String, Any> = emptyMap()) {
            getInstance(context).createNotification(type, data)
        }

        fun createSubscriptionExpiry(context: Context, daysRemaining: Int, planName: String = "Premium") {
            getInstance(context).createSubscriptionExpiryNotification(daysRemaining, planName)
        }

        fun createSubscriptionExpired(context: Context, planName: String = "Premium") {
            getInstance(context).createSubscriptionExpiredNotification(planName)
        }

        fun createLimitReached(context: Context, resourceType: String, maxAllowed: Int) {
            getInstance(context).createLimitReachedNotification(resourceType, maxAllowed)
        }

        fun createAppUpdate(context: Context, updateInfo: AppUpdateInfo) {
            getInstance(context).createAppUpdateNotification(updateInfo)
        }
    }

    // === CREAZIONE NOTIFICHE PRINCIPALI ===

    /**
     * Crea una notifica generica
     * MODIFICATO: Aggiunto controllo anti-duplicati
     */
    fun createNotification(type: NotificationType, data: Map<String, Any> = emptyMap()) {
        scope.launch {
            try {
                Log.d(TAG, "🔔 Richiesta creazione notifica: $type")

                // NUOVO: Controllo anti-duplicati
                if (isDuplicateNotification(type, data)) {
                    Log.d(TAG, "⚠️ Notifica duplicata ignorata: $type")
                    return@launch
                }

                val notification = when (type) {
                    NotificationType.SUBSCRIPTION_EXPIRY -> {
                        val days = data["daysRemaining"] as? Int ?: 7
                        val plan = data["planName"] as? String ?: "Premium"
                        buildSubscriptionExpiryNotification(days, plan)
                    }
                    NotificationType.SUBSCRIPTION_EXPIRED -> {
                        val plan = data["planName"] as? String ?: "Premium"
                        buildSubscriptionExpiredNotification(plan)
                    }
                    NotificationType.LIMIT_REACHED -> {
                        val resource = data["resourceType"] as? String ?: "workouts"
                        val max = data["maxAllowed"] as? Int ?: 3
                        buildLimitReachedNotification(resource, max)
                    }
                    NotificationType.APP_UPDATE -> {
                        val updateInfo = data["updateInfo"] as? AppUpdateInfo
                        updateInfo?.let { buildAppUpdateNotification(it) }
                    }
                    NotificationType.DIRECT_MESSAGE -> {
                        val title = data["title"] as? String ?: "Messaggio"
                        val message = data["message"] as? String ?: "Hai ricevuto un nuovo messaggio"
                        buildDirectMessageNotification(title, message, data)
                    }
                    NotificationType.WORKOUT_COMPLETED -> {
                        val workoutName = data["workoutName"] as? String ?: "Allenamento"
                        val duration = data["duration"] as? Long ?: 0L
                        val exerciseCount = data["exerciseCount"] as? Int ?: 0
                        buildWorkoutCompletedNotification(workoutName, duration, exerciseCount)
                    }
                    NotificationType.ACHIEVEMENT -> {
                        val title = data["title"] as? String ?: "Traguardo raggiunto!"
                        val description = data["description"] as? String ?: "Complimenti per il traguardo!"
                        buildAchievementNotification(title, description)
                    }
                    NotificationType.REMINDER -> {
                        val title = data["title"] as? String ?: "Promemoria"
                        val message = data["message"] as? String ?: "È ora di allenarti!"
                        buildReminderNotification(title, message, data)
                    }
                }

                notification?.let {
                    repository.saveNotification(it)
                    Log.d(TAG, "✅ Notifica creata: ${it.title}")
                } ?: Log.w(TAG, "❌ Impossibile creare notifica per tipo: $type")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore creazione notifica $type: ${e.message}", e)
            }
        }
    }

    // NUOVO: Controllo anti-duplicati
    /**
     * Verifica se esiste già una notifica identica recente
     * CORREZIONE: Controlla notifiche recenti (lette o non lette) per evitare spam
     */
    private suspend fun isDuplicateNotification(type: NotificationType, data: Map<String, Any>): Boolean {
        return try {
            // CORRETTO: Prende TUTTE le notifiche (non solo non lette)
            val allNotifications = repository.getAllNotifications().first()

            // Calcola finestre temporali per controllo duplicati
            val now = System.currentTimeMillis()
            val oneDayAgo = now - (24 * 60 * 60 * 1000) // 24 ore
            val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000) // 7 giorni

            // Trova notifiche dello stesso tipo nelle finestre temporali appropriate
            val recentSameTypeNotifications = allNotifications.filter { notification ->
                notification.type == type && when (type) {
                    // Per subscription: controlla ultimi 7 giorni
                    NotificationType.SUBSCRIPTION_EXPIRY,
                    NotificationType.SUBSCRIPTION_EXPIRED -> notification.timestamp > oneWeekAgo

                    // Per limit reached: controlla ultime 24 ore
                    NotificationType.LIMIT_REACHED -> notification.timestamp > oneDayAgo

                    // Per app update: controlla ultime 24 ore
                    NotificationType.APP_UPDATE -> notification.timestamp > oneDayAgo

                    // Altri tipi: solo se non letti (comportamento originale)
                    else -> !notification.isRead
                }
            }

            if (recentSameTypeNotifications.isEmpty()) {
                return false
            }

            // Controllo specifico per tipo
            when (type) {
                NotificationType.LIMIT_REACHED -> {
                    val resourceType = data["resourceType"] as? String ?: "workouts"
                    // Considera duplicato se stesso tipo + stesso resourceType nelle ultime 24h
                    recentSameTypeNotifications.any { notification ->
                        notification.actionData?.contains("resource:$resourceType") == true ||
                                notification.message.contains(getResourceDisplayName(resourceType))
                    }
                }

                NotificationType.SUBSCRIPTION_EXPIRY -> {
                    val planName = data["planName"] as? String ?: "Premium"
                    val daysRemaining = data["daysRemaining"] as? Int ?: 7
                    // CORREZIONE: Considera duplicato se stessi giorni rimanenti negli ultimi 7 giorni
                    recentSameTypeNotifications.any { notification ->
                        notification.message.contains(planName) &&
                                when (daysRemaining) {
                                    0 -> notification.message.contains("scade oggi")
                                    1 -> notification.message.contains("scade domani")
                                    else -> notification.message.contains("tra $daysRemaining giorni")
                                }
                    }
                }

                NotificationType.SUBSCRIPTION_EXPIRED -> {
                    val planName = data["planName"] as? String ?: "Premium"
                    // Considera duplicato se stesso piano scaduto negli ultimi 7 giorni
                    recentSameTypeNotifications.any { notification ->
                        notification.message.contains(planName)
                    }
                }

                NotificationType.APP_UPDATE -> {
                    val updateInfo = data["updateInfo"] as? AppUpdateInfo
                    updateInfo?.let { info ->
                        // Considera duplicato se stessa versione nelle ultime 24h
                        recentSameTypeNotifications.any { notification ->
                            notification.message.contains(info.newVersion)
                        }
                    } ?: false
                }

                // Per altri tipi, considera duplicato se stesso tipo non letto
                else -> {
                    recentSameTypeNotifications.any { !it.isRead }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore controllo duplicati: ${e.message}", e)
            false // In caso di errore, permetti la creazione
        }
    }

    /**
     * Helper per ottenere nome display risorsa
     */
    private fun getResourceDisplayName(resourceType: String): String {
        return when (resourceType) {
            "workouts", "max_workouts" -> "schede"
            "custom_exercises", "max_custom_exercises" -> "esercizi personalizzati"
            else -> resourceType
        }
    }

    /**
     * Crea notifica scadenza abbonamento
     * SOSTITUISCE: SubscriptionExpiryWarningBanner
     */
    fun createSubscriptionExpiryNotification(daysRemaining: Int, planName: String = "Premium") {
        createNotification(
            NotificationType.SUBSCRIPTION_EXPIRY,
            mapOf("daysRemaining" to daysRemaining, "planName" to planName)
        )
    }

    /**
     * Crea notifica abbonamento scaduto
     * SOSTITUISCE: SubscriptionExpiredBanner
     */
    fun createSubscriptionExpiredNotification(planName: String = "Premium") {
        createNotification(
            NotificationType.SUBSCRIPTION_EXPIRED,
            mapOf("planName" to planName)
        )
    }

    /**
     * Crea notifica limite raggiunto
     * SOSTITUISCE: SubscriptionLimitBanner
     */
    fun createLimitReachedNotification(resourceType: String, maxAllowed: Int) {
        createNotification(
            NotificationType.LIMIT_REACHED,
            mapOf("resourceType" to resourceType, "maxAllowed" to maxAllowed)
        )
    }

    /**
     * Crea notifica aggiornamento app
     */
    fun createAppUpdateNotification(updateInfo: AppUpdateInfo) {
        createNotification(
            NotificationType.APP_UPDATE,
            mapOf("updateInfo" to updateInfo)
        )
    }

    /**
     * Crea notifica workout completato
     */
    fun createWorkoutCompletedNotification(workoutName: String, duration: Long, exerciseCount: Int) {
        createNotification(
            NotificationType.WORKOUT_COMPLETED,
            mapOf(
                "workoutName" to workoutName,
                "duration" to duration,
                "exerciseCount" to exerciseCount
            )
        )
    }

    /**
     * Crea notifica achievement
     */
    fun createAchievementNotification(title: String, description: String) {
        createNotification(
            NotificationType.ACHIEVEMENT,
            mapOf(
                "title" to title,
                "description" to description
            )
        )
    }

    /**
     * Crea notifica promemoria
     */
    fun createReminderNotification(title: String, message: String, data: Map<String, Any> = emptyMap()) {
        createNotification(
            NotificationType.REMINDER,
            mapOf(
                "title" to title,
                "message" to message
            ) + data
        )
    }

    // === BUILDERS NOTIFICHE ===

    private fun buildSubscriptionExpiryNotification(daysRemaining: Int, planName: String): Notification {
        val (title, message) = when (daysRemaining) {
            0 -> Pair(
                "Il tuo abbonamento scade oggi!",
                "Il tuo abbonamento $planName scade oggi. Rinnova ora per non perdere le funzionalità Premium."
            )
            1 -> Pair(
                "Il tuo abbonamento scade domani",
                "Il tuo abbonamento $planName scade domani. Rinnova per continuare ad utilizzare tutte le funzionalità."
            )
            else -> Pair(
                "Abbonamento in scadenza",
                "Il tuo abbonamento $planName scade tra $daysRemaining giorni. Rinnova per continuare ad utilizzare tutte le funzionalità."
            )
        }

        return Notification(
            type = NotificationType.SUBSCRIPTION_EXPIRY,
            source = NotificationSource.LOCAL,
            title = title,
            message = message,
            priority = if (daysRemaining <= 1) NotificationPriority.HIGH else NotificationPriority.NORMAL,
            actionData = buildActionData("navigate", "subscription"),
            expiryDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)
        )
    }

    private fun buildSubscriptionExpiredNotification(planName: String): Notification {
        return Notification(
            type = NotificationType.SUBSCRIPTION_EXPIRED,
            source = NotificationSource.LOCAL,
            title = "Abbonamento scaduto",
            message = "Il tuo abbonamento $planName è scaduto. Sei stato automaticamente riportato al piano Free con funzionalità limitate.",
            priority = NotificationPriority.URGENT,
            actionData = buildActionData("navigate", "subscription", mapOf("highlight" to "premium")),
            expiryDate = null
        )
    }

    private fun buildLimitReachedNotification(resourceType: String, maxAllowed: Int): Notification {
        val (title, message, resourceName) = when (resourceType) {
            "max_workouts", "workouts" -> Triple(
                "Limite schede raggiunto",
                "Hai raggiunto il limite di $maxAllowed schede disponibili con il piano Free. Passa al piano Premium per avere schede illimitate.",
                "schede"
            )
            "max_custom_exercises", "custom_exercises" -> Triple(
                "Limite esercizi raggiunto",
                "Hai raggiunto il limite di $maxAllowed esercizi personalizzati disponibili con il piano Free. Passa al piano Premium per avere esercizi illimitati.",
                "esercizi personalizzati"
            )
            else -> Triple(
                "Limite piano raggiunto",
                "Hai raggiunto un limite del tuo piano corrente. Passa al piano Premium per sbloccare funzionalità illimitate.",
                "funzionalità"
            )
        }

        return Notification(
            type = NotificationType.LIMIT_REACHED,
            source = NotificationSource.LOCAL,
            title = title,
            message = message,
            priority = NotificationPriority.HIGH,
            actionData = buildActionData("navigate", "subscription", mapOf(
                "resource" to resourceType,
                "maxAllowed" to maxAllowed
            )),
            expiryDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(14)
        )
    }

    private fun buildAppUpdateNotification(updateInfo: AppUpdateInfo): Notification? {
        if (!updateInfo.hasUpdate()) {
            Log.d(TAG, "🤷 Nessun aggiornamento disponibile")
            return null
        }

        val priority = if (updateInfo.isCritical) NotificationPriority.HIGH else NotificationPriority.NORMAL
        val title = if (updateInfo.isCritical) {
            "Aggiornamento critico disponibile"
        } else {
            "Nuova versione disponibile"
        }

        val changelogPreview = updateInfo.getChangelogItems().take(2).joinToString("\n• ", "• ")
        val message = "La versione ${updateInfo.newVersion} è disponibile!\n\n$changelogPreview"

        return Notification(
            type = NotificationType.APP_UPDATE,
            source = NotificationSource.REMOTE,
            title = title,
            message = message,
            priority = priority,
            actionData = buildActionData("url", updateInfo.playStoreUrl, mapOf(
                "version" to updateInfo.newVersion,
                "critical" to updateInfo.isCritical
            )),
            expiryDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)
        )
    }

    private fun buildDirectMessageNotification(title: String, message: String, data: Map<String, Any>): Notification {
        val priority = when (data["priority"] as? String) {
            "URGENT" -> NotificationPriority.URGENT
            "HIGH" -> NotificationPriority.HIGH
            "LOW" -> NotificationPriority.LOW
            else -> NotificationPriority.NORMAL
        }

        val expiryDate = (data["expiryTimestamp"] as? Long)
        val imageUrl = data["imageUrl"] as? String
        val actionData = data["actionData"] as? String

        return Notification(
            type = NotificationType.DIRECT_MESSAGE,
            source = NotificationSource.REMOTE,
            title = title,
            message = message,
            priority = priority,
            actionData = actionData ?: buildActionData("dismiss"),
            expiryDate = expiryDate,
            imageUrl = imageUrl
        )
    }

    private fun buildWorkoutCompletedNotification(workoutName: String, duration: Long, exerciseCount: Int): Notification {
        val durationText = when {
            duration >= 60 -> "${duration}min"
            duration > 0 -> "${duration}min"
            else -> ""
        }

        val message = if (durationText.isNotEmpty()) {
            "Complimenti! Hai completato '$workoutName' in $durationText con $exerciseCount esercizi. Ottimo lavoro! 💪"
        } else {
            "Complimenti! Hai completato '$workoutName' con $exerciseCount esercizi. Ottimo lavoro! 💪"
        }

        return Notification(
            type = NotificationType.WORKOUT_COMPLETED,
            source = NotificationSource.LOCAL,
            title = "Allenamento completato! 🎉",
            message = message,
            priority = NotificationPriority.NORMAL,
            actionData = buildActionData("navigate", "workouts"),
            expiryDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)
        )
    }

    private fun buildAchievementNotification(title: String, description: String): Notification {
        return Notification(
            type = NotificationType.ACHIEVEMENT,
            source = NotificationSource.LOCAL,
            title = title,
            message = description,
            priority = NotificationPriority.HIGH,
            actionData = buildActionData("navigate", "stats"),
            expiryDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)
        )
    }

    private fun buildReminderNotification(title: String, message: String, data: Map<String, Any>): Notification {
        val priority = when (data["priority"] as? String) {
            "HIGH" -> NotificationPriority.HIGH
            "LOW" -> NotificationPriority.LOW
            else -> NotificationPriority.NORMAL
        }

        return Notification(
            type = NotificationType.REMINDER,
            source = NotificationSource.LOCAL,
            title = title,
            message = message,
            priority = priority,
            actionData = buildActionData("navigate", "workouts"),
            expiryDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
        )
    }

    // === UTILITY ===

    private fun buildActionData(action: String, data: String = "", extra: Map<String, Any> = emptyMap()): String {
        val actionMap = mutableMapOf<String, Any>(
            "action" to action,
            "data" to data
        )
        actionMap.putAll(extra)

        return try {
            actionMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore building action data: ${e.message}")
            "action:$action,data:$data"
        }
    }

    /**
     * Controlla periodicamente aggiornamenti e nuovi messaggi
     */
    fun startPeriodicChecks() {
        scope.launch {
            try {
                Log.d(TAG, "🔄 Avvio controlli periodici")

                repository.checkAppUpdates().fold(
                    onSuccess = { updateInfo ->
                        updateInfo?.let { createAppUpdateNotification(it) }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Errore controllo aggiornamenti: ${error.message}")
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore controlli periodici: ${e.message}", e)
            }
        }
    }

    /**
     * Pulisce notifiche vecchie
     */
    fun cleanup() {
        scope.launch {
            try {
                repository.cleanupOldNotifications()
                Log.d(TAG, "🧹 Cleanup completato")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore cleanup: ${e.message}", e)
            }
        }
    }

    /**
     * Per debugging - crea notifiche di test
     */
    fun createTestNotifications() {
        Log.d(TAG, "🧪 Creando notifiche di test...")

        // Test subscription
        createSubscriptionExpiryNotification(3, "Premium")
        createLimitReachedNotification("workouts", 3)

        // Test workout e achievement
        createWorkoutCompletedNotification("Test Workout", 45, 8)
        createAchievementNotification("Primo Traguardo!", "Hai completato il tuo primo allenamento! 🎉")

        // Test direct message
        createNotification(NotificationType.DIRECT_MESSAGE, mapOf(
            "title" to "Benvenuto!",
            "message" to "Benvenuto in FitGymTrack! Siamo felici che tu sia qui.",
            "priority" to "NORMAL"
        ))

        // Test reminder
        createReminderNotification(
            "È ora di allenarti!",
            "Non ti alleni da 3 giorni. Che ne dici di una sessione veloce? 💪"
        )

        // Mock AppUpdateInfo
        val mockUpdate = AppUpdateInfo(
            newVersion = "2.1.0",
            newVersionCode = 21,
            currentVersion = "2.0.0",
            currentVersionCode = 20,
            changelog = "• Nuove statistiche avanzate\n• Miglioramenti UI\n• Correzioni bug",
            isCritical = false,
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.fitgymtrack"
        )
        createAppUpdateNotification(mockUpdate)

        Log.d(TAG, "✅ Notifiche di test create")
    }
}