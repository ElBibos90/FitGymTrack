package com.fitgymtrack.app.models

import com.google.gson.annotations.SerializedName

/**
 * Modelli per le richieste di eliminazione e aggiornamento delle serie
 */

/**
 * Richiesta per eliminare una serie completata
 */
data class DeleteSeriesRequest(
    @SerializedName("serie_id")
    val serieId: String
)

/**
 * Richiesta per aggiornare una serie completata
 */
data class UpdateSeriesRequest(
    @SerializedName("serie_id")
    val serieId: String,
    val peso: Float,
    val ripetizioni: Int,
    @SerializedName("tempo_recupero")
    val tempoRecupero: Int? = null,
    val note: String? = null
)

/**
 * Risposta generica per le operazioni sulle serie
 */
data class SeriesOperationResponse(
    val success: Boolean,
    val message: String
)