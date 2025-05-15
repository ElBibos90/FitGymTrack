package com.fitgymtrack.app.repository

import android.util.Log
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for workout history operations
 */
class WorkoutHistoryRepository {
    private val apiService = ApiClient.workoutHistoryApiService

    /**
     * Get the workout history for a user
     */
    suspend fun getWorkoutHistory(userId: Int): Result<List<WorkoutHistory>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WorkoutHistoryRepo", "Fetching workout history for user ID: $userId")
                val response = apiService.getWorkoutHistory(userId)

                if (response.success) {
                    Log.d("WorkoutHistoryRepo", "Successfully retrieved ${response.count} workouts")
                    Result.success(response.allenamenti)
                } else {
                    Log.e("WorkoutHistoryRepo", "Failed to retrieve workout history")
                    Result.failure(Exception("Failed to retrieve workout history"))
                }
            } catch (e: Exception) {
                Log.e("WorkoutHistoryRepo", "Error getting workout history: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get the completed series for a specific workout
     */
    suspend fun getWorkoutSeriesDetail(workoutId: Int): Result<List<CompletedSeriesData>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WorkoutHistoryRepo", "Fetching series for workout ID: $workoutId")
                val response = apiService.getWorkoutSeriesDetail(workoutId)

                if (response.success) {
                    Log.d("WorkoutHistoryRepo", "Successfully retrieved ${response.count} series")
                    Result.success(response.serie)
                } else {
                    Log.e("WorkoutHistoryRepo", "Failed to retrieve workout series")
                    Result.failure(Exception("Failed to retrieve workout series"))
                }
            } catch (e: Exception) {
                Log.e("WorkoutHistoryRepo", "Error getting workout series: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Delete a specific series
     */
    suspend fun deleteCompletedSeries(seriesId: String): Result<SeriesOperationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WorkoutHistoryRepo", "Deleting series with ID: $seriesId")
                val request = DeleteSeriesRequest(seriesId)
                val response = apiService.deleteCompletedSeries(request)

                Log.d("WorkoutHistoryRepo", "Delete series result: ${response.success}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("WorkoutHistoryRepo", "Error deleting series: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update a specific series
     */
    suspend fun updateCompletedSeries(
        seriesId: String,
        weight: Float,
        reps: Int
    ): Result<SeriesOperationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WorkoutHistoryRepo", "Updating series with ID: $seriesId")
                val request = UpdateSeriesRequest(seriesId, weight, reps)
                val response = apiService.updateCompletedSeries(request)

                Log.d("WorkoutHistoryRepo", "Update series result: ${response.success}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("WorkoutHistoryRepo", "Error updating series: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Delete an entire workout
     */
    suspend fun deleteWorkout(workoutId: Int): Result<SeriesOperationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WorkoutHistoryRepo", "Deleting workout with ID: $workoutId")

                // Create a Map instead of using the DeleteWorkoutRequest class
                // This ensures the JSON is formatted exactly as {"allenamento_id": 123}
                val requestMap = mapOf("allenamento_id" to workoutId)

                Log.d("WorkoutHistoryRepo", "Request payload: $requestMap")

                val response = apiService.deleteWorkout(requestMap)

                Log.d("WorkoutHistoryRepo", "Delete workout result: ${response.success}, message: ${response.message}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("WorkoutHistoryRepo", "Error deleting workout: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}