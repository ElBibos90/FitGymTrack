package com.fitgymtrack.app.models

/**
 * Classe generica per le risposte API
 *
 * @param T tipo di dati contenuti nella risposta
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)