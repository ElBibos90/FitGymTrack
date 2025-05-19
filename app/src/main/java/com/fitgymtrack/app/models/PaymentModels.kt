package com.fitgymtrack.app.models

data class PaymentRequest(
    val amount: Double,
    val type: String, // "subscription" o "donation"
    val plan_id: Int? = null, // per abbonamenti
    val description: String? = null,
    val message: String? = null, // per donazioni
    val display_name: Boolean = true // per donazioni
)

data class PaymentResponse(
    val success: Boolean,
    val approval_url: String? = null,
    val order_id: String? = null,
    val paypal_order_id: String? = null,
    val message: String? = null
)