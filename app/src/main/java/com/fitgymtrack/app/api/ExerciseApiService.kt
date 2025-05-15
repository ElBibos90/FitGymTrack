package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.Exercise
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interfaccia per l'API degli esercizi
 */
interface ExerciseApiService {
    @GET("esercizi.php")
    suspend fun getExercises(): List<Exercise>

    @GET("esercizi.php")
    suspend fun getExercise(@Query("id") id: Int): Exercise
}