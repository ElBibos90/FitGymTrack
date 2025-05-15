package com.fitgymtrack.app.models

import com.google.gson.annotations.SerializedName

/**
 * Modello per rappresentare un esercizio restituito dall'API esercizi.php
 */
data class Exercise(
    val id: Int,
    val nome: String,
    val descrizione: String,
    @SerializedName("immagine_url")
    val immagineUrl: String,
    @SerializedName("gruppo_muscolare")
    val gruppoMuscolare: String,
    val attrezzatura: String,
    @SerializedName("is_isometric")
    val isIsometric: Int = 0, // 0 = false, 1 = true
    @SerializedName("equipment_type_id")
    val equipmentTypeId: Int? = null,
    val status: String? = null
) {
    // Proprietà calcolata per facilitare l'uso
    val isIsometricBool: Boolean
        get() = isIsometric > 0
}