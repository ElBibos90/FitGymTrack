package com.fitgymtrack.app.models

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import android.util.Log

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
            // Estrai e converte correttamente i valori numerici
            val idValue = map["id"]
            val schedaIdValue = map["scheda_id"]
            val userIdValue = map["user_id"]

            // Ottieni l'ID allenamento gestendo sia Number che String
            val id = when (idValue) {
                is Number -> idValue.toInt()
                is String -> idValue.toIntOrNull() ?: 0
                else -> {
                    Log.e("WorkoutHistory", "ID allenamento di tipo sconosciuto: ${idValue?.javaClass?.name}")
                    0
                }
            }

            // Ottieni l'ID scheda gestendo sia Number che String
            val schedaId = when (schedaIdValue) {
                is Number -> schedaIdValue.toInt()
                is String -> schedaIdValue.toIntOrNull() ?: 0
                else -> {
                    Log.e("WorkoutHistory", "ID scheda di tipo sconosciuto: ${schedaIdValue?.javaClass?.name}")
                    0
                }
            }

            // Ottieni l'ID utente gestendo sia Number che String
            val userId = when (userIdValue) {
                is Number -> userIdValue.toInt()
                is String -> userIdValue.toIntOrNull() ?: 0
                else -> {
                    Log.e("WorkoutHistory", "ID utente di tipo sconosciuto: ${userIdValue?.javaClass?.name}")
                    0
                }
            }

            // Ottieni la durata totale gestendo sia Number che String
            val durataTotaleValue = map["durata_totale"]
            val durataTotale = when (durataTotaleValue) {
                is Number -> durataTotaleValue.toInt()
                is String -> durataTotaleValue.toIntOrNull()
                else -> null
            }

            // Log per debug
            Log.d("WorkoutHistory", "Conversione allenamento: id=$id (orig=$idValue), " +
                    "schedaId=$schedaId (orig=$schedaIdValue), userId=$userId (orig=$userIdValue)")

            return WorkoutHistory(
                id = id,
                schedaId = schedaId,
                dataAllenamento = map["data_allenamento"]?.toString() ?: "",
                durataTotale = durataTotale,
                note = map["note"]?.toString(),
                userId = userId,
                schedaNome = map["scheda_nome"]?.toString()
            )
        }
    }
}