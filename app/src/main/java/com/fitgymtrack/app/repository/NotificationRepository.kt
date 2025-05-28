package com.fitgymtrack.app.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fitgymtrack.app.BuildConfig
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.api.AppVersionCheckRequest
import com.fitgymtrack.app.api.NotificationApiService
import com.fitgymtrack.app.enums.NotificationPriority
import com.fitgymtrack.app.models.AppUpdateInfo
import com.fitgymtrack.app.models.AppVersionResponse
import com.fitgymtrack.app.models.Notification
import com.fitgymtrack.app.models.NotificationStats
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

// Extension per DataStore
private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notifications")

/**
 * Repository per gestione notifiche con DataStore + API
 */
class NotificationRepository(
    private val context: Context,
    private val apiService: NotificationApiService = ApiClient.notificationApiService,
    private val gson: Gson = Gson()
) {

    private val dataStore = context.notificationDataStore
    private val TAG = "NotificationRepository"

    // Keys per DataStore
    private companion object {
        val NOTIFICATIONS_KEY = stringPreferencesKey("notifications_json")
        val LAST_VERSION_CHECK_KEY = longPreferencesKey("last_version_check")
        val LAST_MESSAGES_CHECK_KEY = longPreferencesKey("last_messages_check")
        val LAST_CLEANUP_KEY = longPreferencesKey("last_cleanup")
        val DISMISSED_NOTIFICATIONS_KEY = stringSetPreferencesKey("dismissed_notifications")
    }

    // === NOTIFICHE LOCALI ===

    /**
     * Salva una notifica nel DataStore
     */
    suspend fun saveNotification(notification: Notification) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "💾 Salvando notifica: ${notification.type} - ${notification.title}")

                val currentNotifications = getAllNotifications().first()
                val updatedNotifications = currentNotifications.toMutableList().apply {
                    // Rimuovi eventuali notifiche duplicate dello stesso tipo
                    removeAll { it.type == notification.type && it.source == notification.source }
                    add(notification)
                }

                // Ordina per timestamp (più recenti prima)
                val sortedNotifications = updatedNotifications.sortedByDescending { it.timestamp }

                // Mantieni solo le ultime 100 notifiche per evitare overflow
                val limitedNotifications = sortedNotifications.take(100)

                saveNotificationsList(limitedNotifications)

                Log.d(TAG, "✅ Notifica salvata con successo. Totale: ${limitedNotifications.size}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore salvando notifica: ${e.message}", e)
            }
        }
    }

    /**
     * Recupera tutte le notifiche
     */
    fun getAllNotifications(): Flow<List<Notification>> {
        return dataStore.data.map { preferences ->
            try {
                val notificationsJson = preferences[NOTIFICATIONS_KEY] ?: "[]"
                val type = object : TypeToken<List<Notification>>() {}.type
                val notifications: List<Notification> = gson.fromJson(notificationsJson, type) ?: emptyList()

                // Filtra notifiche scadute
                val validNotifications = notifications.filterNot { it.isExpired() }

                Log.d(TAG, "📱 Caricate ${validNotifications.size} notifiche dal DataStore")
                validNotifications

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore caricando notifiche: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Recupera solo le notifiche non lette
     */
    fun getUnreadNotifications(): Flow<List<Notification>> {
        return getAllNotifications().map { notifications ->
            notifications.filter { !it.isRead }
        }
    }

    /**
     * Conta le notifiche non lette
     */
    fun getUnreadCount(): Flow<Int> {
        return getUnreadNotifications().map { it.size }
    }

    /**
     * Marca una notifica come letta
     */
    suspend fun markAsRead(notificationId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📖 Marcando come letta: $notificationId")

                val currentNotifications = getAllNotifications().first()
                val updatedNotifications = currentNotifications.map { notification ->
                    if (notification.id == notificationId) {
                        notification.copy(isRead = true)
                    } else {
                        notification
                    }
                }

                saveNotificationsList(updatedNotifications)
                Log.d(TAG, "✅ Notifica marcata come letta")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore marcando come letta: ${e.message}", e)
            }
        }
    }

    /**
     * Marca tutte le notifiche come lette
     */
    suspend fun markAllAsRead() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📖 Marcando tutte come lette")

                val currentNotifications = getAllNotifications().first()
                val updatedNotifications = currentNotifications.map { it.copy(isRead = true) }

                saveNotificationsList(updatedNotifications)
                Log.d(TAG, "✅ Tutte le notifiche marcate come lette")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore marcando tutte come lette: ${e.message}", e)
            }
        }
    }

    /**
     * Elimina una notifica
     */
    suspend fun deleteNotification(notificationId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🗑️ Eliminando notifica: $notificationId")

                val currentNotifications = getAllNotifications().first()
                val updatedNotifications = currentNotifications.filter { it.id != notificationId }

                saveNotificationsList(updatedNotifications)
                Log.d(TAG, "✅ Notifica eliminata")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore eliminando notifica: ${e.message}", e)
            }
        }
    }

    // === VERSION CHECK ===

    /**
     * Verifica aggiornamenti app (con cache di 24 ore)
     */
    suspend fun checkAppUpdates(forceCheck: Boolean = false): Result<AppUpdateInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                // Controlla cache
                if (!forceCheck && !shouldCheckVersion()) {
                    Log.d(TAG, "⏰ Version check in cache, skip")
                    return@withContext Result.success(null)
                }

                Log.d(TAG, "🔄 Controllo aggiornamenti app...")

                // Crea richiesta con BuildConfig
                val request = AppVersionCheckRequest(
                    current_version = BuildConfig.VERSION_NAME,
                    current_version_code = BuildConfig.VERSION_CODE,
                    platform = "android",
                    device_info = null
                )

                // Chiamata API (per ora mock)
                // val response = apiService.checkAppVersion(request)

                // Mock response per testing
                val mockResponse = AppVersionResponse(
                    success = true,
                    update_available = true,
                    latest_version = "2.1.0",
                    latest_version_code = BuildConfig.VERSION_CODE + 1,
                    is_critical = false,
                    changelog = "• Nuove statistiche avanzate\n• Miglioramenti UI\n• Correzioni bug",
                    play_store_url = "https://play.google.com/store/apps/details?id=com.fitgymtrack"
                )

                // Aggiorna cache
                updateVersionCheckTimestamp()

                if (mockResponse.update_available && mockResponse.latest_version_code > BuildConfig.VERSION_CODE) {
                    val updateInfo = AppUpdateInfo(
                        newVersion = mockResponse.latest_version,
                        newVersionCode = mockResponse.latest_version_code,
                        currentVersion = BuildConfig.VERSION_NAME,
                        currentVersionCode = BuildConfig.VERSION_CODE,
                        changelog = mockResponse.changelog,
                        isCritical = mockResponse.is_critical,
                        playStoreUrl = mockResponse.play_store_url
                    )

                    Log.d(TAG, "🆕 Aggiornamento disponibile: ${updateInfo.newVersion}")
                    Result.success(updateInfo)
                } else {
                    Log.d(TAG, "✅ App aggiornata")
                    Result.success(null)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore version check: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Verifica se è necessario controllare la versione
     */
    private suspend fun shouldCheckVersion(): Boolean {
        val lastCheck = dataStore.data.first()[LAST_VERSION_CHECK_KEY] ?: 0L
        val now = System.currentTimeMillis()
        val dayInMillis = TimeUnit.DAYS.toMillis(1)

        return (now - lastCheck) > dayInMillis
    }

    /**
     * Aggiorna timestamp ultimo version check
     */
    private suspend fun updateVersionCheckTimestamp() {
        dataStore.edit { preferences ->
            preferences[LAST_VERSION_CHECK_KEY] = System.currentTimeMillis()
        }
    }

    // === UTILITY METHODS ===

    /**
     * Salva lista notifiche nel DataStore
     */
    private suspend fun saveNotificationsList(notifications: List<Notification>) {
        dataStore.edit { preferences ->
            val notificationsJson = gson.toJson(notifications)
            preferences[NOTIFICATIONS_KEY] = notificationsJson
        }
    }

    /**
     * Pulisce notifiche vecchie e scadute
     */
    suspend fun cleanupOldNotifications() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🧹 Pulizia notifiche vecchie...")

                val currentNotifications = getAllNotifications().first()
                val now = System.currentTimeMillis()
                val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)

                val validNotifications = currentNotifications.filter { notification ->
                    // Mantieni se:
                    // 1. Non è scaduta
                    // 2. È più recente di 30 giorni
                    // 3. È URGENT e non letta (anche se vecchia)
                    !notification.isExpired() &&
                            (notification.timestamp > thirtyDaysAgo ||
                                    (notification.priority == NotificationPriority.URGENT && !notification.isRead))
                }

                if (validNotifications.size != currentNotifications.size) {
                    saveNotificationsList(validNotifications)
                    val removed = currentNotifications.size - validNotifications.size
                    Log.d(TAG, "🧹 Rimosse $removed notifiche vecchie")
                }

                // Aggiorna timestamp cleanup
                dataStore.edit { preferences ->
                    preferences[LAST_CLEANUP_KEY] = now
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore durante cleanup: ${e.message}", e)
            }
        }
    }

    /**
     * Ottieni statistiche per debugging
     */
    suspend fun getNotificationStats(): NotificationStats {
        return withContext(Dispatchers.IO) {
            try {
                val notifications = getAllNotifications().first()
                val lastCleanup = dataStore.data.first()[LAST_CLEANUP_KEY]

                NotificationStats(
                    totalNotifications = notifications.size,
                    unreadCount = notifications.count { !it.isRead },
                    byType = notifications.groupingBy { it.type }.eachCount(),
                    byPriority = notifications.groupingBy { it.priority }.eachCount(),
                    expiredCount = notifications.count { it.isExpired() },
                    lastCleanup = lastCleanup
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore stats: ${e.message}", e)
                NotificationStats(0, 0, emptyMap(), emptyMap(), 0, null)
            }
        }
    }

    /**
     * Reset completo (per testing)
     */
    suspend fun clearAllNotifications() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🗑️ RESET: Eliminando tutte le notifiche")
                dataStore.edit { preferences ->
                    preferences.clear()
                }
                Log.d(TAG, "✅ Tutte le notifiche eliminate")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore durante reset: ${e.message}", e)
            }
        }
    }
}