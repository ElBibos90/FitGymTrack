package com.fitgymtrack.app.models

import com.google.gson.annotations.SerializedName

/**
 * Rappresenta una scheda di allenamento
 */
data class WorkoutPlan(
    val id: Int,
    val nome: String,
    // Resa nullable per essere più difensiva
    val descrizione: String?,
    // Aggiunta annotazione SerializedName per mappare correttamente dal JSON
    @SerializedName("data_creazione")
    val dataCreazione: String?,
    val esercizi: List<WorkoutExercise> = emptyList()
)

/**
 * Rappresenta un esercizio all'interno di una scheda
 *
 * IMPORTANTE: Se modifichi i parametri di questo data class, assicurati che tutti
 * i parametri non-nullable abbiano valori di default o siano gestiti correttamente
 * nel codice client.
 */
data class WorkoutExercise(
    val id: Int,
    @SerializedName("scheda_esercizio_id")
    val schedaEsercizioId: Int? = null,
    val nome: String,
    @SerializedName("gruppo_muscolare")
    val gruppoMuscolare: String? = null,
    val attrezzatura: String? = null,
    val descrizione: String? = null,
    val serie: Int = 3,
    val ripetizioni: Int = 10,
    val peso: Double = 0.0,
    val ordine: Int = 0,
    @SerializedName("tempo_recupero")
    val tempoRecupero: Int = 90,
    val note: String? = null,
    @SerializedName("set_type")
    val setType: String = "normal",  // Non può essere null
    // Cambiato da Boolean a Int per riflettere il formato del JSON
    @SerializedName("linked_to_previous")
    val linkedToPreviousInt: Int = 0,
    @SerializedName("is_isometric")
    val isIsometricInt: Int = 0
) {
    // Proprietà calcolate per mantenere la compatibilità con il resto del codice
    val linkedToPrevious: Boolean
        get() = linkedToPreviousInt > 0

    val isIsometric: Boolean
        get() = isIsometricInt > 0

    // Rimuovere il metodo copy() personalizzato qui
}

/**
 * Funzione di estensione per sostituire il metodo copy() personalizzato
 * La rinominiamo safeCopy per evitare ambiguità
 */
fun WorkoutExercise.safeCopy(
    id: Int = this.id,
    schedaEsercizioId: Int? = this.schedaEsercizioId,
    nome: String = this.nome,
    gruppoMuscolare: String? = this.gruppoMuscolare,
    attrezzatura: String? = this.attrezzatura,
    descrizione: String? = this.descrizione,
    serie: Int = this.serie,
    ripetizioni: Int = this.ripetizioni,
    peso: Double = this.peso,
    ordine: Int = this.ordine,
    tempoRecupero: Int = this.tempoRecupero,
    note: String? = this.note,
    setType: String = this.setType,
    linkedToPrevious: Boolean = this.linkedToPrevious,
    isIsometric: Boolean = this.isIsometric
): WorkoutExercise {
    return WorkoutExercise(
        id = id,
        schedaEsercizioId = schedaEsercizioId,
        nome = nome,
        gruppoMuscolare = gruppoMuscolare,
        attrezzatura = attrezzatura,
        descrizione = descrizione,
        serie = serie,
        ripetizioni = ripetizioni,
        peso = peso,
        ordine = ordine,
        tempoRecupero = tempoRecupero,
        note = note,
        setType = setType.ifEmpty { "normal" }, // Garantiamo che setType non sia mai vuoto
        linkedToPreviousInt = if (linkedToPrevious) 1 else 0, // Conversione Boolean -> Int
        isIsometricInt = if (isIsometric) 1 else 0           // Conversione Boolean -> Int
    )
}

/**
 * Factory function per creare un WorkoutExercise con parametri booleani
 */
fun createWorkoutExercise(
    id: Int,
    schedaEsercizioId: Int? = null,
    nome: String,
    gruppoMuscolare: String? = null,
    attrezzatura: String? = null,
    descrizione: String? = null,
    serie: Int = 3,
    ripetizioni: Int = 10,
    peso: Double = 0.0,
    ordine: Int = 0,
    tempoRecupero: Int = 90,
    note: String? = null,
    setType: String = "normal",
    linkedToPrevious: Boolean = false,
    isIsometric: Boolean = false
): WorkoutExercise {
    return WorkoutExercise(
        id = id,
        schedaEsercizioId = schedaEsercizioId,
        nome = nome,
        gruppoMuscolare = gruppoMuscolare,
        attrezzatura = attrezzatura,
        descrizione = descrizione,
        serie = serie,
        ripetizioni = ripetizioni,
        peso = peso,
        ordine = ordine,
        tempoRecupero = tempoRecupero,
        note = note,
        setType = setType.ifEmpty { "normal" },
        linkedToPreviousInt = if (linkedToPrevious) 1 else 0,
        isIsometricInt = if (isIsometric) 1 else 0
    )
}

/**
 * Richiesta per creare una nuova scheda
 */
data class CreateWorkoutPlanRequest(
    @SerializedName("user_id")
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
    @SerializedName("tempo_recupero")
    val tempo_recupero: Int = 90,
    val note: String? = null,
    @SerializedName("set_type")
    val set_type: String = "normal",
    @SerializedName("linked_to_previous")
    val linked_to_previous: Int = 0
)

/**
 * Richiesta per modificare una scheda esistente
 */
data class UpdateWorkoutPlanRequest(
    @SerializedName("scheda_id")
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