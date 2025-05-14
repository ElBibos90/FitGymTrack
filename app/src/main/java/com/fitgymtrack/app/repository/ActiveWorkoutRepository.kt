package com.fitgymtrack.app.repository

import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository per gli allenamenti attivi
 */
class ActiveWorkoutRepository {
    // Usiamo direttamente activeWorkoutApiService
    private val apiService = ApiClient.activeWorkoutApiService

    /**
     * Inizia un nuovo allenamento
     */
    suspend fun startWorkout(userId: Int, schedaId: Int): Result<StartWorkoutResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Genera un ID univoco per la sessione
                val sessionId = "session_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"

                val request = StartWorkoutRequest(
                    userId = userId,
                    schedaId = schedaId,
                    sessionId = sessionId
                )

                val response = apiService.startWorkout(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Recupera gli esercizi di una scheda
     */
    suspend fun getWorkoutExercises(schedaId: Int): Result<List<WorkoutExercise>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getWorkoutExercises(schedaId)
                if (response.success) {
                    Result.success(response.esercizi)
                } else {
                    Result.failure(Exception("Errore nel recupero degli esercizi"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Recupera le serie completate per un allenamento
     */
    suspend fun getCompletedSeries(allenamentoId: Int): Result<List<CompletedSeriesData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCompletedSeries(allenamentoId)
                if (response.success) {
                    Result.success(response.serie)
                } else {
                    Result.failure(Exception("Errore nel recupero delle serie completate"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Salva una serie completata
     */
    suspend fun saveCompletedSeries(
        allenamentoId: Int,
        serie: List<SeriesData>,
        requestId: String
    ): Result<SaveCompletedSeriesResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SaveCompletedSeriesRequest(
                    allenamentoId = allenamentoId,
                    serie = serie,
                    requestId = requestId
                )

                val response = apiService.saveCompletedSeries(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Completa un allenamento
     */
    suspend fun completeWorkout(
        allenamentoId: Int,
        durataTotale: Int,
        note: String? = null
    ): Result<CompleteWorkoutResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = CompleteWorkoutRequest(
                    allenamentoId = allenamentoId,
                    durataTotale = durataTotale,
                    note = note
                )

                val response = apiService.completeWorkout(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Elimina un allenamento
     */
    suspend fun deleteWorkout(allenamentoId: Int): Result<SaveCompletedSeriesResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = DeleteWorkoutRequest(allenamentoId = allenamentoId)
                val response = apiService.deleteWorkout(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}