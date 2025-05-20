package com.fitgymtrack.app.models

/**
 * Modello per l'abbonamento
 */
data class Subscription(
    val id: Int? = null,
    val user_id: Int? = null,
    val plan_id: Int,
    val planName: String,  // nome adattato per compatibilità
    val status: String = "active",
    val price: Double,
    val maxWorkouts: Int? = null,  // nome adattato per compatibilità
    val maxCustomExercises: Int? = null,  // nome adattato per compatibilità
    val currentCount: Int = 0,  // nome adattato per compatibilità
    val currentCustomExercises: Int = 0,  // nome adattato per compatibilità
    val advancedStats: Boolean = false,  // nome adattato per compatibilità
    val cloudBackup: Boolean = false,  // nome adattato per compatibilità
    val noAds: Boolean = false,  // nome adattato per compatibilità
    val start_date: String? = null,
    val end_date: String? = null
)