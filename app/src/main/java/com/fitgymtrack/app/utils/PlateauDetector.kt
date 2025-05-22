package com.fitgymtrack.app.utils

import com.fitgymtrack.app.models.CompletedSeries
import com.fitgymtrack.app.models.WorkoutExercise
import android.util.Log

/**
 * Utility per rilevare i plateau negli allenamenti
 */
object PlateauDetector {

    /**
     * Rileva se un esercizio è in plateau basandosi sullo storico
     * @param exerciseId ID dell'esercizio
     * @param currentWeight Peso attuale che l'utente sta usando
     * @param currentReps Ripetizioni/secondi attuali
     * @param historicData Dati storici delle serie completate
     * @param minSessionsForPlateau Numero minimo di sessioni consecutive per considerare un plateau
     * @return PlateauInfo se è rilevato un plateau, null altrimenti
     */
    fun detectPlateau(
        exerciseId: Int,
        currentWeight: Float,
        currentReps: Int,
        historicData: Map<Int, List<CompletedSeries>>,
        minSessionsForPlateau: Int = 3
    ): PlateauInfo? {
        val exerciseHistory = historicData[exerciseId]

        Log.d("PlateauDetector", "=== ANALISI PLATEAU ESERCIZIO $exerciseId ===")
        Log.d("PlateauDetector", "Peso corrente: $currentWeight, Reps correnti: $currentReps")
        Log.d("PlateauDetector", "Dati storici disponibili: ${exerciseHistory?.size ?: 0} serie")

        // Se non ci sono dati storici, prova a rilevare plateau "simulato" per test
        if (exerciseHistory == null || exerciseHistory.isEmpty()) {
            Log.d("PlateauDetector", "Nessun dato storico - controllo plateau simulato")
            return checkSimulatedPlateau(exerciseId, currentWeight, currentReps)
        }

        // Raggruppa le serie per sessione di allenamento
        val sessionGroups = groupSeriesBySession(exerciseHistory)
        Log.d("PlateauDetector", "Sessioni raggruppate: ${sessionGroups.size}")

        if (sessionGroups.size < minSessionsForPlateau) {
            Log.d("PlateauDetector", "Sessioni insufficienti: ${sessionGroups.size} < $minSessionsForPlateau")
            return tryDetectWithLimitedData(exerciseId, currentWeight, currentReps, exerciseHistory)
        }

        // Prendi le ultime N sessioni per confronto serie per serie
        val recentSessions = sessionGroups.takeLast(minSessionsForPlateau)
        Log.d("PlateauDetector", "Analizzando le ultime $minSessionsForPlateau sessioni per confronto serie per serie")

        // NUOVA LOGICA: Confronto serie per serie
        return detectPlateauSeriesBySeries(
            exerciseId = exerciseId,
            currentWeight = currentWeight,
            currentReps = currentReps,
            recentSessions = recentSessions,
            sessionsCount = minSessionsForPlateau
        )
    }

