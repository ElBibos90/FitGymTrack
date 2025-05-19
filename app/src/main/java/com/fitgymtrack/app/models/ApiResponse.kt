package com.fitgymtrack.app.models

/**
 * Modelo genérico de respuesta API para el app Android
 *
 * @param T tipo de datos que se espera en la respuesta
 * @property success indica si la solicitud fue exitosa
 * @property data datos devueltos por la API (puede ser null)
 * @property message mensaje de éxito o error
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)