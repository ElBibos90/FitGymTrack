package com.fitgymtrack.app.models

/**
 * Modello per i limiti di risorse
 */
data class ResourceLimits(
    val success: Boolean,
    val current_count: Int,
    val max_allowed: Int?,
    val limit_reached: Boolean,
    val remaining: Int?
)