    /**
     * NUOVA FUNZIONE: Rileva plateau confrontando serie per serie
     */
    private fun detectPlateauSeriesBySeries(
        exerciseId: Int,
        currentWeight: Float,
        currentReps: Int,
        recentSessions: List<List<CompletedSeries>>,
        sessionsCount: Int
    ): PlateauInfo? {

        Log.d("PlateauDetector", "🔍 CONFRONTO SERIE PER SERIE")

        // Organizza le serie per numero di serie
        val seriesByNumber = mutableMapOf<Int, MutableList<CompletedSeries>>()

        recentSessions.forEachIndexed { sessionIndex, session ->
            Log.d("PlateauDetector", "📅 Sessione $sessionIndex: ${session.size} serie")
            session.forEach { series ->
                val serieNumber = series.serieNumber
                if (!seriesByNumber.containsKey(serieNumber)) {
                    seriesByNumber[serieNumber] = mutableListOf()
                }
                seriesByNumber[serieNumber]!!.add(series)
                Log.d("PlateauDetector", "   Serie $serieNumber: ${series.peso}kg x ${series.ripetizioni}")
            }
        }

        Log.d("PlateauDetector", "📊 Organizzazione per numero di serie:")
        seriesByNumber.forEach { (serieNumber, seriesList) ->
            Log.d("PlateauDetector", "Serie $serieNumber: ${seriesList.size} occorrenze nelle sessioni")
        }

        // Controlla ogni serie per plateau
        var plateauDetectedCount = 0
        val totalSeriesChecked = seriesByNumber.size

        seriesByNumber.forEach { (serieNumber, seriesList) ->
            // Verifica se questa serie appare in tutte le sessioni
            if (seriesList.size >= sessionsCount) {
                Log.d("PlateauDetector", "🔍 Controllo plateau Serie $serieNumber:")

                // Prendi le ultime N occorrenze (una per sessione)
                val recentSeries = seriesList.takeLast(sessionsCount)

                recentSeries.forEachIndexed { index, series ->
                    Log.d("PlateauDetector", "   Sessione $index: ${series.peso}kg x ${series.ripetizioni}")
                }

                // Verifica se peso e ripetizioni sono rimasti costanti
                val firstSeries = recentSeries.first()
                val weightTolerance = 1.0f
                val repsTolerance = 1

                val isWeightConstant = recentSeries.all {
                    kotlin.math.abs(it.peso - firstSeries.peso) <= weightTolerance
                }
                val areRepsConstant = recentSeries.all {
                    kotlin.math.abs(it.ripetizioni - firstSeries.ripetizioni) <= repsTolerance
                }

                Log.d("PlateauDetector", "   Serie $serieNumber: peso costante=$isWeightConstant, reps costanti=$areRepsConstant")

                // Se è la prima serie, controlla anche i valori correnti
                if (serieNumber == 1) {
                    val currentMatchesPattern = kotlin.math.abs(currentWeight - firstSeries.peso) <= weightTolerance &&
                            kotlin.math.abs(currentReps - firstSeries.ripetizioni) <= repsTolerance

                    Log.d("PlateauDetector", "   Serie $serieNumber (corrente): valori corrispondono=$currentMatchesPattern")

                    if (isWeightConstant && areRepsConstant && currentMatchesPattern) {
                        plateauDetectedCount++
                        Log.d("PlateauDetector", "🚨 PLATEAU rilevato per Serie $serieNumber!")
                    }
                } else {
                    if (isWeightConstant && areRepsConstant) {
                        plateauDetectedCount++
                        Log.d("PlateauDetector", "🚨 PLATEAU rilevato per Serie $serieNumber!")
                    }
                }
            } else {
                Log.d("PlateauDetector", "⏭️ Serie $serieNumber: insufficienti dati (${seriesList.size}/$sessionsCount sessioni)")
            }
        }

        Log.d("PlateauDetector", "📈 RISULTATO: $plateauDetectedCount/$totalSeriesChecked serie in plateau")

        // Considera plateau se almeno il 50% delle serie sono in plateau
        val plateauThreshold = kotlin.math.max(1, totalSeriesChecked / 2)

        if (plateauDetectedCount >= plateauThreshold) {
            Log.d("PlateauDetector", "🚨 PLATEAU CONFERMATO per esercizio $exerciseId!")
            Log.d("PlateauDetector", "   Serie in plateau: $plateauDetectedCount/$totalSeriesChecked (soglia: $plateauThreshold)")

            // Usa i valori della serie più rappresentativa (tipicamente la prima)
            val representativeSeries = seriesByNumber[1]?.last() ?:
            seriesByNumber.values.first().last()

            return PlateauInfo(
                exerciseId = exerciseId,
                plateauType = determinePlateauType(representativeSeries.peso, representativeSeries.ripetizioni),
                sessionsInPlateau = sessionsCount,
                currentWeight = representativeSeries.peso,
                currentReps = representativeSeries.ripetizioni,
                suggestions = generateProgressionSuggestions(
                    currentWeight = representativeSeries.peso,
                    currentReps = representativeSeries.ripetizioni,
                    exerciseHistory = recentSessions.flatten()
                )
            )
        }

        Log.d("PlateauDetector", "✅ Nessun plateau significativo rilevato")
        return null
    }

    /**
     * NUOVO: Prova a rilevare plateau con dati limitati
     */
    private fun tryDetectWithLimitedData(
        exerciseId: Int,
        currentWeight: Float,
        currentReps: Int,
        exerciseHistory: List<CompletedSeries>
    ): PlateauInfo? {
        Log.d("PlateauDetector", "Tentativo rilevamento con dati limitati")

        // Se abbiamo almeno una serie storica, confrontala con i valori correnti
        if (exerciseHistory.isNotEmpty()) {
            val lastSeries = exerciseHistory.last()
            val weightMatch = kotlin.math.abs(currentWeight - lastSeries.peso) <= 1.0f
            val repsMatch = kotlin.math.abs(currentReps - lastSeries.ripetizioni) <= 1

            Log.d("PlateauDetector", "Confronto con ultima serie: peso match=$weightMatch, reps match=$repsMatch")

            if (weightMatch && repsMatch) {
                Log.d("PlateauDetector", "🚨 PLATEAU LIMITATO rilevato per esercizio $exerciseId!")

                return PlateauInfo(
                    exerciseId = exerciseId,
                    plateauType = determinePlateauType(currentWeight, currentReps),
                    sessionsInPlateau = 1,
                    currentWeight = currentWeight,
                    currentReps = currentReps,
                    suggestions = generateProgressionSuggestions(
                        currentWeight = currentWeight,
                        currentReps = currentReps,
                        exerciseHistory = exerciseHistory
                    )
                )
            }
        }

        return null
    }

