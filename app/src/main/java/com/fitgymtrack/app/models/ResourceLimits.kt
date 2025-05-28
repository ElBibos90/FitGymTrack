package com.fitgymtrack.app.models

/**
 * Modello per i limiti delle risorse dell'abbonamento
 */
data class ResourceLimits(
    val success: Boolean,
    val current_count: Int,
    val max_allowed: Int?,
    val limit_reached: Boolean,
    val remaining: Int,
    val subscription_status: String? = null, // NUOVO: Status della subscription (active, expired, etc.)
    val days_remaining: Int? = null // NUOVO: Giorni rimanenti alla scadenza
)