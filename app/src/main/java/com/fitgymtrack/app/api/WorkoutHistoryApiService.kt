package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.*
import retrofit2.http.*

/**
 * API interface for workout history operations
 */
interface WorkoutHistoryApiService {
    /**
     * Get the workout history for a user
     */
    @GET("get_allenamenti_standalone.php")
    suspend fun getWorkoutHistory(
        @Query("user_id") userId: Int
    ): Map<String, Any>

    /**
     * Get the completed series for a specific workout
     */
    @GET("get_completed_series_standalone.php")
    suspend fun getWorkoutSeriesDetail(
        @Query("allenamento_id") allenamentoId: Int
    ): GetCompletedSeriesResponse

    /**
     * Delete a specific series
     */
    @HTTP(method = "DELETE", path = "delete_completed_series.php", hasBody = true)
    suspend fun deleteCompletedSeries(
        @Body request: DeleteSeriesRequest
    ): SeriesOperationResponse

    /**
     * Update a specific series
     */
    @HTTP(method = "PUT", path = "update_completed_series.php", hasBody = true)
    suspend fun updateCompletedSeries(
        @Body request: UpdateSeriesRequest
    ): SeriesOperationResponse

    /**
     * Delete an entire workout
     */
    @POST("delete_allenamento_standalone.php")
    suspend fun deleteWorkout(
        @Body request: Map<String, Int>
    ): SeriesOperationResponse
}