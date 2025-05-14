package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.*
import retrofit2.http.*

interface WorkoutApiService {
    /**
     * Recupera tutte le schede dell'utente
     */
    @GET("schede_standalone.php")
    suspend fun getWorkoutPlans(
        @Query("user_id") userId: Int
    ): WorkoutPlansResponse

    /**
     * Recupera gli esercizi di una scheda specifica
     */
    @GET("schede_standalone.php")
    suspend fun getWorkoutExercises(
        @Query("scheda_id") schedaId: Int
    ): WorkoutExercisesResponse

    /**
     * Crea una nuova scheda
     */
    @POST("create_scheda_standalone.php")
    suspend fun createWorkoutPlan(
        @Body request: CreateWorkoutPlanRequest
    ): WorkoutPlanResponse

    /**
     * Aggiorna una scheda esistente
     */
    @PUT("schede_standalone.php")
    suspend fun updateWorkoutPlan(
        @Body request: UpdateWorkoutPlanRequest
    ): WorkoutPlanResponse

    /**
     * Elimina una scheda
     */
    @HTTP(method = "DELETE", path = "schede_standalone.php", hasBody = true)
    suspend fun deleteWorkoutPlan(
        @Body data: Map<String, Int>
    ): WorkoutPlanResponse

    /**
     * Recupera tutti gli esercizi disponibili per la creazione/modifica schede
     */
    @GET("get_esercizi_standalone.php")
    suspend fun getAvailableExercises(
        @Query("user_id") userId: Int
    ): ExercisesResponse
}

// Risposte per gli esercizi disponibili
data class ExercisesResponse(
    val success: Boolean,
    val esercizi: List<ExerciseItem>
)

data class ExerciseItem(
    val id: Int,
    val nome: String,
    val descrizione: String?,
    val gruppo_muscolare: String?,
    val attrezzatura: String?,
    val immagine_url: String?,
    val is_isometric: Boolean = false,
    val serie_default: Int? = null,
    val ripetizioni_default: Int? = null,
    val peso_default: Double? = null
)