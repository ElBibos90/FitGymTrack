package com.fitgymtrack.app.models

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

/**
 * Rappresenta un elemento nella cronologia degli allenamenti
 */
data class WorkoutHistory(
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
    @SerializedName("scheda_nome")
    val schedaNome: String? = null,
    // Proprietà calcolate
    val isCompleted: Boolean = durataTotale != null && durataTotale > 0
) {
    // Proprietà calcolata per la data formattata
    val formattedDate: String
        get() {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dataAllenamento)
                date?.let { outputFormat.format(it) } ?: dataAllenamento
            } catch (e: Exception) {
                dataAllenamento
            }
        }

    // Proprietà calcolata per la durata formattata
    val formattedDuration: String
        get() {
            return if (durataTotale != null && durataTotale > 0) {
                val hours = durataTotale / 60
                val minutes = durataTotale % 60

                if (hours > 0) {
                    "$hours h ${String.format("%02d", minutes)} min"
                } else {
                    "$minutes min"
                }
            } else {
                "N/D"
            }
        }

    companion object {
        /**
         * Crea un oggetto WorkoutHistory da una mappa di valori
         */
        fun fromMap(map: Map<String, Any>): WorkoutHistory {
            return WorkoutHistory(
                id = map["id"]?.toString()?.toIntOrNull() ?: 0,
                schedaId = map["scheda_id"]?.toString()?.toIntOrNull() ?: 0,
                dataAllenamento = map["data_allenamento"]?.toString() ?: "",
                durataTotale = map["durata_totale"]?.toString()?.toIntOrNull(),
                note = map["note"]?.toString(),
                userId = map["user_id"]?.toString()?.toIntOrNull() ?: 0,
                schedaNome = map["scheda_nome"]?.toString()
            )
        }
    }
}