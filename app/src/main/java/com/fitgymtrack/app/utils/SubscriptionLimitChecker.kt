
package com.fitgymtrack.app.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fitgymtrack.app.repository.SubscriptionRepository

/**
 * Utility per verificare i limiti dell'abbonamento
 */
object SubscriptionLimitChecker {
    private val repository = SubscriptionRepository()

    /**
     * Verifica se l'utente può creare una nuova scheda
     * @return Triple(limitReached, currentCount, maxAllowed)
     */
    suspend fun canCreateWorkout(): Triple<Boolean, Int, Int?> {
        return withContext(Dispatchers.IO) {
            val result = repository.checkResourceLimits("max_workouts")

            result.fold(
                onSuccess = { response ->
                    val limitReached = response["limit_reached"] as? Boolean ?: false
                    val currentCount = response["current_count"] as? Int ?: 0
                    val maxAllowed = response["max_allowed"] as? Int

                    Triple(limitReached, currentCount, maxAllowed)
                },
                onFailure = {
                    // In caso di errore, presumiamo che l'utente possa creare
                    // per evitare di bloccare le funzionalità
                    Triple(false, 0, null)
                }
            )
        }
    }

    /**
     * Verifica se l'utente può creare un nuovo esercizio personalizzato
     * @return Triple(limitReached, currentCount, maxAllowed)
     */
    suspend fun canCreateCustomExercise(): Triple<Boolean, Int, Int?> {
        return withContext(Dispatchers.IO) {
            val result = repository.checkResourceLimits("max_custom_exercises")

            result.fold(
                onSuccess = { response ->
                    val limitReached = response["limit_reached"] as? Boolean ?: false
                    val currentCount = response["current_count"] as? Int ?: 0
                    val maxAllowed = response["max_allowed"] as? Int

                    Triple(limitReached, currentCount, maxAllowed)
                },
                onFailure = {
                    // In caso di errore, presumiamo che l'utente possa creare
                    Triple(false, 0, null)
                }
            )
        }
    }
}