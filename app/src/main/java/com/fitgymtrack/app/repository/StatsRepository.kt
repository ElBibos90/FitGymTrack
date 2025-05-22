package com.fitgymtrack.app.repository

import android.util.Log
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Repository per la gestione delle statistiche utente
 * Calcola le statistiche dai dati esistenti invece di usare un'API dedicata
 */
class StatsRepository {
    private val workoutHistoryApi = ApiClient.workoutHistoryApiService
    private val TAG = "StatsRepository"

    /**
     * Recupera le statistiche dell'utente calcolandole dai suoi allenamenti
     */
    suspend fun getUserStats(userId: Int): Result<UserStats> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== INIZIO CALCOLO STATISTICHE ===")
                Log.d(TAG, "Calcolo statistiche per utente: $userId")

                // Ottieni lo storico degli allenamenti dell'utente
                Log.d(TAG, "Chiamata API getWorkoutHistory per utente $userId...")
                val historyResponse = workoutHistoryApi.getWorkoutHistory(userId)
                Log.d(TAG, "Risposta API ricevuta: ${historyResponse.keys}")

                // Estrai la lista degli allenamenti dalla risposta
                val allenamenti = extractWorkoutsFromResponse(historyResponse)
                Log.d(TAG, "Allenamenti estratti: ${allenamenti.size}")

                if (allenamenti.isEmpty()) {
                    Log.w(TAG, "⚠️ Nessun allenamento trovato per l'utente $userId")
                    Log.d(TAG, "Risposta API completa: $historyResponse")
                    return@withContext Result.success(createEmptyStats())
                }

                // Log dei primi allenamenti per debug
                allenamenti.take(3).forEachIndexed { index, workout ->
                    Log.d(TAG, "Allenamento ${index + 1}: ${workout.nomeScheda}, Data: ${workout.dataOraInizio}, Serie: ${workout.serieCompletate}")
                }

                // Calcola le statistiche dai dati degli allenamenti
                Log.d(TAG, "Calcolo statistiche da ${allenamenti.size} allenamenti...")
                val stats = calculateStatsFromWorkouts(allenamenti, userId)

