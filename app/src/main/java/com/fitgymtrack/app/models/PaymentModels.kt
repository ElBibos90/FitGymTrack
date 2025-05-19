package com.fitgymtrack.app.models

/**
 * Modelo para la respuesta de la inicialización de pago
 */
data class PaymentResponse(
    val approval_url: String,
    val order_id: String,
    val paypal_order_id: String
)

/**
 * Modelo para el estado de un pago
 */
data class PaymentStatus(
    val order_id: String,
    val paypal_order_id: String,
    val amount: Double,
    val type: String,
    val status: String,
    val created_at: String,
    val plan_id: Int? = null,
    val plan_name: String? = null,
    val message: String? = null,
    val display_name: Boolean? = null,
    val subscription: Subscription? = null
)

/**
 * Modelo para la solicitud de pago
 */
data class PaymentRequest(
    val amount: Double,
    val type: String, // "subscription" o "donation"
    val plan_id: Int? = null, // para suscripciones
    val description: String? = null,
    val message: String? = null, // para donaciones
    val display_name: Boolean = true // para donaciones
)

/**
 * Modelo para los límites de los recursos
 */
data class ResourceLimits(
    val limit_reached: Boolean,
    val current_count: Int,
    val max_allowed: Int?,
    val remaining: Int
)

/**
 * Modelo para el plan de suscripción
 */
data class SubscriptionPlan(
    val id: Int,
    val name: String,
    val price: Double,
    val max_workouts: Int?,
    val max_custom_exercises: Int?,
    val advanced_stats: Boolean,
    val cloud_backup: Boolean,
    val no_ads: Boolean
)

// Nota: No definimos Subscription aquí, ya existe en Subscription.kt