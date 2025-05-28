package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.ApiResponse
import com.fitgymtrack.app.models.ResourceLimits
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interfaccia per le API relative agli abbonamenti
 */
interface SubscriptionApiService {
    /**
     * Ottiene tutti i piani di abbonamento disponibili
     */
    @GET("android_subscription_api.php")
    suspend fun getAvailablePlans(
        @Query("action") action: String = "get_plans"
    ): ApiResponse<PlansResponse>

    /**
     * Ottiene l'abbonamento attuale dell'utente
     */
    @GET("android_subscription_api.php")
    suspend fun getCurrentSubscription(
        @Query("action") action: String = "current_subscription"
    ): ApiResponse<SubscriptionResponse>

    /**
     * NUOVO: Controlla e aggiorna le subscription scadute
     */
    @GET("android_subscription_api.php")
    suspend fun checkExpiredSubscriptions(
        @Query("action") action: String = "check_expired"
    ): ApiResponse<ExpiredCheckResponse>

    /**
     * Verifica i limiti per un tipo di risorsa
     */
    @GET("android_resource_limits_api.php")
    suspend fun checkResourceLimits(
        @Query("resource_type") resourceType: String
    ): ApiResponse<ResourceLimits>

    /**
     * Aggiorna il piano di abbonamento
     */
    @POST("android_update_plan_api.php")
    suspend fun updatePlan(
        @Body request: UpdatePlanRequest
    ): ApiResponse<UpdatePlanResponse>
}

/**
 * Modello per la risposta di piani
 */
data class PlansResponse(
    val plans: List<SubscriptionPlanResponse>
)

/**
 * Modello per piano di abbonamento (risposta API)
 */
data class SubscriptionPlanResponse(
    val id: Int,
    val name: String,
    val price: Double,
    val billing_cycle: String,
    val max_workouts: Int? = null,
    val max_custom_exercises: Int? = null,
    val advanced_stats: Int = 0,
    val cloud_backup: Int = 0,
    val no_ads: Int = 0
)

/**
 * Modello per la risposta di abbonamento
 */
data class SubscriptionResponse(
    val subscription: ApiSubscription
)

/**
 * Modello per l'abbonamento (risposta API)
 */
data class ApiSubscription(
    val id: Int? = null,
    val user_id: Int? = null,
    val plan_id: Int,
    val plan_name: String,
    val status: String = "active",
    val price: Double,
    val max_workouts: Int? = null,
    val max_custom_exercises: Int? = null,
    val current_count: Int = 0,
    val current_custom_exercises: Int = 0,
    val advanced_stats: Int = 0,
    val cloud_backup: Int = 0,
    val no_ads: Int = 0,
    val start_date: String? = null,
    val end_date: String? = null,
    val days_remaining: Int? = null, // NUOVO
    val computed_status: String? = null // NUOVO
)

/**
 * NUOVO: Modello per la risposta del controllo scadenze
 */
data class ExpiredCheckResponse(
    val success: Boolean,
    val message: String,
    val updated_count: Int
)

/**
 * Modello per la richiesta di aggiornamento piano
 */
data class UpdatePlanRequest(
    val plan_id: Int
)

/**
 * Modello per la risposta di aggiornamento piano
 */
data class UpdatePlanResponse(
    val success: Boolean,
    val message: String,
    val plan_name: String
)