package com.fitgymtrack.app.models

/**
 * Rappresenta una scheda di allenamento
 */
data class WorkoutPlan(
    val id: Int,
    val nome: String,
    val descrizione: String?,
    val dataCreazione: String,
    val esercizi: List<WorkoutExercise> = emptyList()
)

/**
 * Rappresenta un esercizio all'interno di una scheda
 */
data class WorkoutExercise(
    val id: Int,
    val schedaEsercizioId: Int? = null,
    val nome: String,
    val gruppoMuscolare: String? = null,
    val attrezzatura: String? = null,
    val descrizione: String? = null,
    val serie: Int = 3,
    val ripetizioni: Int = 10,
    val peso: Double = 0.0,
    val ordine: Int = 0,
    val tempoRecupero: Int = 90,
    val note: String? = null,
    val setType: String = "normal",
    val linkedToPrevious: Boolean = false,
    val isIsometric: Boolean = false
)

/**
 * Richiesta per creare una nuova scheda
 */
data class CreateWorkoutPlanRequest(
    val user_id: Int,
    val nome: String,
    val descrizione: String?,
    val esercizi: List<WorkoutExerciseRequest>
)

/**
 * Rappresenta un esercizio nella richiesta di creazione/modifica scheda
 */
data class WorkoutExerciseRequest(
    val id: Int,
    val serie: Int,
    val ripetizioni: Int,
    val peso: Double,
    val ordine: Int,
    val tempo_recupero: Int = 90,
    val note: String? = null,
    val set_type: String = "normal",
    val linked_to_previous: Int = 0
)

/**
 * Richiesta per modificare una scheda esistente
 */
data class UpdateWorkoutPlanRequest(
    val scheda_id: Int,
    val nome: String,
    val descrizione: String?,
    val esercizi: List<WorkoutExerciseRequest>,
    val rimuovi: List<WorkoutExerciseToRemove>? = null
)

/**
 * Esercizio da rimuovere nella richiesta di modifica
 */
data class WorkoutExerciseToRemove(
    val id: Int
)

/**
 * Risposta generica per le operazioni sulle schede
 */
data class WorkoutPlanResponse(
    val success: Boolean,
    val message: String
)

/**
 * Risposta per la lista schede
 */
data class WorkoutPlansResponse(
    val success: Boolean,
    val schede: List<WorkoutPlan>
)

/**
 * Risposta per gli esercizi di una scheda
 */
data class WorkoutExercisesResponse(
    val success: Boolean,
    val esercizi: List<WorkoutExercise>
)