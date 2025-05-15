package com.fitgymtrack.app.repository

import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository per gestire le operazioni relative agli esercizi
 */
class ExerciseRepository {
    private val apiService = ApiClient.exerciseApiService

    /**
     * Recupera tutti gli esercizi
     */
    suspend fun getExercises(): Result<List<Exercise>> {
        return withContext(Dispatchers.IO) {
            try {
                val exercises = apiService.getExercises()
                Result.success(exercises)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Recupera un singolo esercizio per ID
     */
    suspend fun getExercise(id: Int): Result<Exercise> {
        return withContext(Dispatchers.IO) {
            try {
                val exercise = apiService.getExercise(id)
                Result.success(exercise)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}