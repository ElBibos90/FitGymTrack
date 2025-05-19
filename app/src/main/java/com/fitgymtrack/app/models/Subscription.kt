// File: app/src/main/java/com/fitgymtrack/app/models/Subscription.kt
package com.fitgymtrack.app.models

data class Subscription(
    val planId: Int,
    val planName: String,
    val price: Double,
    val maxWorkouts: Int? = null,
    val currentCount: Int = 0,
    val maxCustomExercises: Int? = null,
    val currentCustomExercises: Int = 0,
    val advancedStats: Boolean = false,
    val cloudBackup: Boolean = false,
    val noAds: Boolean = false
)