    /**
     * NUOVO: Rileva plateau "simulato" per testing quando non ci sono dati storici
     */
    private fun checkSimulatedPlateau(
        exerciseId: Int,
        currentWeight: Float,
        currentReps: Int
    ): PlateauInfo? {
        // Per testing: considera plateau se il peso è un valore "tipico" di plateau
        val isTypicalPlateauWeight = currentWeight > 0f && (
                currentWeight % 5f == 0f || // Pesi multipli di 5
                        currentWeight % 2.5f == 0f   // Pesi multipli di 2.5
                )

        val isTypicalePlateauReps = currentReps in 6..15 // Range tipico di plateau

        Log.d("PlateauDetector", "Test plateau simulato: peso tipico=$isTypicalPlateauWeight, reps tipiche=$isTypicalePlateauReps")

        // SOLO PER TESTING: rileva plateau simulato su alcuni esercizi
        if (isTypicalPlateauWeight && isTypicalePlateauReps && exerciseId % 3 == 0) {
            Log.d("PlateauDetector", "🚨 PLATEAU SIMULATO rilevato per esercizio $exerciseId (per testing)!")

            return PlateauInfo(
                exerciseId = exerciseId,
                plateauType = determinePlateauType(currentWeight, currentReps),
                sessionsInPlateau = 2,
                currentWeight = currentWeight,
                currentReps = currentReps,
                suggestions = generateProgressionSuggestions(
                    currentWeight = currentWeight,
                    currentReps = currentReps,
                    exerciseHistory = emptyList()
                )
            )
        }

        return null
    }

    /**
     * Raggruppa le serie per sessione di allenamento
     */
    private fun groupSeriesBySession(series: List<CompletedSeries>): List<List<CompletedSeries>> {
        Log.d("PlateauDetector", "Raggruppamento ${series.size} serie per sessione...")

        // Prima prova a raggruppare per data (primi 10 caratteri del timestamp)
        val groupedByDate = series
            .groupBy {
                val timestamp = it.timestamp
                // Prendi i primi 10 caratteri se disponibili, altrimenti usa tutto il timestamp
                if (timestamp.length >= 10) timestamp.substring(0, 10) else timestamp
            }
            .values
            .toList()
            .sortedBy { it.first().timestamp }

        Log.d("PlateauDetector", "Raggruppamento per data: ${groupedByDate.size} sessioni")
        groupedByDate.forEachIndexed { index, session ->
            val date = session.first().timestamp.take(10)
            Log.d("PlateauDetector", "Sessione $index ($date): ${session.size} serie")
        }

        // Se abbiamo solo una sessione ma molte serie, prova un raggruppamento alternativo
        if (groupedByDate.size == 1 && series.size >= 6) {
            Log.d("PlateauDetector", "Tentativo raggruppamento alternativo per serie multiple...")

            // Raggruppa ogni 3-4 serie come sessioni separate (simulazione)
            val alternativeGroups = mutableListOf<List<CompletedSeries>>()
            val seriesPerSession = 3

            for (i in series.indices step seriesPerSession) {
                val sessionEnd = minOf(i + seriesPerSession, series.size)
                val sessionSeries = series.subList(i, sessionEnd)
                if (sessionSeries.isNotEmpty()) {
                    alternativeGroups.add(sessionSeries)
                }
            }

            Log.d("PlateauDetector", "Raggruppamento alternativo: ${alternativeGroups.size} sessioni simulate")
            return alternativeGroups
        }

        return groupedByDate
    }

    /**
     * Determina il tipo di plateau
     */
    private fun determinePlateauType(weight: Float, reps: Int): PlateauType {
        return when {
            weight < 10f -> PlateauType.LIGHT_WEIGHT
            weight > 100f -> PlateauType.HEAVY_WEIGHT
            reps < 5 -> PlateauType.LOW_REPS
            reps > 15 -> PlateauType.HIGH_REPS
            else -> PlateauType.MODERATE
        }
    }

