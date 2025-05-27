package com.fitgymtrack.app.repository

import android.util.Log
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository per la gestione della cronologia degli allenamenti
 */
class WorkoutHistoryRepository {
    private val apiService = ApiClient.workoutHistoryApiService

    /**
     * Recupera la cronologia degli allenamenti di un utente
     */
    suspend fun getWorkoutHistory(userId: Int): Result<List<Map<String, Any>>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WorkoutHistory", "Richiesta allenamenti per utente $userId")
                val response = apiService.getWorkoutHistory(userId)

                // Estraiamo i dati dalla risposta Map<String, Any>
                val success = response["success"] as? Boolean == true
                @Suppress("UNCHECKED_CAST")
                val allenamenti = response["allenamenti"] as? List<Map<String, Any>> ?: emptyList()
                val count = allenamenti.size

                Log.d("WorkoutHistory", "Risposta getWorkoutHistory: success=$success, count=$count")

                if (success) {
                    // Log per debugging
                    allenamenti.take(3).forEachIndexed { index, workout ->
                        val id = workout["id"]?.toString() ?: "N/A"
                        val schedaId = workout["scheda_id"]?.toString() ?: "N/A"
                        val schedaNome = workout["scheda_nome"]?.toString() ?: "N/A"
                        val data = workout["data_allenamento"]?.toString() ?: "N/A"

                        Log.d("WorkoutHistory", "Allenamento[$index]: id=$id, schedaId=$schedaId, nome=$schedaNome, data=$data")
                    }

                    Result.success(allenamenti)
                } else {
                    Log.e("WorkoutHistory", "Errore nel recupero della cronologia allenamenti")
                    Result.failure(Exception("Errore nel recupero della cronologia allenamenti"))
                }
            } catch (e: Exception) {
                Log.e("WorkoutHistory", "Errore getWorkoutHistory: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Recupera i dettagli delle serie completate per un allenamento specifico
     */
    suspend fun getWorkoutSeriesDetail(allenamentoId: Int): Result<List<CompletedSeriesData>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WorkoutHistory", "Richiesta serie per allenamento $allenamentoId")
                val response = apiService.getWorkoutSeriesDetail(allenamentoId)

                Log.d("WorkoutHistory", "Risposta API: success=${response.success}, serie=${response.serie.size}")

                if (response.success) {
                    // Log delle serie per debug
                    response.serie.forEach { serie ->
                        Log.d("WorkoutHistory", "Serie trovata: id=${serie.id}, " +
                                "esercizioId=${serie.esercizioId}, " +
                                "schedaEsercizioId=${serie.schedaEsercizioId}, " +
                                "esercizioNome=${serie.esercizioNome}, " +
                                "peso=${serie.peso}, rip=${serie.ripetizioni}, " +
                                "serieNumber=${serie.serieNumber}, " +
                                "realSerieNumber=${serie.realSerieNumber}")
                    }
                    Result.success(response.serie)
                } else {
                    Log.e("WorkoutHistory", "Errore nel recupero delle serie")
                    Result.failure(Exception("Errore nel recupero delle serie completate"))
                }
            } catch (e: Exception) {
                Log.e("WorkoutHistory", "Errore getWorkoutSeriesDetail: ${e.message}", e)
                e.printStackTrace() // Stampa lo stack trace per diagnostica
                Result.failure(e)
            }
        }
    }

    /**
     * Elimina una serie completata
     */
    suspend fun deleteCompletedSeries(seriesId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WorkoutHistory", "Eliminazione serie: $seriesId")
                val request = DeleteSeriesRequest(seriesId)
                val response = apiService.deleteCompletedSeries(request)

                Log.d("WorkoutHistory", "Risposta deleteCompletedSeries: success=${response.success}")
                Result.success(response.success)
            } catch (e: Exception) {
                Log.e("WorkoutHistory", "Errore deleteCompletedSeries: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Aggiorna una serie completata
     */
    suspend fun updateCompletedSeries(
        seriesId: String,
        weight: Float,
        reps: Int,
        recoveryTime: Int? = null,
        notes: String? = null
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WorkoutHistory", "Aggiornamento serie: id=$seriesId, peso=$weight, rip=$reps")
                val request = UpdateSeriesRequest(seriesId, weight, reps, recoveryTime, notes)
                val response = apiService.updateCompletedSeries(request)

                Log.d("WorkoutHistory", "Risposta updateCompletedSeries: success=${response.success}")
                Result.success(response.success)
            } catch (e: Exception) {
                Log.e("WorkoutHistory", "Errore updateCompletedSeries: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Elimina un intero allenamento
     */
    suspend fun deleteWorkout(workoutId: Int): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("WorkoutHistory", "Eliminazione allenamento: $workoutId")
                val response = apiService.deleteWorkout(mapOf("allenamento_id" to workoutId))

                Log.d("WorkoutHistory", "Risposta deleteWorkout: success=${response.success}")
                Result.success(response.success)
            } catch (e: Exception) {
                Log.e("WorkoutHistory", "Errore deleteWorkout: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}