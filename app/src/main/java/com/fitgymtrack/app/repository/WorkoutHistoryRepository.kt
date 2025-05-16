package com.fitgymtrack.app.repository

import android.util.Log
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository per la gestione della cronologia degli allenamenti
 */
class WorkoutHistoryRepository {
    private val apiService = ApiClient.workoutHistoryApiService

    /**
     * Recupera la cronologia degli allenamenti di un utente
     */
    suspend fun getWorkoutHistory(userId: Int): Result<List<Map<String, Any>>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getWorkoutHistory(userId)

                // Estraiamo i dati dalla risposta Map<String, Any>
                val success = response["success"] as? Boolean ?: false
                val allenamenti = response["allenamenti"] as? List<Map<String, Any>> ?: emptyList()

                if (success) {
                    Result.success(allenamenti)
                } else {
                    Result.failure(Exception("Errore nel recupero della cronologia allenamenti"))
                }
            } catch (e: Exception) {
                Log.e("WorkoutHistoryRepository", "Errore getWorkoutHistory: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Recupera i dettagli delle serie completate per un allenamento specifico
     */
    suspend fun getWorkoutSeriesDetail(allenamentoId: Int): Result<List<CompletedSeriesData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getWorkoutSeriesDetail(allenamentoId)

                if (response.success) {
                    Result.success(response.serie)
                } else {
                    Result.failure(Exception("Errore nel recupero delle serie completate"))
                }
            } catch (e: Exception) {
                Log.e("WorkoutHistoryRepository", "Errore getWorkoutSeriesDetail: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Elimina una serie completata
     */
    suspend fun deleteCompletedSeries(seriesId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val request = DeleteSeriesRequest(seriesId)
                val response = apiService.deleteCompletedSeries(request)

                Result.success(response.success)
            } catch (e: Exception) {
                Log.e("WorkoutHistoryRepository", "Errore deleteCompletedSeries: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Aggiorna una serie completata
     */
    suspend fun updateCompletedSeries(
        seriesId: String,
        weight: Float,
        reps: Int,
        recoveryTime: Int? = null,
        notes: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UpdateSeriesRequest(seriesId, weight, reps, recoveryTime, notes)
                val response = apiService.updateCompletedSeries(request)

                Result.success(response.success)
            } catch (e: Exception) {
                Log.e("WorkoutHistoryRepository", "Errore updateCompletedSeries: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Elimina un intero allenamento
     */
    suspend fun deleteWorkout(workoutId: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteWorkout(mapOf("allenamento_id" to workoutId))

                Result.success(response.success)
            } catch (e: Exception) {
                Log.e("WorkoutHistoryRepository", "Errore deleteWorkout: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}