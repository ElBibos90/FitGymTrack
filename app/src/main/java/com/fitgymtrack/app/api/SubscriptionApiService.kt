package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.ApiResponse
import com.fitgymtrack.app.models.ResourceLimits
import com.fitgymtrack.app.models.SubscriptionPlan
import com.fitgymtrack.app.models.Subscription
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interfaz para las API relacionadas con suscripciones
 */
interface SubscriptionApiService {
    /**
     * Obtiene todos los planes de suscripción disponibles
     */
    @GET("android_subscription_api.php")
    suspend fun getAvailablePlans(
        @Query("action") action: String = "get_plans"
    ): ApiResponse<PlansResponse>

    /**
     * Obtiene la suscripción actual del usuario
     */
    @GET("android_subscription_api.php")
    suspend fun getCurrentSubscription(
        @Query("action") action: String = "current_subscription"
    ): ApiResponse<SubscriptionResponse>

    /**
     * Verifica los límites para un tipo de recurso
     */
    @GET("android_subscription_api.php")
    suspend fun checkResourceLimits(
        @Query("action") action: String = "check_limits",
        @Query("resource_type") resourceType: String
    ): ApiResponse<ResourceLimits>

    /**
     * Actualiza el plan de suscripción
     */
    @POST("android_subscription_api.php")
    suspend fun updatePlan(
        @Body request: UpdatePlanRequest,
        @Query("action") action: String = "update_plan"
    ): ApiResponse<SubscriptionResponse>
}

/**
 * Modelo para la respuesta de planes
 */
data class PlansResponse(
    val plans: List<SubscriptionPlan>
)

/**
 * Modelo para la respuesta de suscripción
 */
data class SubscriptionResponse(
    val subscription: Subscription
)

/**
 * Modelo para la solicitud de actualización de plan
 */
data class UpdatePlanRequest(
    val plan_id: Int
)