package com.fitgymtrack.app.repository

import android.util.Log
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository per la gestione degli abbonamenti
 */
class SubscriptionRepository {
    private val apiService = ApiClient.apiService
    private val TAG = "SubscriptionRepository"

    /**
     * Recupera l'abbonamento corrente dell'utente
     */
    suspend fun getCurrentSubscription(): Result<Subscription> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Recupero abbonamento corrente")
                val response = apiService.getCurrentSubscription()

                // Estrai i dati dalla mappa
                @Suppress("UNCHECKED_CAST")
                val subscription = response["subscription"] as? Map<String, Any>

                if (subscription != null) {
                    val planName = subscription["plan_name"] as? String ?: "Free"
                    val price = (subscription["price"] as? Number)?.toDouble() ?: 0.0
                    val maxWorkouts = (subscription["max_workouts"] as? Number)?.toInt()

                    // Conteggio schede: recuperiamo il valore attuale
                    val limitCheckResult = apiService.checkResourceLimits("max_workouts")
                    val currentCount = (limitCheckResult["current_count"] as? Number)?.toInt() ?: 0

                    val maxCustomExercises = (subscription["max_custom_exercises"] as? Number)?.toInt()

                    // Conteggio esercizi personalizzati: recuperiamo il valore attuale
                    val exerciseLimitResult = apiService.checkResourceLimits("max_custom_exercises")
                    val currentCustomExercises = (exerciseLimitResult["current_count"] as? Number)?.toInt() ?: 0

                    // Aggiungi proprietà avanzate
                    val advancedStats = (subscription["advanced_stats"] as? Number)?.toInt() == 1
                    val cloudBackup = (subscription["cloud_backup"] as? Number)?.toInt() == 1
                    val noAds = (subscription["no_ads"] as? Number)?.toInt() == 1

                    Log.d(TAG, "Abbonamento recuperato: Piano=$planName, Schede=$currentCount/$maxWorkouts, Esercizi=$currentCustomExercises/$maxCustomExercises")

                    Result.success(Subscription(
                        planId = (subscription["plan_id"] as? Number)?.toInt() ?: 0,
                        planName = planName,
                        price = price,
                        maxWorkouts = maxWorkouts,
                        currentCount = currentCount,
                        maxCustomExercises = maxCustomExercises,
                        currentCustomExercises = currentCustomExercises,
                        advancedStats = advancedStats,
                        cloudBackup = cloudBackup,
                        noAds = noAds
                    ))
                } else {
                    Log.e(TAG, "Dati abbonamento non disponibili")
                    Result.failure(Exception("Dati abbonamento non disponibili"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore nel recupero dell'abbonamento: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Verifica i limiti di utilizzo per un determinato tipo di risorsa
     */
    suspend fun checkResourceLimits(resourceType: String): Result<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Verifica limiti per: $resourceType")
                val response = apiService.checkResourceLimits(resourceType)

                Log.d(TAG, "Risposta limiti: $response")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Errore nella verifica dei limiti: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Aggiorna il piano di abbonamento
     */
    suspend fun updatePlan(planId: Int): Result<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Aggiornamento al piano ID: $planId")
                val request = mapOf("plan_id" to planId)
                val response = apiService.updatePlan(request)

                Log.d(TAG, "Risposta aggiornamento piano: $response")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Errore nell'aggiornamento del piano: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}