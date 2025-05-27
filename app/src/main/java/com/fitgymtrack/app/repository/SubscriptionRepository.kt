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
    private val apiService = ApiClient.subscriptionApiService
    private val TAG = "SubscriptionRepository"

    /**
     * Recupera l'abbonamento corrente dell'utente
     */
    suspend fun getCurrentSubscription(): Result<Subscription> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Recupero abbonamento corrente")
                val response = apiService.getCurrentSubscription()

                if (response.success && response.data?.subscription != null) {
                    // Converti dal modello API al modello dell'app
                    val apiSubscription = response.data.subscription
                    val subscription = Subscription(
                        id = apiSubscription.id,
                        user_id = apiSubscription.user_id,
                        plan_id = apiSubscription.plan_id,
                        planName = apiSubscription.plan_name,
                        status = apiSubscription.status,
                        price = apiSubscription.price,
                        maxWorkouts = apiSubscription.max_workouts,
                        maxCustomExercises = apiSubscription.max_custom_exercises,
                        currentCount = apiSubscription.current_count,
                        currentCustomExercises = apiSubscription.current_custom_exercises,
                        advancedStats = apiSubscription.advanced_stats == 1,
                        cloudBackup = apiSubscription.cloud_backup == 1,
                        noAds = apiSubscription.no_ads == 1,
                        start_date = apiSubscription.start_date,
                        end_date = apiSubscription.end_date
                    )

                    Log.d(TAG, "Abbonamento recuperato con successo")
                    Result.success(subscription)
                } else {
                    Log.e(TAG, "Errore nel recupero dell'abbonamento: ${response.message}")
                    Result.failure(Exception(response.message ?: "Errore sconosciuto"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione nel recupero dell'abbonamento: ${e.message}", e)
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

                if (response.success && response.data != null) {
                    // Converti l'oggetto ResourceLimits in una Map<String, Any>
                    val limitData = mapOf(
                        "limit_reached" to response.data.limit_reached,
                        "current_count" to response.data.current_count,
                        "max_allowed" to (response.data.max_allowed ?: Int.MAX_VALUE),
                        "remaining" to (response.data.remaining ?: Int.MAX_VALUE)
                    )

                    Log.d(TAG, "Limiti verificati: $limitData")
                    Result.success(limitData)
                } else {
                    Log.e(TAG, "Errore nella verifica dei limiti: ${response.message}")
                    Result.failure(Exception(response.message ?: "Errore sconosciuto"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione nella verifica dei limiti: ${e.message}", e)
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
                mapOf("plan_id" to planId)
                val response = apiService.updatePlan(com.fitgymtrack.app.api.UpdatePlanRequest(planId))

                if (response.success && response.data != null) {
                    val resultData = mapOf(
                        "success" to response.data.success,
                        "message" to response.data.message,
                        "plan_name" to response.data.plan_name
                    )

                    Log.d(TAG, "Piano aggiornato con successo: $resultData")
                    Result.success(resultData)
                } else {
                    Log.e(TAG, "Errore nell'aggiornamento del piano: ${response.message}")
                    Result.failure(Exception(response.message ?: "Errore sconosciuto"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione nell'aggiornamento del piano: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}