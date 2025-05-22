package com.fitgymtrack.app.repository

import android.util.Log
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.api.UpdateGoalRequest
import com.fitgymtrack.app.api.CalculateStatsRequest
import com.fitgymtrack.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository per la gestione delle statistiche utente
 */
class StatsRepository {
    private val apiService = ApiClient.statsApiService
    private val TAG = "StatsRepository"

    /**
     * Recupera le statistiche dell'utente
     */
    suspend fun getUserStats(userId: Int): Result<UserStats> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Recupero statistiche per utente: $userId")
                val response = apiService.getUserStats(userId)

                if (response.success && response.stats != null) {
                    Log.d(TAG, "Statistiche recuperate con successo")
                    Result.success(response.stats)
                } else {
                    val error = response.message ?: "Errore nel recupero delle statistiche"
                    Log.e(TAG, "Errore API: $error")
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione nel recupero statistiche: ${e.message}", e)
                // In caso di errore, restituiamo statistiche vuote come fallback
                Result.success(createEmptyStats())
            }
        }
    }

    /**
     * Recupera le statistiche per un periodo specifico
     */
    suspend fun getPeriodStats(
        userId: Int,
        period: String,
        startDate: String? = null,
        endDate: String? = null
    ): Result<PeriodStats> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Recupero statistiche periodo $period per utente: $userId")
                val response = apiService.getPeriodStats(userId, period, startDate, endDate)

                if (response.success && response.data != null) {
                    Log.d(TAG, "Statistiche periodo recuperate con successo")
                    Result.success(response.data)
                } else {
                    val error = response.message ?: "Errore nel recupero delle statistiche del periodo"
                    Log.e(TAG, "Errore API: $error")
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione nel recupero statistiche periodo: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Recupera i record personali
     */
    suspend fun getPersonalRecords(userId: Int, exerciseId: Int? = null): Result<List<PersonalRecord>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Recupero record personali per utente: $userId")
                val response = apiService.getPersonalRecords(userId, exerciseId)

                if (response.success && response.data != null) {
                    Log.d(TAG, "Record personali recuperati: ${response.data.size}")
                    Result.success(response.data)
                } else {
                    val error = response.message ?: "Errore nel recupero dei record personali"
                    Log.e(TAG, "Errore API: $error")
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione nel recupero record personali: ${e.message}", e)
                Result.success(emptyList()) // Fallback a lista vuota
            }
        }
    }

    /**
     * Recupera gli achievement dell'utente
     */
    suspend fun getAchievements(userId: Int): Result<List<Achievement>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Recupero achievement per utente: $userId")
                val response = apiService.getAchievements(userId)

                if (response.success && response.data != null) {
                    Log.d(TAG, "Achievement recuperati: ${response.data.size}")
                    Result.success(response.data)
                } else {
                    val error = response.message ?: "Errore nel recupero degli achievement"
                    Log.e(TAG, "Errore API: $error")
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione nel recupero achievement: ${e.message}", e)
                Result.success(emptyList()) // Fallback a lista vuota
            }
        }
    }

    /**
     * Recupera la frequenza degli allenamenti
     */
    suspend fun getWorkoutFrequency(userId: Int, period: String = "month"): Result<WorkoutFrequency> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Recupero frequenza allenamenti per utente: $userId, periodo: $period")
                val response = apiService.getWorkoutFrequency(userId, period)

                if (response.success && response.data != null) {
                    Log.d(TAG, "Frequenza allenamenti recuperata con successo")
                    Result.success(response.data)
                } else {
                    val error = response.message ?: "Errore nel recupero della frequenza"
                    Log.e(TAG, "Errore API: $error")
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione nel recupero frequenza: ${e.message}", e)
                Result.success(WorkoutFrequency()) // Fallback a frequenza vuota
            }
        }
    }

    /**
     * Confronta statistiche tra periodi
     */
    suspend fun getStatsComparison(
        userId: Int,
        periodType: String,
        currentPeriod: String,
        previousPeriod: String
    ): Result<StatsComparison> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Confronto statistiche per utente: $userId")
                val response = apiService.getStatsComparison(userId, periodType, currentPeriod, previousPeriod)

                if (response.success && response.data != null) {
                    Log.d(TAG, "Confronto statistiche recuperato con successo")
                    Result.success(response.data)
                } else {
                    val error = response.message ?: "Errore nel confronto delle statistiche"
                    Log.e(TAG, "Errore API: $error")
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione nel confronto statistiche: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Aggiorna un obiettivo dell'utente
     */
    suspend fun updateUserGoal(
        userId: Int,
        goalType: String,
        targetValue: Int,
        startDate: String? = null,
        endDate: String? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Aggiornamento obiettivo per utente: $userId, tipo: $goalType")
                val request = UpdateGoalRequest(userId, goalType, targetValue, startDate, endDate)
                val response = apiService.updateUserGoal(request)

                if (response.success && response.data != null) {
                    Log.d(TAG, "Obiettivo aggiornato con successo")
                    Result.success(response.data)
                } else {
                    val error = response.message ?: "Errore nell'aggiornamento dell'obiettivo"
                    Log.e(TAG, "Errore API: $error")
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione nell'aggiornamento obiettivo: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Ricalcola le statistiche dell'utente
     */
    suspend fun calculateStats(userId: Int, recalculateAll: Boolean = false): Result<UserStats> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Calcolo statistiche per utente: $userId")
                val request = CalculateStatsRequest(userId, recalculateAll, true)
                val response = apiService.calculateStats(request)

                if (response.success && response.data != null) {
                    Log.d(TAG, "Statistiche calcolate con successo")
                    Result.success(response.data)
                } else {
                    val error = response.message ?: "Errore nel calcolo delle statistiche"
                    Log.e(TAG, "Errore API: $error")
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione nel calcolo statistiche: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Crea statistiche vuote come fallback
     */
    private fun createEmptyStats(): UserStats {
        return UserStats(
            totalWorkouts = 0,
            totalHours = 0,
            currentStreak = 0,
            longestStreak = 0,
            weeklyAverage = 0.0,
            monthlyAverage = 0.0,
            favoriteExercise = null,
            totalExercisesPerformed = 0,
            totalSetsCompleted = 0,
            totalRepsCompleted = 0,
            weightProgress = null,
            heaviestLift = null,
            averageWorkoutDuration = 0,
            bestWorkoutTime = null,
            mostActiveDay = null,
            goalsAchieved = 0,
            personalRecords = emptyList(),
            recentWorkouts = 0,
            recentImprovements = 0,
            firstWorkoutDate = null,
            lastWorkoutDate = null,
            consistencyScore = 0.0f,
            workoutFrequency = null
        )
    }

    /**
     * Crea statistiche demo per testing (da rimuovere in produzione)
     */
    fun createDemoStats(): UserStats {
        return UserStats(
            totalWorkouts = 45,
            totalHours = 67,
            currentStreak = 7,
            longestStreak = 21,
            weeklyAverage = 3.2,
            monthlyAverage = 13.8,
            favoriteExercise = "Panca piana",
            totalExercisesPerformed = 234,
            totalSetsCompleted = 678,
            totalRepsCompleted = 8945,
            weightProgress = 15.5f,
            heaviestLift = WeightRecord(
                exerciseName = "Deadlift",
                weight = 120.0,
                reps = 5,
                date = "2024-05-15"
            ),
            averageWorkoutDuration = 89,
            bestWorkoutTime = "morning",
            mostActiveDay = "monday",
            goalsAchieved = 3,
            personalRecords = listOf(
                PersonalRecord(
                    exerciseName = "Squat",
                    type = "max_weight",
                    value = 100.0,
                    dateAchieved = "2024-05-10",
                    previousRecord = 95.0
                )
            ),
            recentWorkouts = 12,
            recentImprovements = 5,
            firstWorkoutDate = "2024-01-15",
            lastWorkoutDate = "2024-05-20",
            consistencyScore = 85.5f,
            workoutFrequency = WorkoutFrequency(
                weeklyDays = mapOf(
                    "monday" to 8,
                    "wednesday" to 7,
                    "friday" to 6,
                    "sunday" to 4
                ),
                monthlyWeeks = mapOf(
                    "week1" to 3,
                    "week2" to 4,
                    "week3" to 3,
                    "week4" to 2
                ),
                hourlyDistribution = mapOf(
                    8 to 5,
                    9 to 12,
                    18 to 8,
                    19 to 10,
                    20 to 7
                )
            )
        )
    }
}