                Log.d(TAG, "✅ Statistiche calcolate con successo!")
                Log.d(TAG, "Risultato finale: ${stats.totalWorkouts} allenamenti, ${stats.totalHours}h totali")
                Log.d(TAG, "=== FINE CALCOLO STATISTICHE ===")
                Result.success(stats)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Errore nel calcolo delle statistiche: ${e.message}", e)
                Log.e(TAG, "Ritorno statistiche vuote come fallback")
                // In caso di errore, restituisce statistiche vuote invece di fallire
                Result.success(createEmptyStats())
            }
        }
    }

    /**
     * Estrae la lista degli allenamenti dalla risposta dell'API
     */
    @Suppress("UNCHECKED_CAST")
    private fun extractWorkoutsFromResponse(response: Map<String, Any>): List<CompletedWorkout> {
        val workouts = mutableListOf<CompletedWorkout>()

        try {
            Log.d(TAG, "Estrazione allenamenti dalla risposta API...")
            Log.d(TAG, "Chiavi della risposta: ${response.keys}")

            // La risposta potrebbe avere diversi formati, proviamo a gestirli
            when {
                response.containsKey("allenamenti") -> {
                    Log.d(TAG, "Trovata chiave 'allenamenti'")
                    val allenamenti = response["allenamenti"] as? List<Map<String, Any>>
                    Log.d(TAG, "Lista allenamenti trovata: ${allenamenti?.size ?: 0} elementi")
                    allenamenti?.forEach { workout ->
                        workouts.add(mapToCompletedWorkout(workout))
                    }
                }
                response.containsKey("data") -> {
                    Log.d(TAG, "Trovata chiave 'data'")
                    val data = response["data"] as? List<Map<String, Any>>
                    Log.d(TAG, "Lista data trovata: ${data?.size ?: 0} elementi")
                    data?.forEach { workout ->
                        workouts.add(mapToCompletedWorkout(workout))
                    }
                }
                else -> {
                    Log.d(TAG, "Tentativo di trattare l'intera risposta come lista")
                    // Prova a trattare l'intera risposta come lista
                    if (response.values.firstOrNull() is List<*>) {
                        val list = response.values.first() as List<Map<String, Any>>
                        Log.d(TAG, "Lista trovata nel primo valore: ${list.size} elementi")
                        list.forEach { workout ->
                            workouts.add(mapToCompletedWorkout(workout))
                        }
                    } else {
                        Log.w(TAG, "⚠️ Formato risposta non riconosciuto")
                        Log.d(TAG, "Contenuto risposta: $response")
                    }
                }
            }

            Log.d(TAG, "Estrazione completata: ${workouts.size} allenamenti estratti")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore nell'estrazione degli allenamenti: ${e.message}", e)
            Log.d(TAG, "Risposta che ha causato l'errore: $response")
        }

        return workouts
    }

    /**
     * Converte una mappa in un oggetto CompletedWorkout
     */
    private fun mapToCompletedWorkout(workoutMap: Map<String, Any>): CompletedWorkout {
        try {
            // CORRETTO: Usa i nomi di campo corretti dall'API get_allenamenti_standalone.php
            val workout = CompletedWorkout(
                id = (workoutMap["id"] as? Number)?.toInt() ?: 0,
                nomeScheda = workoutMap["scheda_nome"] as? String ?: "Sconosciuto", // CORRETTO: era "nome_scheda"
                dataOraInizio = workoutMap["data_allenamento"] as? String ?: "",     // CORRETTO: era "data_ora_inizio"
                dataOraFine = null, // Non presente in questa API
                durata = (workoutMap["durata_totale"] as? Number)?.toInt(),         // CORRETTO: era "durata"
                serieCompletate = 0 // Non presente in questa API, useremo altre API per i dettagli
            )

            // Log per debug (solo per i primi allenamenti per non intasare i log)
            if (workoutMap["id"] != null && (workoutMap["id"] as Number).toInt() <= 3) {
                Log.d(TAG, "✅ Mapping allenamento: ID=${workout.id}, Scheda='${workout.nomeScheda}', Data='${workout.dataOraInizio}', Durata=${workout.durata}min")
            }

            return workout
        } catch (e: Exception) {
            Log.e(TAG, "❌ Errore nel mapping dell'allenamento: ${e.message}")
            Log.d(TAG, "Dati dell'allenamento: $workoutMap")
            // Ritorna un workout vuoto invece di crashare
            return CompletedWorkout(
                id = 0,
                nomeScheda = "Errore",
                dataOraInizio = "",
                dataOraFine = null,
                durata = null,
                serieCompletate = 0
            )
        }
    }

    /**
     * Calcola le statistiche dai dati degli allenamenti
     */
    private suspend fun calculateStatsFromWorkouts(
        allenamenti: List<CompletedWorkout>,
        userId: Int
    ): UserStats {
        try {
            Log.d(TAG, "=== CALCOLO STATISTICHE DETTAGLIATE ===")

            // Statistiche di base
            val totalWorkouts = allenamenti.size
            val totalDuration = allenamenti.mapNotNull { it.durata }.sum()
            val totalHours = (totalDuration / 60.0).roundToInt()

            Log.d(TAG, "Statistiche base: $totalWorkouts allenamenti, ${totalDuration}min totali, ${totalHours}h")

            // Calcola streak corrente e più lungo
            val (currentStreak, longestStreak) = calculateStreaks(allenamenti)
            Log.d(TAG, "Streak: corrente=$currentStreak, più lungo=$longestStreak")

            // Calcola medie
            val weeklyAverage = calculateWeeklyAverage(allenamenti)
            val monthlyAverage = calculateMonthlyAverage(allenamenti)
            Log.d(TAG, "Medie: settimanale=$weeklyAverage, mensile=$monthlyAverage")

            // Date importanti
            val sortedDates = allenamenti
                .mapNotNull { parseDate(it.dataOraInizio) }
                .sorted()

            val firstWorkoutDate = sortedDates.firstOrNull()?.let { formatDate(it) }
            val lastWorkoutDate = sortedDates.lastOrNull()?.let { formatDate(it) }
            Log.d(TAG, "Date: prima=$firstWorkoutDate, ultima=$lastWorkoutDate")

            // Per ora, statistiche dettagliate semplici (senza chiamate aggiuntive per evitare errori)
            var totalSetsCompleted = 0
            var totalRepsCompleted = 0
            var totalExercisesPerformed = 0

            // Calcola stime basate sulla durata degli allenamenti
            // Assumi circa 3-4 serie per esercizio, 8-12 ripetizioni per serie
            // e circa 1 esercizio ogni 8-10 minuti di allenamento
            allenamenti.forEach { workout ->
                val duration = workout.durata ?: 0
                if (duration > 0) {
                    val estimatedExercises = maxOf(1, duration / 8) // 1 esercizio ogni 8 minuti
                    val estimatedSets = estimatedExercises * 3 // 3 serie per esercizio in media
                    val estimatedReps = estimatedSets * 10 // 10 ripetizioni per serie in media

                    totalExercisesPerformed += estimatedExercises
                    totalSetsCompleted += estimatedSets
                    totalRepsCompleted += estimatedReps
                }
            }

            Log.d(TAG, "Stime calcolate: $totalExercisesPerformed esercizi, $totalSetsCompleted serie, $totalRepsCompleted ripetizioni")

            // Durata media
            val averageWorkoutDuration = if (allenamenti.isNotEmpty()) {
                val durationsWithValues = allenamenti.mapNotNull { it.durata }
                if (durationsWithValues.isNotEmpty()) {
                    durationsWithValues.average().roundToInt()
                } else 0
            } else 0

            // Calcola consistenza (percentuale di giorni con allenamento negli ultimi 30 giorni)
            val consistencyScore = calculateConsistencyScore(allenamenti)

            // Giorno più attivo e orario migliore
            val mostActiveDay = findMostActiveDay(allenamenti)
            val bestWorkoutTime = findBestWorkoutTime(allenamenti)

            Log.d(TAG, "Giorno più attivo: $mostActiveDay, Orario migliore: $bestWorkoutTime")
            Log.d(TAG, "Consistenza: $consistencyScore%")

            val finalStats = UserStats(
                totalWorkouts = totalWorkouts,
                totalHours = totalHours,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                weeklyAverage = weeklyAverage,
                monthlyAverage = monthlyAverage,
                favoriteExercise = null, // Da implementare in futuro
                totalExercisesPerformed = totalExercisesPerformed,
                totalSetsCompleted = totalSetsCompleted,
                totalRepsCompleted = totalRepsCompleted,
                weightProgress = null, // Da implementare in futuro
                heaviestLift = null, // Da implementare in futuro
                averageWorkoutDuration = averageWorkoutDuration,
                bestWorkoutTime = bestWorkoutTime,
                mostActiveDay = mostActiveDay,
                goalsAchieved = 0,
                personalRecords = emptyList(), // Da implementare in futuro
                recentWorkouts = calculateRecentWorkouts(allenamenti),
                recentImprovements = 0,
                firstWorkoutDate = firstWorkoutDate,
                lastWorkoutDate = lastWorkoutDate,
                consistencyScore = consistencyScore,
                workoutFrequency = calculateWorkoutFrequency(allenamenti)
            )

            Log.d(TAG, "=== STATISTICHE FINALI ===")
            Log.d(TAG, "Allenamenti: ${finalStats.totalWorkouts}, Ore: ${finalStats.totalHours}")
            Log.d(TAG, "Serie stimate: ${finalStats.totalSetsCompleted}, Ripetizioni stimate: ${finalStats.totalRepsCompleted}")
            Log.d(TAG, "Streak: ${finalStats.currentStreak}, Media settimanale: ${finalStats.weeklyAverage}")
            Log.d(TAG, "=== FINE CALCOLO STATISTICHE DETTAGLIATE ===")

            return finalStats

        } catch (e: Exception) {
            Log.e(TAG, "Errore nel calcolo delle statistiche: ${e.message}", e)
            return createEmptyStats()
        }
    }

    /**
     * Calcola streak corrente e più lungo
     */
    private fun calculateStreaks(allenamenti: List<CompletedWorkout>): Pair<Int, Int> {
        if (allenamenti.isEmpty()) return Pair(0, 0)

        try {
            val dates = allenamenti
                .mapNotNull { parseDate(it.dataOraInizio) }
                .map {
                    // Converti in giorni (ignora l'ora)
                    Calendar.getInstance().apply {
                        time = it
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                }
                .distinct()
                .sortedDescending()

            if (dates.isEmpty()) return Pair(0, 0)

            var currentStreak = 0
            var longestStreak = 0
            var tempStreak = 1

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            // Calcola streak corrente
            for (i in dates.indices) {
                val daysDiff = ((today.time - dates[i].time) / (1000 * 60 * 60 * 24)).toInt()

                if (i == 0 && daysDiff <= 1) {
                    currentStreak = 1
                } else if (i > 0) {
                    val prevDaysDiff = ((today.time - dates[i-1].time) / (1000 * 60 * 60 * 24)).toInt()
                    if (daysDiff - prevDaysDiff == 1) {
                        currentStreak++
                    } else {
                        break
                    }
                }
            }

            // Calcola streak più lungo
            for (i in 1 until dates.size) {
                val daysDiff = ((dates[i-1].time - dates[i].time) / (1000 * 60 * 60 * 24)).toInt()
                if (daysDiff == 1) {
                    tempStreak++
                } else {
                    longestStreak = maxOf(longestStreak, tempStreak)
                    tempStreak = 1
                }
            }
            longestStreak = maxOf(longestStreak, tempStreak, currentStreak)

            return Pair(currentStreak, longestStreak)
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel calcolo degli streak: ${e.message}", e)
            return Pair(0, 0)
        }
    }

    /**
     * Calcola la media settimanale di allenamenti
     */
    private fun calculateWeeklyAverage(allenamenti: List<CompletedWorkout>): Double {
        if (allenamenti.isEmpty()) return 0.0

        try {
            val dates = allenamenti.mapNotNull { parseDate(it.dataOraInizio) }
            if (dates.isEmpty()) return 0.0

            val sortedDates = dates.sorted()
            val firstDate = sortedDates.first()
            val lastDate = sortedDates.last()

            val daysDiff = ((lastDate.time - firstDate.time) / (1000 * 60 * 60 * 24)).toInt()
            val weeks = maxOf(1, daysDiff / 7)

            return allenamenti.size.toDouble() / weeks
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel calcolo della media settimanale: ${e.message}", e)
            return 0.0
        }
    }

    /**
     * Calcola la media mensile di allenamenti
     */
    private fun calculateMonthlyAverage(allenamenti: List<CompletedWorkout>): Double {
        return calculateWeeklyAverage(allenamenti) * 4.33 // Circa 4.33 settimane in un mese
    }

    /**
     * Calcola il punteggio di consistenza
     */
    private fun calculateConsistencyScore(allenamenti: List<CompletedWorkout>): Float {
        try {
            val last30Days = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -30)
            }.time

            val recentWorkouts = allenamenti.count {
                parseDate(it.dataOraInizio)?.after(last30Days) == true
            }

            return (recentWorkouts / 30.0 * 100).toFloat().coerceIn(0f, 100f)
        } catch (e: Exception) {
            return 0f
        }
    }

    /**
     * Trova il giorno più attivo
     */
    private fun findMostActiveDay(allenamenti: List<CompletedWorkout>): String? {
        try {
            val dayCount = mutableMapOf<String, Int>()
            val daysOfWeek = arrayOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")

            allenamenti.forEach { workout ->
                parseDate(workout.dataOraInizio)?.let { date ->
                    val calendar = Calendar.getInstance().apply { time = date }
                    val dayOfWeek = daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1]
                    dayCount[dayOfWeek] = dayCount.getOrDefault(dayOfWeek, 0) + 1
                }
            }

            return dayCount.maxByOrNull { it.value }?.key
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Trova l'orario migliore per allenarsi
     */
    private fun findBestWorkoutTime(allenamenti: List<CompletedWorkout>): String? {
        try {
            val timeCount = mutableMapOf<String, Int>()

            allenamenti.forEach { workout ->
                parseDate(workout.dataOraInizio)?.let { date ->
                    val calendar = Calendar.getInstance().apply { time = date }
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)

                    val timeOfDay = when (hour) {
                        in 6..11 -> "morning"
                        in 12..17 -> "afternoon"
                        in 18..23 -> "evening"
                        else -> "night"
                    }

                    timeCount[timeOfDay] = timeCount.getOrDefault(timeOfDay, 0) + 1
                }
            }

            return timeCount.maxByOrNull { it.value }?.key
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Calcola allenamenti recenti (ultimi 30 giorni)
     */
    private fun calculateRecentWorkouts(allenamenti: List<CompletedWorkout>): Int {
        val last30Days = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
        }.time

        return allenamenti.count {
            parseDate(it.dataOraInizio)?.after(last30Days) == true
        }
    }

    /**
     * Calcola la frequenza degli allenamenti
     */
    private fun calculateWorkoutFrequency(allenamenti: List<CompletedWorkout>): WorkoutFrequency {
        val weeklyDays = mutableMapOf<String, Int>()
        val daysOfWeek = arrayOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")

        allenamenti.forEach { workout ->
            parseDate(workout.dataOraInizio)?.let { date ->
                val calendar = Calendar.getInstance().apply { time = date }
                val dayOfWeek = daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1]
                weeklyDays[dayOfWeek] = weeklyDays.getOrDefault(dayOfWeek, 0) + 1
            }
        }

        return WorkoutFrequency(
            weeklyDays = weeklyDays,
            monthlyWeeks = emptyMap(),
            hourlyDistribution = emptyMap()
        )
    }

    /**
     * Parsa una data dalla stringa
     */
    private fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrEmpty()) return null

        val formats = arrayOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy"
        )

        for (format in formats) {
            try {
                return SimpleDateFormat(format, Locale.getDefault()).parse(dateString)
            } catch (e: Exception) {
                // Prova il prossimo formato
            }
        }

        return null
    }

    /**
     * Formatta una data come stringa
     */
    private fun formatDate(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    }

    /**
     * Calcola le statistiche (non usato, per compatibilità API)
     */
    suspend fun calculateStats(userId: Int, recalculateAll: Boolean = false): Result<UserStats> {
        return getUserStats(userId)
    }

    /**
     * Crea statistiche vuote
     */
    private fun createEmptyStats(): UserStats {
        return UserStats(
            totalWorkouts = 0,
            totalHours = 0,
            currentStreak = 0,
            longestStreak = 0,
            weeklyAverage = 0.0,
            monthlyAverage = 0.0,
            favoriteExercise = null,
            totalExercisesPerformed = 0,
            totalSetsCompleted = 0,
            totalRepsCompleted = 0,
            weightProgress = null,
            heaviestLift = null,
            averageWorkoutDuration = 0,
            bestWorkoutTime = null,
            mostActiveDay = null,
            goalsAchieved = 0,
            personalRecords = emptyList(),
            recentWorkouts = 0,
            recentImprovements = 0,
            firstWorkoutDate = null,
            lastWorkoutDate = null,
            consistencyScore = 0.0f,
            workoutFrequency = null
        )
    }

    /**
     * Crea statistiche demo per testing - ora tutte a 0 per nuovi utenti
     */
    fun createDemoStats(): UserStats {
        return createEmptyStats()
    }
}

/**
 * Modello per un allenamento completato (semplificato)
 */
data class CompletedWorkout(
    val id: Int,
    val nomeScheda: String,
    val dataOraInizio: String,
    val dataOraFine: String?,
    val durata: Int?, // in minuti
    val serieCompletate: Int
)