package com.fitgymtrack.app.models

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

/**
 * Model representing a workout history item
 */
data class WorkoutHistory(
    val id: Int? = null,
    @SerializedName("scheda_id")
    val schedaId: Int? = null,
    @SerializedName("user_id")
    val userId: Int? = null,
    @SerializedName("data_allenamento")
    val dataAllenamento: String? = null,
    @SerializedName("durata_totale")
    val durataTotale: Int? = null,
    val note: String? = null,
    @SerializedName("completato")
    val completato: Int = 0,
    @SerializedName("session_id")
    val sessionId: String? = null,
    @SerializedName("scheda_nome")
    val schedaNome: String? = null
) {
    /**
     * Returns whether the workout is completed
     * A workout is completed if it has a duration (durata_totale)
     */
    val isCompleted: Boolean
        get() = durataTotale != null && durataTotale > 0

    /**
     * Returns the formatted date of the workout
     */
    val formattedDate: String
        get() {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault())

                val date = dataAllenamento?.let { inputFormat.parse(it) }
                date?.let { outputFormat.format(it) } ?: dataAllenamento ?: ""
            } catch (e: Exception) {
                dataAllenamento ?: ""
            }
        }

    /**
     * Returns the formatted duration in hours and minutes
     */
    val formattedDuration: String
        get() {
            if (durataTotale == null) return ""

            val hours = durataTotale / 60
            val minutes = durataTotale % 60

            return when {
                hours > 0 && minutes > 0 -> "$hours h $minutes min"
                hours > 0 -> "$hours h"
                else -> "$minutes min"
            }
        }
}

/**
 * Response for getting workout history
 */
data class WorkoutHistoryResponse(
    val success: Boolean,
    val allenamenti: List<WorkoutHistory>,
    val count: Int
)

/**
 * Request for deleting a series
 */
data class DeleteSeriesRequest(
    @SerializedName("serie_id")
    val serieId: String
)

/**
 * Request for updating a series
 */
data class UpdateSeriesRequest(
    @SerializedName("serie_id")
    val serieId: String,
    val peso: Float,
    val ripetizioni: Int
)

/**
 * Response for series operations
 */
data class SeriesOperationResponse(
    val success: Boolean,
    val message: String
)