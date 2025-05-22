package com.fitgymtrack.app.models

import com.google.gson.annotations.SerializedName

/**
 * Modello per le statistiche dell'utente
 */
data class UserStats(
    // Statistiche di base
    @SerializedName("total_workouts")
    val totalWorkouts: Int = 0,

    @SerializedName("total_hours")
    val totalHours: Int = 0,

    @SerializedName("current_streak")
    val currentStreak: Int = 0,

    @SerializedName("longest_streak")
    val longestStreak: Int = 0,

    @SerializedName("weekly_average")
    val weeklyAverage: Double = 0.0,

    @SerializedName("monthly_average")
    val monthlyAverage: Double = 0.0,

    // Esercizi
    @SerializedName("favorite_exercise")
    val favoriteExercise: String? = null,

    @SerializedName("total_exercises_performed")
    val totalExercisesPerformed: Int = 0,

    @SerializedName("total_sets_completed")
    val totalSetsCompleted: Int = 0,

    @SerializedName("total_reps_completed")
    val totalRepsCompleted: Int = 0,

    // Progressi peso
    @SerializedName("weight_progress")
    val weightProgress: Float? = null, // Percentuale di miglioramento

    @SerializedName("heaviest_lift")
    val heaviestLift: WeightRecord? = null,

    // Statistiche temporali
    @SerializedName("average_workout_duration")
    val averageWorkoutDuration: Int = 0, // in minuti

    @SerializedName("best_workout_time")
    val bestWorkoutTime: String? = null, // "morning", "afternoon", "evening"

    @SerializedName("most_active_day")
    val mostActiveDay: String? = null, // "monday", "tuesday", etc.

    // Obiettivi e achievement
    @SerializedName("goals_achieved")
    val goalsAchieved: Int = 0,

    @SerializedName("personal_records")
    val personalRecords: List<PersonalRecord> = emptyList(),

    // Statistiche recenti (ultimi 30 giorni)
    @SerializedName("recent_workouts")
    val recentWorkouts: Int = 0,

    @SerializedName("recent_improvements")
    val recentImprovements: Int = 0,

    // Date importanti
    @SerializedName("first_workout_date")
    val firstWorkoutDate: String? = null,

    @SerializedName("last_workout_date")
    val lastWorkoutDate: String? = null,

    // Consistenza
    @SerializedName("consistency_score")
    val consistencyScore: Float = 0.0f, // 0-100

    @SerializedName("workout_frequency")
    val workoutFrequency: WorkoutFrequency? = null
)

/**
 * Record di peso per un esercizio specifico
 */
data class WeightRecord(
    @SerializedName("exercise_name")
    val exerciseName: String,

    @SerializedName("weight")
    val weight: Double,

    @SerializedName("reps")
    val reps: Int,

    @SerializedName("date")
    val date: String
)

/**
 * Record personale
 */
data class PersonalRecord(
    @SerializedName("exercise_name")
    val exerciseName: String,

    @SerializedName("type")
    val type: String, // "max_weight", "max_reps", "max_volume"

    @SerializedName("value")
    val value: Double,

    @SerializedName("date_achieved")
    val dateAchieved: String,

    @SerializedName("previous_record")
    val previousRecord: Double? = null
)

/**
 * Frequenza degli allenamenti
 */
data class WorkoutFrequency(
    @SerializedName("weekly_days")
    val weeklyDays: Map<String, Int> = emptyMap(), // "monday" -> count

    @SerializedName("monthly_weeks")
    val monthlyWeeks: Map<String, Int> = emptyMap(), // "week1" -> count

    @SerializedName("hourly_distribution")
    val hourlyDistribution: Map<Int, Int> = emptyMap() // hour -> count
)

/**
 * Risposta API per le statistiche
 */
data class UserStatsResponse(
    val success: Boolean,
    val stats: UserStats?,
    val message: String? = null
)

/**
 * Statistiche aggregate per periodi specifici
 */
data class PeriodStats(
    @SerializedName("period_type")
    val periodType: String, // "week", "month", "year"

    @SerializedName("period_label")
    val periodLabel: String, // "Questa settimana", "Questo mese", etc.

    @SerializedName("workouts_count")
    val workoutsCount: Int,

    @SerializedName("total_duration")
    val totalDuration: Int, // in minuti

    @SerializedName("average_duration")
    val averageDuration: Int, // in minuti

    @SerializedName("most_trained_muscle_group")
    val mostTrainedMuscleGroup: String? = null,

    @SerializedName("improvement_percentage")
    val improvementPercentage: Float? = null,

    @SerializedName("new_records")
    val newRecords: Int = 0
)

/**
 * Confronto tra periodi
 */
data class StatsComparison(
    @SerializedName("current_period")
    val currentPeriod: PeriodStats,

    @SerializedName("previous_period")
    val previousPeriod: PeriodStats,

    @SerializedName("improvement_percentage")
    val improvementPercentage: Float,

    @SerializedName("trend")
    val trend: String // "improving", "declining", "stable"
)

/**
 * Achievement/Obiettivo
 */
data class Achievement(
    val id: Int,
    val name: String,
    val description: String,
    @SerializedName("icon_name")
    val iconName: String,
    @SerializedName("is_unlocked")
    val isUnlocked: Boolean,
    @SerializedName("unlock_date")
    val unlockDate: String? = null,
    @SerializedName("progress_current")
    val progressCurrent: Int = 0,
    @SerializedName("progress_target")
    val progressTarget: Int = 100
)