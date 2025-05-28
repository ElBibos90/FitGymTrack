package com.fitgymtrack.app.extensions

import android.content.Context
import android.util.Log
import com.fitgymtrack.app.models.WorkoutPlan
import com.fitgymtrack.app.services.NotificationIntegrationService
import com.fitgymtrack.app.utils.SubscriptionLimitChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estensioni per integrare notifiche automatiche nei workflow esistenti
 */
object WorkoutNotificationExtensions {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private const val TAG = "WorkoutNotificationExt"

    /**
     * Notifica completamento workout
     */
    fun notifyWorkoutCompleted(
        context: Context,
        workoutName: String,
        durationMinutes: Long,
        exerciseCount: Int
    ) {
        scope.launch {
            try {
                Log.d(TAG, "💪 Workout completato: $workoutName")

                val service = NotificationIntegrationService.getInstance(context)
                service.notifyWorkoutCompleted(
                    workoutName = workoutName,
                    duration = durationMinutes,
                    exerciseCount = exerciseCount
                )

                // Check for achievements
                checkWorkoutAchievements(context, workoutName, durationMinutes)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore notifica workout: ${e.message}", e)
            }
        }
    }

    /**
     * Controlla e notifica traguardi raggiunti
     */
    private fun checkWorkoutAchievements(
        context: Context,
        workoutName: String,
        durationMinutes: Long
    ) {
        scope.launch {
            try {
                val service = NotificationIntegrationService.getInstance(context)

                // Esempio traguardi
                when {
                    durationMinutes >= 60 -> {
                        service.notifyAchievement(
                            "Warrior!",
                            "Hai completato un allenamento di oltre 1 ora! 💪"
                        )
                    }
                    durationMinutes >= 30 -> {
                        service.notifyAchievement(
                            "Costanza!",
                            "Altro allenamento di 30+ minuti completato! 🔥"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore check achievements: ${e.message}", e)
            }
        }
    }

    /**
     * Controlla limiti prima di creare workout/esercizio
     * FIX: Gestisce correttamente le suspend functions
     */
    fun checkLimitsBeforeCreation(
        context: Context,
        resourceType: String, // "workouts" o "custom_exercises"
        onLimitReached: () -> Unit,
        onCanProceed: () -> Unit
    ) {
        scope.launch {
            try {
                Log.d(TAG, "🔍 Controllo limiti per: $resourceType")

                // FIX: Chiamate suspend corrette
                val (limitReached, currentCount, maxAllowed) = try {
                    when (resourceType) {
                        "workouts" -> SubscriptionLimitChecker.canCreateWorkout()
                        "custom_exercises" -> SubscriptionLimitChecker.canCreateCustomExercise()
                        else -> Triple(false, 0, null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Errore chiamata SubscriptionLimitChecker: ${e.message}", e)
                    // In caso di errore, permettiamo la creazione
                    Triple(false, 0, null)
                }

                if (limitReached && maxAllowed != null) {
                    Log.d(TAG, "🚨 Limite raggiunto: $currentCount/$maxAllowed")

                    // Crea notifica invece di mostrare banner
                    val service = NotificationIntegrationService.getInstance(context)
                    service.checkResourceLimits(
                        resourceType = resourceType,
                        currentCount = currentCount,
                        maxAllowed = maxAllowed,
                        isLimitReached = true
                    )

                    // FIX: Callback per UI sul Main thread
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onLimitReached()
                    }
                } else {
                    Log.d(TAG, "✅ Può procedere: $currentCount/${maxAllowed ?: "∞"}")
                    // FIX: Callback per UI sul Main thread
                    withContext(Dispatchers.Main) {
                        onCanProceed()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore controllo limiti: ${e.message}", e)
                // In caso di errore, permetti la creazione
                onCanProceed()
            }
        }
    }

    /**
     * Integrazione con WorkoutPlan creazione
     */
    fun onWorkoutPlanCreated(context: Context, workoutPlan: WorkoutPlan) {
        scope.launch {
            try {
                // Usa il campo corretto del WorkoutPlan (probabilmente 'title' invece di 'name')
                val planName = workoutPlan.nome ?: "Workout Plan"
                Log.d(TAG, "📝 Workout plan creato: $planName")

                // Qui potresti aggiungere logica per:
                // - Notifiche di completamento creazione
                // - Suggerimenti per primo allenamento
                // - Achievement per numero schede create

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore on workout created: ${e.message}", e)
            }
        }
    }

    /**
     * Notifica per promemoria allenamento
     */
    fun scheduleWorkoutReminder(
        context: Context,
        workoutName: String,
        daysSinceLastWorkout: Int
    ) {
        if (daysSinceLastWorkout >= 3) {
            scope.launch {
                try {
                    Log.d(TAG, "⏰ Promemoria allenamento dopo $daysSinceLastWorkout giorni")

                    val service = NotificationIntegrationService.getInstance(context)
                    // Qui useresti REMINDER type quando sarà implementato

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Errore promemoria: ${e.message}", e)
                }
            }
        }
    }
}

/**
 * Helper per integrazioni rapide
 */
object NotificationHelper {

    /**
     * Quick method per notificare subscription issues
     */
    fun checkSubscriptionAndNotify(context: Context) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val service = NotificationIntegrationService.getInstance(context)
                service.checkAppUpdates()

            } catch (e: Exception) {
                Log.e("NotificationHelper", "❌ Errore quick check: ${e.message}", e)
            }
        }
    }

    /**
     * Test method per creare notifiche di esempio
     */
    fun createTestNotifications(context: Context) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val service = NotificationIntegrationService.getInstance(context)

                // Test workout completion
                service.notifyWorkoutCompleted(
                    workoutName = "Push Day",
                    duration = 45,
                    exerciseCount = 8
                )

                // Test achievement
                service.notifyAchievement(
                    "Primo Traguardo!",
                    "Hai completato il tuo primo allenamento! 🎉"
                )

            } catch (e: Exception) {
                Log.e("NotificationHelper", "❌ Errore test notifications: ${e.message}", e)
            }
        }
    }
}