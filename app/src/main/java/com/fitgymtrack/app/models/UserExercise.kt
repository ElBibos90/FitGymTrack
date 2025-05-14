package com.fitgymtrack.app.models

import com.google.gson.annotations.SerializedName

/**
 * Rappresenta un esercizio personalizzato creato dall'utente
 */
data class UserExercise(
    val id: Int,
    val nome: String,
    @SerializedName("gruppo_muscolare")
    val gruppoMuscolare: String,
    val descrizione: String? = null,
    val attrezzatura: String? = null,
    @SerializedName("is_isometric")
    val isIsometricInt: Int = 0,
    @SerializedName("created_by_user_id")
    val createdByUserId: Int,
    val status: String = "pending_review",
    @SerializedName("immagine_url")
    val immagineUrl: String? = null
) {
    // Proprietà calcolata per convertire Int a Boolean
    val isIsometric: Boolean
        get() = isIsometricInt > 0
}

/**
 * Classe per la richiesta di creazione di un nuovo esercizio
 */
data class CreateUserExerciseRequest(
    val nome: String,
    @SerializedName("gruppo_muscolare")
    val gruppoMuscolare: String,
    val descrizione: String? = null,
    val attrezzatura: String? = null,
    @SerializedName("is_isometric")
    val isIsometric: Boolean = false,
    @SerializedName("created_by_user_id")
    val createdByUserId: Int,
    val status: String = "pending_review"
)

/**
 * Classe per la richiesta di aggiornamento di un esercizio esistente
 */
data class UpdateUserExerciseRequest(
    val id: Int,
    val nome: String,
    @SerializedName("gruppo_muscolare")
    val gruppoMuscolare: String,
    val descrizione: String? = null,
    val attrezzatura: String? = null,
    @SerializedName("is_isometric")
    val isIsometric: Boolean = false,
    @SerializedName("user_id")
    val userId: Int
)

/**
 * Risposta generica per le operazioni sugli esercizi
 */
data class UserExerciseResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("exercise_id")
    val exerciseId: Int? = null
)

/**
 * Risposta per l'elenco degli esercizi
 */
data class UserExercisesResponse(
    val success: Boolean,
    val exercises: List<UserExercise>? = null,
    val message: String? = null
)

/**
 * Richiesta per l'eliminazione di un esercizio
 */
data class DeleteUserExerciseRequest(
    @SerializedName("exercise_id")
    val exerciseId: Int,
    @SerializedName("user_id")
    val userId: Int
)