    /**
     * Genera suggerimenti per la progressione
     */
    private fun generateProgressionSuggestions(
        currentWeight: Float,
        currentReps: Int,
        exerciseHistory: List<CompletedSeries>
    ): List<ProgressionSuggestion> {
        val suggestions = mutableListOf<ProgressionSuggestion>()

        // Suggerisci aumento di peso
        val weightIncrement = when {
            currentWeight < 10f -> 0.5f
            currentWeight < 50f -> 1.25f
            currentWeight < 100f -> 2.5f
            else -> 5f
        }

        suggestions.add(
            ProgressionSuggestion(
                type = SuggestionType.INCREASE_WEIGHT,
                description = "Prova ad aumentare il peso a ${WeightFormatter.formatWeight(currentWeight + weightIncrement)} kg",
                newWeight = currentWeight + weightIncrement,
                newReps = currentReps,
                confidence = calculateWeightIncreaseConfidence(currentWeight, exerciseHistory)
            )
        )

        // Suggerisci aumento ripetizioni
        val repsIncrement = when {
            currentReps < 8 -> 1
            currentReps < 12 -> 2
            else -> 3
        }

        suggestions.add(
            ProgressionSuggestion(
                type = SuggestionType.INCREASE_REPS,
                description = "Prova ad aumentare le ripetizioni a ${currentReps + repsIncrement}",
                newWeight = currentWeight,
                newReps = currentReps + repsIncrement,
                confidence = calculateRepsIncreaseConfidence(currentReps, exerciseHistory)
            )
        )

        // Suggerisci tecniche avanzate per casi specifici
        if (currentWeight > 50f && currentReps > 10) {
            suggestions.add(
                ProgressionSuggestion(
                    type = SuggestionType.ADVANCED_TECHNIQUE,
                    description = "Considera tecniche avanzate come drop set o rest-pause",
                    newWeight = currentWeight,
                    newReps = currentReps,
                    confidence = 0.7f
                )
            )
        }

        return suggestions.sortedByDescending { it.confidence }
    }

    /**
     * Calcola la confidenza per l'aumento di peso
     */
    private fun calculateWeightIncreaseConfidence(
        currentWeight: Float,
        history: List<CompletedSeries>
    ): Float {
        // Logica semplificata: più alto è il peso attuale rispetto alla storia,
        // meno confidenza abbiamo nell'aumentare ulteriormente
        val maxHistoricWeight = history.maxOfOrNull { it.peso } ?: currentWeight

        return when {
            currentWeight <= maxHistoricWeight * 0.8f -> 0.9f
            currentWeight <= maxHistoricWeight * 0.95f -> 0.7f
            currentWeight >= maxHistoricWeight -> 0.5f
            else -> 0.6f
        }
    }

    /**
     * Calcola la confidenza per l'aumento delle ripetizioni
     */
    private fun calculateRepsIncreaseConfidence(
        currentReps: Int,
        history: List<CompletedSeries>
    ): Float {
        val maxHistoricReps = history.maxOfOrNull { it.ripetizioni } ?: currentReps

        return when {
            currentReps <= maxHistoricReps * 0.8 -> 0.8f
            currentReps <= maxHistoricReps -> 0.6f
            currentReps > maxHistoricReps -> 0.4f
            else -> 0.5f
        }
    }
}

/**
 * Informazioni su un plateau rilevato
 */
data class PlateauInfo(
    val exerciseId: Int,
    val plateauType: PlateauType,
    val sessionsInPlateau: Int,
    val currentWeight: Float,
    val currentReps: Int,
    val suggestions: List<ProgressionSuggestion>
)

/**
 * Tipi di plateau
 */
enum class PlateauType {
    LIGHT_WEIGHT,    // Peso leggero
    HEAVY_WEIGHT,    // Peso pesante
    LOW_REPS,        // Poche ripetizioni
    HIGH_REPS,       // Molte ripetizioni
    MODERATE         // Valori moderati
}

/**
 * Media dei valori di una sessione
 */
private data class SessionAverage(
    val avgWeight: Float,
    val avgReps: Int,
    val seriesCount: Int
)

/**
 * Suggerimento per la progressione
 */
data class ProgressionSuggestion(
    val type: SuggestionType,
    val description: String,
    val newWeight: Float,
    val newReps: Int,
    val confidence: Float // 0.0 - 1.0
)

/**
 * Tipi di suggerimenti
 */
enum class SuggestionType {
    INCREASE_WEIGHT,      // Aumenta il peso
    INCREASE_REPS,        // Aumenta le ripetizioni
    ADVANCED_TECHNIQUE,   // Usa tecniche avanzate
    REDUCE_REST,          // Riduci il tempo di recupero
    CHANGE_TEMPO          // Cambia il tempo di esecuzione
}