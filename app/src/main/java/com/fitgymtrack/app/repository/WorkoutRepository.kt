package com.fitgymtrack.app.repository

import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.api.ExerciseItem
import com.fitgymtrack.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkoutRepository {
    // Usiamo direttamente workoutApiService anziché apiService
    private val apiService = ApiClient.workoutApiService

    /**
     * Recupera tutte le schede dell'utente
     */
    suspend fun getWorkoutPlans(userId: Int): Result<List<WorkoutPlan>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getWorkoutPlans(userId)
                if (response.success) {
                    Result.success(response.schede)
                } else {
                    Result.failure(Exception("Errore nel caricamento delle schede"))
                }
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
                    Result.failure(Exception("Errore nel caricamento degli esercizi"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Crea una nuova scheda
     */
    suspend fun createWorkoutPlan(request: CreateWorkoutPlanRequest): Result<WorkoutPlanResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createWorkoutPlan(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Aggiorna una scheda esistente
     */
    suspend fun updateWorkoutPlan(request: UpdateWorkoutPlanRequest): Result<WorkoutPlanResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateWorkoutPlan(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Elimina una scheda - MODIFICATO per passare direttamente l'ID
     */
    suspend fun deleteWorkoutPlan(schedaId: Int): Result<WorkoutPlanResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Passa direttamente schedaId invece di usare una mappa
                val response = apiService.deleteWorkoutPlan(schedaId)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Recupera gli esercizi disponibili
     */
    suspend fun getAvailableExercises(userId: Int): Result<List<ExerciseItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAvailableExercises(userId)
                if (response.success) {
                    Result.success(response.esercizi)
                } else {
                    Result.failure(Exception("Errore nel caricamento degli esercizi disponibili"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}