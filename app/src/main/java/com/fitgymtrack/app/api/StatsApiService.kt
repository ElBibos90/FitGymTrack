package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.*
import com.google.gson.annotations.SerializedName
import retrofit2.http.*

/**
 * Interfaccia per le API delle statistiche utente
 */
interface StatsApiService {
    /**
     * Recupera le statistiche generali dell'utente
     */
    @GET("android_user_stats.php")
    suspend fun getUserStats(
        @Query("user_id") userId: Int
    ): UserStatsResponse

    /**
     * Recupera le statistiche per un periodo specifico
     */
    @GET("android_period_stats.php")
    suspend fun getPeriodStats(
        @Query("user_id") userId: Int,
        @Query("period") period: String, // "week", "month", "year"
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): ApiResponse<PeriodStats>

    /**
     * Confronta le statistiche tra periodi
     */
    @GET("android_stats_comparison.php")
    suspend fun getStatsComparison(
        @Query("user_id") userId: Int,
        @Query("period_type") periodType: String,
        @Query("current_period") currentPeriod: String,
        @Query("previous_period") previousPeriod: String
    ): ApiResponse<StatsComparison>

    /**
     * Recupera i record personali dell'utente
     */
    @GET("android_personal_records.php")
    suspend fun getPersonalRecords(
        @Query("user_id") userId: Int,
        @Query("exercise_id") exerciseId: Int? = null
    ): ApiResponse<List<PersonalRecord>>

    /**
     * Recupera gli achievement dell'utente
     */
    @GET("android_achievements.php")
    suspend fun getAchievements(
        @Query("user_id") userId: Int
    ): ApiResponse<List<Achievement>>

    /**
     * Recupera la frequenza degli allenamenti
     */
    @GET("android_workout_frequency.php")
    suspend fun getWorkoutFrequency(
        @Query("user_id") userId: Int,
        @Query("period") period: String = "month" // "week", "month", "year"
    ): ApiResponse<WorkoutFrequency>

    /**
     * Aggiorna un obiettivo/goal dell'utente
     */
    @POST("android_update_goal.php")
    suspend fun updateUserGoal(
        @Body request: UpdateGoalRequest
    ): ApiResponse<String>

    /**
     * Calcola le statistiche aggregate
     */
    @POST("android_calculate_stats.php")
    suspend fun calculateStats(
        @Body request: CalculateStatsRequest
    ): ApiResponse<UserStats>
}

/**
 * Richiesta per aggiornare un obiettivo
 */
data class UpdateGoalRequest(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("goal_type")
    val goalType: String, // "weekly_workouts", "monthly_hours", etc.
    @SerializedName("target_value")
    val targetValue: Int,
    @SerializedName("start_date")
    val startDate: String? = null,
    @SerializedName("end_date")
    val endDate: String? = null
)

/**
 * Richiesta per calcolare statistiche
 */
data class CalculateStatsRequest(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("recalculate_all")
    val recalculateAll: Boolean = false,
    @SerializedName("include_achievements")
    val includeAchievements: Boolean = true
)