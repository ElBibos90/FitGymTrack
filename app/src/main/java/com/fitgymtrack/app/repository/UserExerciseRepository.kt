package com.fitgymtrack.app.repository

import android.util.Log
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository per la gestione degli esercizi personalizzati
 */
class UserExerciseRepository {
    private val apiService = ApiClient.userExerciseApiService

    /**
     * Recupera tutti gli esercizi creati dall'utente
     */
    suspend fun getUserExercises(userId: Int): Result<List<UserExercise>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("UserExerciseRepository", "Recupero esercizi per utente ID: $userId")
                val response = apiService.getUserExercises(userId)
                if (response.success) {
                    Log.d("UserExerciseRepository", "Recuperati ${response.exercises?.size ?: 0} esercizi")
                    Result.success(response.exercises ?: emptyList())
                } else {
                    Log.e("UserExerciseRepository", "Errore: ${response.message}")
                    Result.failure(Exception(response.message ?: "Errore nel recupero degli esercizi"))
                }
            } catch (e: Exception) {
                Log.e("UserExerciseRepository", "Eccezione: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Crea un nuovo esercizio personalizzato
     */
    suspend fun createUserExercise(request: CreateUserExerciseRequest): Result<UserExerciseResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("UserExerciseRepository", "Creazione esercizio: ${request.nome}")
                val response = apiService.createUserExercise(request)
                if (response.success) {
                    Log.d("UserExerciseRepository", "Esercizio creato con ID: ${response.exerciseId}")
                } else {
                    Log.e("UserExerciseRepository", "Errore creazione: ${response.message}")
                }
                Result.success(response)
            } catch (e: Exception) {
                Log.e("UserExerciseRepository", "Eccezione creazione: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Aggiorna un esercizio esistente
     */
    suspend fun updateUserExercise(request: UpdateUserExerciseRequest): Result<UserExerciseResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("UserExerciseRepository", "Aggiornamento esercizio ID: ${request.id}")
                val response = apiService.updateUserExercise(request)
                if (response.success) {
                    Log.d("UserExerciseRepository", "Esercizio aggiornato con successo")
                } else {
                    Log.e("UserExerciseRepository", "Errore aggiornamento: ${response.message}")
                }
                Result.success(response)
            } catch (e: Exception) {
                Log.e("UserExerciseRepository", "Eccezione aggiornamento: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Elimina un esercizio
     */
    suspend fun deleteUserExercise(exerciseId: Int, userId: Int): Result<UserExerciseResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("UserExerciseRepository", "Eliminazione esercizio ID: $exerciseId")
                val request = DeleteUserExerciseRequest(exerciseId, userId)
                val response = apiService.deleteUserExercise(request)
                if (response.success) {
                    Log.d("UserExerciseRepository", "Esercizio eliminato con successo")
                } else {
                    Log.e("UserExerciseRepository", "Errore eliminazione: ${response.message}")
                }
                Result.success(response)
            } catch (e: Exception) {
                Log.e("UserExerciseRepository", "Eccezione eliminazione: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}