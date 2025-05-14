package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.*
import retrofit2.http.*

/**
 * Interfaccia per le API relative agli esercizi personalizzati
 */
interface UserExerciseApiService {
    /**
     * Recupera tutti gli esercizi creati dall'utente
     */
    @GET("user_exercises_standalone.php")
    suspend fun getUserExercises(
        @Query("user_id") userId: Int
    ): UserExercisesResponse

    /**
     * Crea un nuovo esercizio personalizzato
     */
    @POST("custom_exercise_standalone.php")
    suspend fun createUserExercise(
        @Body request: CreateUserExerciseRequest
    ): UserExerciseResponse

    /**
     * Aggiorna un esercizio esistente
     */
    @PUT("custom_exercise_standalone.php")
    suspend fun updateUserExercise(
        @Body request: UpdateUserExerciseRequest
    ): UserExerciseResponse

    /**
     * Elimina un esercizio
     */
    @HTTP(method = "DELETE", path = "user_exercises_standalone.php", hasBody = true)
    suspend fun deleteUserExercise(
        @Body request: DeleteUserExerciseRequest
    ): UserExerciseResponse
}