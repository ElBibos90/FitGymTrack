package com.fitgymtrack.app.models

/**
 * Request model for PayPal payment initialization
 */
data class PaymentRequest(
    val user_id: Int,
    val plan_id: Int,
    val amount: Double,
    val type: String, // "subscription" or "donation"
    val message: String? = null, // Optional, for donations
    val display_name: Boolean = true // Optional, for donations
)

/**
 * Response model from PayPal payment initialization
 */
data class PaymentResponse(
    val success: Boolean,
    val approval_url: String? = null,
    val order_id: String? = null,
    val paypal_order_id: String? = null,
    val message: String? = null
)