package com.fitgymtrack.app.repository

import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {
    private val apiService = ApiClient.apiService

    suspend fun getUserProfile(): Result<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getUserProfile()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getCurrentSubscription(): Result<Subscription> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCurrentSubscription()

                // Estrai i dati dalla mappa
                @Suppress("UNCHECKED_CAST")
                val subscription = response["subscription"] as? Map<String, Any>

                if (subscription != null) {
                    val planName = subscription["plan_name"] as? String ?: "Free"
                    val price = (subscription["price"] as? Number)?.toDouble() ?: 0.0
                    val maxWorkouts = (subscription["max_workouts"] as? Number)?.toInt()
                    val currentCount = (subscription["current_count"] as? Number)?.toInt() ?: 0
                    val maxCustomExercises = (subscription["max_custom_exercises"] as? Number)?.toInt()
                    val currentCustomExercises = (subscription["current_custom_exercises"] as? Number)?.toInt() ?: 0

                    Result.success(Subscription(
                        planId = (subscription["plan_id"] as? Number)?.toInt() ?: 0,
                        planName = planName,
                        price = price,
                        maxWorkouts = maxWorkouts,
                        currentCount = currentCount,
                        maxCustomExercises = maxCustomExercises,
                        currentCustomExercises = currentCustomExercises
                    ))
                } else {
                    Result.failure(Exception("Dati abbonamento non disponibili"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}