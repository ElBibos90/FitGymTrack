package com.fitgymtrack.app.models

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Rappresenta una sessione di allenamento attiva
 */
data class ActiveWorkout(
    val id: Int,
    @SerializedName("scheda_id")
    val schedaId: Int,
    @SerializedName("data_allenamento")
    val dataAllenamento: String,
    @SerializedName("durata_totale")
    val durataTotale: Int? = null,
    val note: String? = null,
    @SerializedName("user_id")
    val userId: Int,
    val esercizi: List<WorkoutExercise> = emptyList(),
    @SerializedName("session_id")
    val sessionId: String? = null
)

/**
 * Rappresenta una richiesta per iniziare un nuovo allenamento
 */
data class StartWorkoutRequest(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("scheda_id")
    val schedaId: Int,
    @SerializedName("session_id")
    val sessionId: String
)

/**
 * Rappresenta la risposta quando si inizia un nuovo allenamento
 */
data class StartWorkoutResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("allenamento_id")
    val allenamentoId: Int,
    @SerializedName("session_id")
    val sessionId: String? = null
)

/**
 * Rappresenta una serie completata durante l'allenamento
 */
data class CompletedSeries(
    val id: String,
    val serieNumber: Int,
    val peso: Float,
    val ripetizioni: Int,
    @SerializedName("tempo_recupero")
    val tempoRecupero: Int,
    val timestamp: String = Date().toString(),
    val note: String? = null
)

/**
 * Richiesta per salvare una serie completata
 */
data class SaveCompletedSeriesRequest(
    @SerializedName("allenamento_id")
    val allenamentoId: Int,
    val serie: List<SeriesData>,
    @SerializedName("request_id")
    val requestId: String
)

/**
 * Dati di una singola serie da salvare
 */
data class SeriesData(
    @SerializedName("scheda_esercizio_id")
    val schedaEsercizioId: Int,
    val peso: Float,
    val ripetizioni: Int,
    val completata: Int = 1,
    @SerializedName("tempo_recupero")
    val tempoRecupero: Int? = null,
    val note: String? = null,
    @SerializedName("serie_number")
    val serieNumber: Int? = null,
    @SerializedName("serie_id")
    val serieId: String? = null
)

/**
 * Risposta generica per le operazioni sulle serie
 */
data class SaveCompletedSeriesResponse(
    val success: Boolean,
    val message: String
)

/**
 * Risposta per ottenere le serie completate
 */
data class GetCompletedSeriesResponse(
    val success: Boolean,
    val serie: List<CompletedSeriesData>,
    val count: Int
)

/**
 * Dati di una serie completata ricevuta dal server
 */
data class CompletedSeriesData(
    val id: String,
    @SerializedName("scheda_esercizio_id")
    val schedaEsercizioId: Int,
    val peso: Float,
    val ripetizioni: Int,
    val completata: Int,
    @SerializedName("tempo_recupero")
    val tempoRecupero: Int? = null,
    val timestamp: String,
    val note: String? = null,
    @SerializedName("serie_number")
    val serieNumber: Int? = null,
    @SerializedName("esercizio_id")
    val esercizioId: Int? = null,
    @SerializedName("esercizio_nome")
    val esercizioNome: String? = null,
    @SerializedName("real_serie_number")
    val realSerieNumber: Int? = null
)

/**
 * Richiesta per completare un allenamento
 */
data class CompleteWorkoutRequest(
    @SerializedName("allenamento_id")
    val allenamentoId: Int,
    @SerializedName("durata_totale")
    val durataTotale: Int,
    val note: String? = null
)

/**
 * Risposta per il completamento di un allenamento
 */
data class CompleteWorkoutResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("allenamento_id")
    val allenamentoId: Int,
    @SerializedName("durata_totale")
    val durataTotale: Int
)

/**
 * Richiesta per eliminare un allenamento
 */
data class DeleteWorkoutRequest(
    @SerializedName("allenamento_id")
    val allenamentoId: Int
)

/**
 * Stato dell'allenamento attivo
 */
sealed class ActiveWorkoutState {
    object Idle : ActiveWorkoutState()
    object Loading : ActiveWorkoutState()
    data class Success(val workout: ActiveWorkout) : ActiveWorkoutState()
    data class Error(val message: String) : ActiveWorkoutState()
}

/**
 * Stato delle serie completate
 */
sealed class CompletedSeriesState {
    object Idle : CompletedSeriesState()
    object Loading : CompletedSeriesState()
    data class Success(val series: Map<Int, List<CompletedSeries>>) : CompletedSeriesState()
    data class Error(val message: String) : CompletedSeriesState()
}

/**
 * Stato del salvataggio di una serie
 */
sealed class SaveSeriesState {
    object Idle : SaveSeriesState()
    object Loading : SaveSeriesState()
    object Success : SaveSeriesState()
    data class Error(val message: String) : SaveSeriesState()
}

/**
 * Stato del completamento di un allenamento
 */
sealed class CompleteWorkoutState {
    object Idle : CompleteWorkoutState()
    object Loading : CompleteWorkoutState()
    object Success : CompleteWorkoutState()
    data class Error(val message: String) : CompleteWorkoutState()
}