package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.ApiResponse
import com.fitgymtrack.app.models.AppVersionResponse
import com.fitgymtrack.app.models.MarkMessageReadRequest
import com.fitgymtrack.app.models.UserMessagesResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * API Service per gestione notifiche e version checking
 */
interface NotificationApiService {

    /**
     * Verifica se è disponibile un aggiornamento dell'app
     * Usa BuildConfig.VERSION_NAME e BuildConfig.VERSION_CODE
     */
    @POST("app_version_check.php")
    suspend fun checkAppVersion(
        @Body request: AppVersionCheckRequest
    ): AppVersionResponse

    /**
     * Recupera messaggi per l'utente corrente
     */
    @GET("user_messages.php")
    suspend fun getUserMessages(
        @Query("include_read") includeRead: Boolean = false,
        @Query("limit") limit: Int = 50
    ): UserMessagesResponse

    /**
     * Marca un messaggio come letto
     */
    @POST("user_messages.php")
    suspend fun markMessageAsRead(
        @Body request: MarkMessageReadRequest
    ): ApiResponse<String>

    /**
     * Marca tutti i messaggi come letti
     */
    @POST("user_messages.php")
    suspend fun markAllMessagesAsRead(): ApiResponse<String>

    /**
     * Elimina un messaggio (solo se permesso)
     */
    @DELETE("user_messages.php")
    suspend fun deleteMessage(
        @Query("message_id") messageId: String
    ): ApiResponse<String>

    // === ADMIN ENDPOINTS (per sviluppatore) ===

    /**
     * Invia un messaggio diretto (solo admin)
     */
    @POST("admin_messages.php")
    suspend fun sendDirectMessage(
        @Body request: SendMessageRequest
    ): ApiResponse<String>

    /**
     * Recupera statistiche messaggi (solo admin)
     */
    @GET("admin_messages.php")
    suspend fun getMessageStats(): ApiResponse<MessageStats>
}

/**
 * Richiesta per check versione app
 */
data class AppVersionCheckRequest(
    val current_version: String,           // BuildConfig.VERSION_NAME
    val current_version_code: Int,         // BuildConfig.VERSION_CODE
    val platform: String = "android",
    val device_info: DeviceInfo? = null
)

/**
 * Informazioni dispositivo per analytics
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val android_version: String,
    val app_language: String = "it"
)

/**
 * Richiesta per inviare messaggio diretto (admin)
 */
data class SendMessageRequest(
    val target_user_id: Int? = null,      // null = broadcast
    val title: String,
    val message: String,
    val is_markdown: Boolean = false,
    val priority: String = "NORMAL",       // LOW, NORMAL, HIGH, URGENT
    val expires_at: String? = null,        // YYYY-MM-DD HH:MM:SS
    val image_url: String? = null,
    val action_button: ActionButtonRequest? = null
)

/**
 * Bottone azione per messaggi
 */
data class ActionButtonRequest(
    val text: String,
    val action: String,                    // "url", "deeplink", "dismiss"
    val data: String
)

/**
 * Statistiche messaggi per admin
 */
data class MessageStats(
    val total_messages: Int,
    val total_users: Int,
    val messages_sent_today: Int,
    val messages_sent_this_week: Int,
    val read_rate: Double,                 // Percentuale lettura
    val recent_messages: List<RecentMessage>
)

/**
 * Messaggio recente per admin dashboard
 */
data class RecentMessage(
    val id: String,
    val title: String,
    val sent_at: String,
    val recipient_count: Int,
    val read_count: Int,
    val read_rate: Double
)

/**
 * Helper per creare richieste version check
 */
object VersionCheckHelper {

    /**
     * Crea richiesta version check usando BuildConfig
     */
    fun createVersionCheckRequest(): AppVersionCheckRequest {
        return AppVersionCheckRequest(
            current_version = android.os.Build.VERSION.RELEASE, // Placeholder - sarà sostituito con BuildConfig
            current_version_code = 1, // Placeholder - sarà sostituito con BuildConfig
            platform = "android",
            device_info = createDeviceInfo()
        )
    }

    /**
     * Crea informazioni dispositivo
     */
    private fun createDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = android.os.Build.MANUFACTURER,
            model = android.os.Build.MODEL,
            android_version = android.os.Build.VERSION.RELEASE,
            app_language = "it"
        )
    }

    /**
     * Verifica se un aggiornamento è disponibile
     */
    fun isUpdateAvailable(response: AppVersionResponse, currentVersionCode: Int): Boolean {
        return response.update_available && response.latest_version_code > currentVersionCode
    }

    /**
     * Determina se l'aggiornamento è critico
     */
    fun isCriticalUpdate(response: AppVersionResponse): Boolean {
        return response.is_critical
    }
}

/**
 * Costanti per l'API
 */
object NotificationApiConstants {
    const val VERSION_CHECK_ENDPOINT = "app_version_check.php"
    const val USER_MESSAGES_ENDPOINT = "user_messages.php"
    const val ADMIN_MESSAGES_ENDPOINT = "admin_messages.php"

    // Cache durations
    const val VERSION_CHECK_CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 ore
    const val MESSAGES_CACHE_DURATION = 5 * 60 * 1000L // 5 minuti

    // Limiti
    const val MAX_MESSAGES_PER_REQUEST = 100
    const val MAX_MESSAGE_LENGTH = 2000
    const val MAX_TITLE_LENGTH = 100
}