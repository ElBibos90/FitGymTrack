package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.*
import retrofit2.http.*

/**
 * Interfaccia per le API relative agli allenamenti attivi
 */
interface ActiveWorkoutApiService {
    /**
     * Inizia un nuovo allenamento
     */
    @POST("start_active_workout_standalone.php")
    suspend fun startWorkout(
        @Body request: StartWorkoutRequest
    ): StartWorkoutResponse

    /**
     * Recupera gli esercizi di una scheda specifica
     */
    @GET("schede_standalone.php")
    suspend fun getWorkoutExercises(
        @Query("scheda_id") schedaId: Int
    ): WorkoutExercisesResponse

    /**
     * Recupera le serie completate per un allenamento
     */
    @GET("get_completed_series_standalone.php")
    suspend fun getCompletedSeries(
        @Query("allenamento_id") allenamentoId: Int
    ): GetCompletedSeriesResponse

    /**
     * Salva una serie completata
     */
    @POST("save_completed_series.php")
    suspend fun saveCompletedSeries(
        @Body request: SaveCompletedSeriesRequest
    ): SaveCompletedSeriesResponse

    /**
     * Completa un allenamento
     */
    @POST("complete_allenamento_standalone.php")
    suspend fun completeWorkout(
        @Body request: CompleteWorkoutRequest
    ): CompleteWorkoutResponse

    /**
     * Elimina un allenamento
     */
    @POST("delete_allenamento_standalone.php")
    suspend fun deleteWorkout(
        @Body request: DeleteWorkoutRequest
    ): SaveCompletedSeriesResponse
}