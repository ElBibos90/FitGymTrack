package com.fitgymtrack.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.*
import com.fitgymtrack.app.repository.ActiveWorkoutRepository
import com.fitgymtrack.app.utils.PlateauDetector
import com.fitgymtrack.app.utils.PlateauInfo
import com.fitgymtrack.app.utils.ProgressionSuggestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Date
import kotlin.math.max

class ActiveWorkoutViewModel : ViewModel() {
    private val repository = ActiveWorkoutRepository()

    // Stato dell'allenamento
    private val _workoutState = MutableStateFlow<ActiveWorkoutState>(ActiveWorkoutState.Idle)
    val workoutState: StateFlow<ActiveWorkoutState> = _workoutState.asStateFlow()

    // Stato delle serie completate
    private val _seriesState = MutableStateFlow<CompletedSeriesState>(CompletedSeriesState.Idle)
    val seriesState: StateFlow<CompletedSeriesState> = _seriesState.asStateFlow()

    // Stato del salvataggio di una serie
    private val _saveSeriesState = MutableStateFlow<SaveSeriesState>(SaveSeriesState.Idle)
    val saveSeriesState: StateFlow<SaveSeriesState> = _saveSeriesState.asStateFlow()

    // Stato del completamento dell'allenamento
    private val _completeWorkoutState = MutableStateFlow<CompleteWorkoutState>(CompleteWorkoutState.Idle)
    val completeWorkoutState: StateFlow<CompleteWorkoutState> = _completeWorkoutState.asStateFlow()

    // Tempo trascorso in minuti
    private val _elapsedTime = MutableStateFlow(0)
    val elapsedTime: StateFlow<Int> = _elapsedTime.asStateFlow()

    // Stato di completamento dell'allenamento
    private val _workoutCompleted = MutableStateFlow(false)
    val workoutCompleted: StateFlow<Boolean> = _workoutCompleted.asStateFlow()

    // Stato del timer di recupero
    private val _recoveryTime = MutableStateFlow(0)
    val recoveryTime: StateFlow<Int> = _recoveryTime.asStateFlow()

    // Stato del timer attivo
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    // ID dell'esercizio attualmente in recupero
    private val _currentRecoveryExerciseId = MutableStateFlow<Int?>(null)
    val currentRecoveryExerciseId: StateFlow<Int?> = _currentRecoveryExerciseId.asStateFlow()

    // StateFlow per memorizzare i dati storici dell'ultimo allenamento
    private val _historicWorkoutData = MutableStateFlow<Map<Int, List<CompletedSeries>>>(emptyMap())
    val historicWorkoutData: StateFlow<Map<Int, List<CompletedSeries>>> = _historicWorkoutData.asStateFlow()

    // ID del gruppo di esercizi corrente per superset/circuit
    private val _currentExerciseGroupId = MutableStateFlow<String?>(null)
    val currentExerciseGroupId: StateFlow<String?> = _currentExerciseGroupId.asStateFlow()

    // ID dell'esercizio selezionato in un superset/circuit
    private val _currentSelectedExerciseId = MutableStateFlow<Int?>(null)
    val currentSelectedExerciseId: StateFlow<Int?> = _currentSelectedExerciseId.asStateFlow()

    // Memorizza i valori di peso e ripetizioni per esercizio
    private val _exerciseValues = MutableStateFlow<Map<Int, Pair<Float, Int>>>(emptyMap())
    val exerciseValues: StateFlow<Map<Int, Pair<Float, Int>>> = _exerciseValues.asStateFlow()

    // NUOVO: Stato per i plateau rilevati
    private val _plateauInfo = MutableStateFlow<Map<Int, PlateauInfo>>(emptyMap())
    val plateauInfo: StateFlow<Map<Int, PlateauInfo>> = _plateauInfo.asStateFlow()

    // NUOVO: Stato per plateau dismissati dall'utente (per non mostrarli più)
    private val _dismissedPlateaus = MutableStateFlow<Set<Int>>(emptySet())
    val dismissedPlateaus: StateFlow<Set<Int>> = _dismissedPlateaus.asStateFlow()

    // Data e ora di inizio dell'allenamento
    private var sessionStartTime: Long = 0

    // ID dell'allenamento corrente
    private var allenamentoId: Int? = null

    // ID dell'utente corrente
    private var userId: Int? = null

    // Set per tenere traccia delle serie già salvate ed evitare duplicati
    private val savedSeriesIds = mutableSetOf<String>()

    /**
     * Inizializza un nuovo allenamento
     */
    fun initializeWorkout(userId: Int, schedaId: Int) {
        if (allenamentoId != null) {
            // Se l'allenamento è già inizializzato, carica solo gli esercizi
            loadWorkoutExercises(schedaId)
            return
        }

        this.userId = userId
        _workoutState.value = ActiveWorkoutState.Loading

        viewModelScope.launch {
            try {
                val result = repository.startWorkout(userId, schedaId)

                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            allenamentoId = response.allenamentoId
                            sessionStartTime = System.currentTimeMillis()

                            // Ora carica gli esercizi
                            loadWorkoutExercises(schedaId)

                            // Avvia il timer per tracciare il tempo trascorso
                            startElapsedTimeTracking()

                            // Carica le serie già completate (se ce ne sono)
                            loadCompletedSeries()
                        } else {
                            _workoutState.value = ActiveWorkoutState.Error(response.message)
                        }
                    },
                    onFailure = { e ->
                        _workoutState.value = ActiveWorkoutState.Error(
                            e.message ?: "Errore nell'inizializzazione dell'allenamento"
                        )
                    }
                )
            } catch (e: Exception) {
                _workoutState.value = ActiveWorkoutState.Error(
                    e.message ?: "Errore nell'inizializzazione dell'allenamento"
                )
            }
        }
    }

    /**
     * Carica gli esercizi di una scheda
     */
    private fun loadWorkoutExercises(schedaId: Int) {
        viewModelScope.launch {
            try {
                Log.d("WorkoutHistory", "Inizio caricamento esercizi per scheda $schedaId")
                val result = repository.getWorkoutExercises(schedaId)

                result.fold(
                    onSuccess = { exercises ->
                        Log.d("WorkoutHistory", "Esercizi caricati con successo: ${exercises.size} esercizi")

                        // Crea un ActiveWorkout temporaneo con i dati disponibili
                        val workout = ActiveWorkout(
                            id = allenamentoId ?: 0,
                            schedaId = schedaId,
                            dataAllenamento = Date().toString(),
                            userId = userId ?: 0,
                            esercizi = exercises
                        )

                        _workoutState.value = ActiveWorkoutState.Success(workout)

                        // Pre-carica i valori di peso e ripetizioni dai default
                        preloadDefaultValues()

                        // Carica lo storico DOPO aver impostato gli esercizi
                        userId?.let {
                            Log.d("WorkoutHistory", "Caricamento storico dopo aver impostato gli esercizi")
                            loadLastWorkoutHistory(it, schedaId)
                        }

                        // NUOVO: Controlla i plateau dopo aver caricato tutto
                        delay(1000)
                        checkForPlateaus(exercises)
                    },
                    onFailure = { e ->
                        Log.e("WorkoutHistory", "Errore nel caricamento degli esercizi: ${e.message}")
                        _workoutState.value = ActiveWorkoutState.Error(
                            e.message ?: "Errore nel caricamento degli esercizi"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("WorkoutHistory", "Eccezione nel caricamento degli esercizi: ${e.message}")
                _workoutState.value = ActiveWorkoutState.Error(
                    e.message ?: "Errore nel caricamento degli esercizi"
                )
            }
        }
    }

    /**
     * NUOVO: Controlla i plateau per tutti gli esercizi
     */
    private fun checkForPlateaus(exercises: List<WorkoutExercise>) {
        val currentPlateaus = mutableMapOf<Int, PlateauInfo>()
        val historicData = _historicWorkoutData.value
        val currentValues = _exerciseValues.value
        val dismissedSet = _dismissedPlateaus.value

        Log.d("PlateauCheck", "🔍 === INIZIO CONTROLLO PLATEAU ===")
        Log.d("PlateauCheck", "Controllo plateau per ${exercises.size} esercizi")
        Log.d("PlateauCheck", "Dati storici disponibili per ${historicData.size} esercizi: ${historicData.keys}")
        Log.d("PlateauCheck", "Esercizi dismissi: $dismissedSet")

        exercises.forEach { exercise ->
            Log.d("PlateauCheck", "--- Controllo esercizio ${exercise.id} (${exercise.nome}) ---")

            // Salta esercizi già dismissati
            if (exercise.id in dismissedSet) {
                Log.d("PlateauCheck", "❌ Esercizio ${exercise.id} è stato dismisso")
                return@forEach
            }

            val exerciseValues = currentValues[exercise.id]
            val currentWeight = exerciseValues?.first ?: exercise.peso.toFloat()
            val currentReps = exerciseValues?.second ?: exercise.ripetizioni

            Log.d("PlateauCheck", "📊 Esercizio ${exercise.id}: peso=$currentWeight, reps=$currentReps")
            Log.d("PlateauCheck", "📈 Dati storici per esercizio ${exercise.id}: ${historicData[exercise.id]?.size ?: 0} serie")

            // Log dei dati storici se disponibili
            historicData[exercise.id]?.let { history ->
                Log.d("PlateauCheck", "🗂️ Storia esercizio ${exercise.id}:")
                history.take(5).forEach { series ->  // Mostra solo le prime 5 per non intasare i log
                    Log.d("PlateauCheck", "   - Serie ${series.serieNumber}: ${series.peso}kg x ${series.ripetizioni} reps (${series.timestamp})")
                }
                if (history.size > 5) {
                    Log.d("PlateauCheck", "   ... e altre ${history.size - 5} serie")
                }
            }

            val plateau = PlateauDetector.detectPlateau(
                exerciseId = exercise.id,
                currentWeight = currentWeight,
                currentReps = currentReps,
                historicData = historicData,
                minSessionsForPlateau = 2
            )

            if (plateau != null) {
                Log.d("PlateauCheck", "🚨 PLATEAU RILEVATO per esercizio ${exercise.id}!")
                currentPlateaus[exercise.id] = plateau
            } else {
                Log.d("PlateauCheck", "✅ Nessun plateau per esercizio ${exercise.id}")
            }
        }

        _plateauInfo.value = currentPlateaus

        Log.d("PlateauCheck", "🏁 === FINE CONTROLLO PLATEAU ===")
        Log.d("PlateauCheck", "Totale plateau rilevati: ${currentPlateaus.size}")
        if (currentPlateaus.isNotEmpty()) {
            Log.d("PlateauCheck", "Plateau rilevati per esercizi: ${currentPlateaus.keys}")
        }
    }

    /**
     * NUOVO: Applica un suggerimento di progressione
     */
    fun applyProgressionSuggestion(exerciseId: Int, suggestion: ProgressionSuggestion) {
        Log.d("PlateauProgression", "Applicando suggerimento per esercizio $exerciseId: ${suggestion.description}")

        // Aggiorna i valori dell'esercizio con i nuovi valori suggeriti
        val currentValues = _exerciseValues.value.toMutableMap()
        currentValues[exerciseId] = Pair(suggestion.newWeight, suggestion.newReps)
        _exerciseValues.value = currentValues

        // Rimuovi il plateau per questo esercizio (verrà ri-rilevato se necessario)
        val currentPlateaus = _plateauInfo.value.toMutableMap()
        currentPlateaus.remove(exerciseId)
        _plateauInfo.value = currentPlateaus

        Log.d("PlateauProgression", "Nuovi valori applicati: peso=${suggestion.newWeight}, reps=${suggestion.newReps}")
    }

    /**
     * NUOVO: Dismissi un plateau (l'utente sceglie di ignorarlo)
     */
    fun dismissPlateau(exerciseId: Int) {
        Log.d("PlateauCheck", "Plateau dismisso per esercizio $exerciseId")

        // Aggiungi alla lista dei dismissati
        val currentDismissed = _dismissedPlateaus.value.toMutableSet()
        currentDismissed.add(exerciseId)
        _dismissedPlateaus.value = currentDismissed

        // Rimuovi dai plateau attivi
        val currentPlateaus = _plateauInfo.value.toMutableMap()
        currentPlateaus.remove(exerciseId)
        _plateauInfo.value = currentPlateaus
    }

    /**
     * NUOVO: Resetta i plateau dismissati (per ricominciarli a controllare)
     */
    fun resetDismissedPlateaus() {
        _dismissedPlateaus.value = emptySet()
    }

    /**
     * NUOVO: Forza il controllo dei plateau (per testing)
     */
    fun forceCheckPlateaus() {
        Log.d("PlateauCheck", "🔧 FORZATO controllo plateau per testing")
        val currentState = _workoutState.value
        if (currentState is ActiveWorkoutState.Success) {
            checkForPlateaus(currentState.workout.esercizi)
        }
    }

    /**
     * NUOVO: Controlla se un exercizio specifico è in plateau
     */
    fun isExerciseInPlateau(exerciseId: Int): Boolean {
        return _plateauInfo.value.containsKey(exerciseId)
    }

    /**
     * NUOVO: Ottieni informazioni plateau per un esercizio specifico
     */
    fun getPlateauInfoForExercise(exerciseId: Int): PlateauInfo? {
        return _plateauInfo.value[exerciseId]
    }

    /**
     * Carica lo storico dell'ultimo allenamento per inizializzare i valori
     */
    private fun loadLastWorkoutHistory(userId: Int, schedaId: Int) {
        Log.d("WorkoutHistory", "Caricamento storico allenamento precedente per userId=$userId, schedaId=$schedaId")

        viewModelScope.launch {
            try {
                val workoutHistoryApiService = ApiClient.workoutHistoryApiService
                val allWorkoutsResponse = workoutHistoryApiService.getWorkoutHistory(userId)
                Log.d("WorkoutHistory", "RISPOSTA API getWorkoutHistory: success=${allWorkoutsResponse["success"]}, " +
                        "count=${allWorkoutsResponse["count"]}")

                val success = allWorkoutsResponse["success"] as? Boolean ?: false
                val allenamenti = allWorkoutsResponse["allenamenti"] as? List<Map<String, Any>> ?: emptyList()

                if (!success || allenamenti.isEmpty()) {
                    Log.d("WorkoutHistory", "Nessun allenamento precedente trovato")
                    return@launch
                }

                Log.d("WorkoutHistory", "Trovati ${allenamenti.size} allenamenti precedenti per l'utente")

                val currentExercises = when (val state = _workoutState.value) {
                    is ActiveWorkoutState.Success -> state.workout.esercizi
                    else -> emptyList()
                }

                val exerciseIds = currentExercises.map { it.id }.toSet()
                Log.d("WorkoutHistory", "Esercizi nella scheda corrente: $exerciseIds")

                val sameSchemaWorkouts = allenamenti.filter {
                    val schemaId = it["scheda_id"]?.toString()?.toDoubleOrNull()?.toInt()
                    schemaId == schedaId
                }

                Log.d("WorkoutHistory", "Allenamenti con scheda $schedaId: ${sameSchemaWorkouts.size}")

                val sortedWorkouts = sameSchemaWorkouts.sortedByDescending {
                    it["data_allenamento"]?.toString() ?: ""
                }

                val allHistoricData = mutableMapOf<Int, MutableList<CompletedSeries>>()
                val exercisesWithHistory = mutableSetOf<Int>()

                for (workout in sortedWorkouts) {
                    val workoutIdValue = workout["id"]
                    val workoutId = when (workoutIdValue) {
                        is Double -> workoutIdValue.toInt()
                        is Float -> workoutIdValue.toInt()
                        is Int -> workoutIdValue
                        is String -> workoutIdValue.toDoubleOrNull()?.toInt()
                        else -> null
                    }

                    if (workoutId == null) {
                        Log.e("WorkoutHistory", "ID allenamento non valido: $workoutIdValue")
                        continue
                    }

                    if (exercisesWithHistory.containsAll(exerciseIds)) {
                        Log.d("WorkoutHistory", "Già trovati dati per tutti gli esercizi")
                        break
                    }

                    try {
                        val seriesResponse = workoutHistoryApiService.getWorkoutSeriesDetail(workoutId)

                        if (seriesResponse.success && seriesResponse.serie.isNotEmpty()) {
                            val relevantSeries = seriesResponse.serie.filter { serie ->
                                val exerciseId = serie.schedaEsercizioId
                                exerciseId in exerciseIds && exerciseId !in exercisesWithHistory
                            }

                            if (relevantSeries.isNotEmpty()) {
                                val seriesByExercise = relevantSeries.groupBy { it.schedaEsercizioId }

                                seriesByExercise.forEach { (exerciseId, series) ->
                                    val completedSeries = mutableListOf<CompletedSeries>()

                                    series.forEach { serie ->
                                        val serieNumber = serie.realSerieNumber ?:
                                        (serie.serieNumber?.rem(100)) ?:
                                        (completedSeries.size + 1)

                                        val completed = CompletedSeries(
                                            id = serie.id,
                                            serieNumber = serieNumber,
                                            peso = serie.peso,
                                            ripetizioni = serie.ripetizioni,
                                            tempoRecupero = serie.tempoRecupero ?: 60,
                                            timestamp = serie.timestamp,
                                            note = serie.note
                                        )

                                        completedSeries.add(completed)
                                    }

                                    if (!allHistoricData.containsKey(exerciseId)) {
                                        allHistoricData[exerciseId] = completedSeries.sortedBy { it.serieNumber }.toMutableList()
                                        exercisesWithHistory.add(exerciseId)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WorkoutHistory", "Errore nell'allenamento $workoutId: ${e.message}")
                    }
                }

                if (allHistoricData.isNotEmpty()) {
                    _historicWorkoutData.value = allHistoricData
                    preloadExerciseValues(currentExercises)

                    // NUOVO: Dopo aver caricato lo storico, controlla i plateau
                    delay(500)
                    checkForPlateaus(currentExercises)
                } else {
                    preloadExerciseValues(currentExercises)
                }

            } catch (e: Exception) {
                Log.e("WorkoutHistory", "Errore generale: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Pre-carica i valori di peso e ripetizioni per tutti gli esercizi
     */
    private fun preloadExerciseValues(exercises: List<WorkoutExercise>) {
        val valueMap = mutableMapOf<Int, Pair<Float, Int>>()

        exercises.forEach { exercise ->
            val initialValues = getInitialValues(exercise.id, 0)
            valueMap[exercise.id] = initialValues

            Log.d("WorkoutHistory", "Precaricati valori per esercizio ${exercise.id}: " +
                    "peso=${initialValues.first}, rip=${initialValues.second}")
        }

        _exerciseValues.value = valueMap
    }

    /**
     * Precarica i valori di default quando non ci sono dati storici
     */
    private fun preloadDefaultValues() {
        val exercises = when (val state = _workoutState.value) {
            is ActiveWorkoutState.Success -> state.workout.esercizi
            else -> emptyList()
        }

        val valueMap = mutableMapOf<Int, Pair<Float, Int>>()

        exercises.forEach { exercise ->
            val defaultWeight = exercise.peso.toFloat()
            val defaultReps = exercise.ripetizioni

            valueMap[exercise.id] = Pair(defaultWeight, defaultReps)

            Log.d("WorkoutHistory", "Precaricati valori DEFAULT per esercizio ${exercise.id}: " +
                    "peso=$defaultWeight, rip=$defaultReps")
        }

        _exerciseValues.value = valueMap
    }

    /**
     * Ottiene i valori iniziali per un esercizio basandosi sullo storico o sui valori di default
     */
    fun getInitialValues(exerciseId: Int, seriesIndex: Int): Pair<Float, Int> {
        val currentState = _workoutState.value
        val seriesState = _seriesState.value
        val historicData = _historicWorkoutData.value

        if (currentState !is ActiveWorkoutState.Success) {
            return Pair(0f, 0)
        }

        val workout = currentState.workout
        val exercise = workout.esercizi.find { it.id == exerciseId } ?: return Pair(0f, 0)

        val currentSerieNumber = seriesIndex + 1

        Log.d("WorkoutHistory", "getInitialValues: esercizio=$exerciseId, serie=$currentSerieNumber")

        // 1. Prima verifica i dati storici
        if (historicData.isNotEmpty() && historicData.containsKey(exerciseId)) {
            val historicSeries = historicData[exerciseId] ?: emptyList()

            if (historicSeries.isNotEmpty()) {
                val historicSeriesWithSameNumber = historicSeries.firstOrNull {
                    it.serieNumber == currentSerieNumber
                }

                if (historicSeriesWithSameNumber != null) {
                    Log.d("WorkoutHistory", "Trovata serie storica $currentSerieNumber")
                    return Pair(historicSeriesWithSameNumber.peso, historicSeriesWithSameNumber.ripetizioni)
                }

                if (currentSerieNumber > historicSeries.maxOfOrNull { it.serieNumber } ?: 0) {
                    val lastHistoricSeries = historicSeries.maxByOrNull { it.serieNumber }
                    if (lastHistoricSeries != null) {
                        Log.d("WorkoutHistory", "Usando ultima serie storica disponibile")
                        return Pair(lastHistoricSeries.peso, lastHistoricSeries.ripetizioni)
                    }
                }
            }
        }

        // 2. Serie già completate nell'allenamento corrente
        if (seriesState is CompletedSeriesState.Success) {
            val completedSeries = seriesState.series[exerciseId] ?: emptyList()

            if (completedSeries.isNotEmpty()) {
                val seriesWithSameNumber = completedSeries.firstOrNull { it.serieNumber == currentSerieNumber }

                if (seriesWithSameNumber != null) {
                    return Pair(seriesWithSameNumber.peso, seriesWithSameNumber.ripetizioni)
                }

                val previousSerieNumber = currentSerieNumber - 1
                val previousCompletedSeries = completedSeries.firstOrNull { it.serieNumber == previousSerieNumber }

                if (previousCompletedSeries != null) {
                    return Pair(previousCompletedSeries.peso, previousCompletedSeries.ripetizioni)
                }

                val lastCompletedSeries = completedSeries.maxByOrNull { it.serieNumber }
                if (lastCompletedSeries != null) {
                    return Pair(lastCompletedSeries.peso, lastCompletedSeries.ripetizioni)
                }
            }
        }

        // 3. Valori di default dell'esercizio
        val defaultWeight = exercise.peso.toFloat()
        val defaultReps = exercise.ripetizioni

        Log.d("WorkoutHistory", "Usando valori default: peso=$defaultWeight, rip=$defaultReps")
        return Pair(defaultWeight, defaultReps)
    }

    /**
     * Salva i valori correnti di peso e ripetizioni per un esercizio
     */
    fun saveExerciseValues(exerciseId: Int, weight: Float, reps: Int) {
        val currentValues = _exerciseValues.value.toMutableMap()
        currentValues[exerciseId] = Pair(weight, reps)
        _exerciseValues.value = currentValues

        Log.d("WorkoutHistory", "Salvati nuovi valori per esercizio $exerciseId: peso=$weight, rip=$reps")
    }

    /**
     * Avvia il tracking del tempo trascorso
     */
    private fun startElapsedTimeTracking() {
        viewModelScope.launch {
            while (!_workoutCompleted.value) {
                val elapsedMillis = System.currentTimeMillis() - sessionStartTime
                _elapsedTime.value = (elapsedMillis / 1000).toInt()
                delay(1000)
            }
        }
    }

    /**
     * Carica le serie già completate per l'allenamento corrente
     */
    private fun loadCompletedSeries() {
        val currentAllenamentoId = allenamentoId ?: return

        _seriesState.value = CompletedSeriesState.Loading

        viewModelScope.launch {
            try {
                val result = repository.getCompletedSeries(currentAllenamentoId)

                result.fold(
                    onSuccess = { seriesDataList ->
                        val seriesMap = mutableMapOf<Int, MutableList<CompletedSeries>>()

                        Log.d("WorkoutHistory", "Serie nell'allenamento corrente: ${seriesDataList.size}")

                        seriesDataList.forEach { seriesData ->
                            val exId = seriesData.esercizioId ?: 0

                            if (!seriesMap.containsKey(exId)) {
                                seriesMap[exId] = mutableListOf()
                            }

                            val completedSeries = CompletedSeries(
                                id = seriesData.id,
                                serieNumber = seriesData.realSerieNumber ?: seriesMap[exId]!!.size + 1,
                                peso = seriesData.peso,
                                ripetizioni = seriesData.ripetizioni,
                                tempoRecupero = seriesData.tempoRecupero ?: 60,
                                timestamp = seriesData.timestamp,
                                note = seriesData.note
                            )

                            seriesMap[exId]!!.add(completedSeries)
                            savedSeriesIds.add(seriesData.id)
                        }

                        _seriesState.value = CompletedSeriesState.Success(seriesMap)
                    },
                    onFailure = { e ->
                        if (e.message?.contains("404") == true) {
                            _seriesState.value = CompletedSeriesState.Success(emptyMap())
                        } else {
                            _seriesState.value = CompletedSeriesState.Error(
                                e.message ?: "Errore nel caricamento delle serie completate"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _seriesState.value = CompletedSeriesState.Error(
                    e.message ?: "Errore nel caricamento delle serie completate"
                )
            }
        }
    }

    /**
     * Seleziona un esercizio in un superset o circuito
     */
    fun selectExercise(exerciseId: Int) {
        _currentSelectedExerciseId.value = exerciseId
    }

    /**
     * Trova il prossimo esercizio in un superset
     */
    fun findNextExerciseInSuperset(currentExerciseId: Int): Int? {
        val currentState = _workoutState.value

        if (currentState !is ActiveWorkoutState.Success) {
            return null
        }

        val workout = currentState.workout
        val currentExercise = workout.esercizi.find { it.id == currentExerciseId } ?: return null

        val isInGroup = currentExercise.setType == "superset" || currentExercise.setType == "circuit"

        if (!isInGroup) {
            return null
        }

        val supersetExercises = findExercisesInSameSuperset(workout.esercizi, currentExerciseId)
        val currentIndex = supersetExercises.indexOfFirst { it.id == currentExerciseId }

        if (currentIndex == -1 || currentIndex >= supersetExercises.size - 1) {
            return supersetExercises.firstOrNull()?.id
        }

        return supersetExercises[currentIndex + 1].id
    }

    /**
     * Trova tutti gli esercizi in uno stesso superset
     */
    private fun findExercisesInSameSuperset(
        allExercises: List<WorkoutExercise>,
        startExerciseId: Int
    ): List<WorkoutExercise> {
        val result = mutableListOf<WorkoutExercise>()

        val startExercise = allExercises.find { it.id == startExerciseId } ?: return result
        val startIndex = allExercises.indexOfFirst { it.id == startExerciseId }

        val isSuperset = startExercise.setType == "superset" || startExercise.setType == "circuit"

        if (!isSuperset) {
            result.add(startExercise)
            return result
        }

        var supersetStartIndex = startIndex
        while (supersetStartIndex > 0 &&
            allExercises[supersetStartIndex - 1].setType == startExercise.setType &&
            allExercises[supersetStartIndex].linkedToPrevious) {
            supersetStartIndex--
        }

        result.add(allExercises[supersetStartIndex])
        var currentIndex = supersetStartIndex + 1

        while (currentIndex < allExercises.size &&
            allExercises[currentIndex].setType == startExercise.setType &&
            allExercises[currentIndex].linkedToPrevious) {
            result.add(allExercises[currentIndex])
            currentIndex++
        }

        return result
    }

    /**
     * Controlla se tutti gli esercizi in un superset hanno completato tutte le serie
     */
    fun isAllSupersetExercisesCompleted(supersetExercises: List<WorkoutExercise>): Boolean {
        val seriesState = _seriesState.value

        if (seriesState !is CompletedSeriesState.Success) {
            return false
        }

        return supersetExercises.all { exercise ->
            val completedSeries = seriesState.series[exercise.id] ?: emptyList()
            completedSeries.size >= exercise.serie
        }
    }

    /**
     * Aggiornamento UI per una nuova serie
     */
    fun updateUIForNewSeries(exerciseId: Int, newSeriesIndex: Int) {
        Log.d("WorkoutHistory", "Aggiornamento UI per nuova serie: esercizio=$exerciseId, nuova serie=${newSeriesIndex + 1}")

        val initialValues = getInitialValues(exerciseId, newSeriesIndex)

        val currentValues = _exerciseValues.value.toMutableMap()
        currentValues[exerciseId] = initialValues
        _exerciseValues.value = currentValues

        Log.d("WorkoutHistory", "UI aggiornata per serie ${newSeriesIndex + 1}: peso=${initialValues.first}, rip=${initialValues.second}")
    }

    /**
     * Metodo per cambiare serie nel workout
     */
    fun switchToSeries(exerciseId: Int, seriesIndex: Int) {
        updateUIForNewSeries(exerciseId, seriesIndex)
    }

    /**
     * Aggiunge una serie completata
     */
    fun addCompletedSeries(
        exerciseId: Int,
        peso: Float,
        ripetizioni: Int,
        serieNumber: Int,
        tempoRecupero: Int = 60
    ) {
        val currentAllenamentoId = allenamentoId ?: return

        val seriesMap = when (val state = _seriesState.value) {
            is CompletedSeriesState.Success -> state.series
            else -> emptyMap()
        }

        val existingSeries = seriesMap[exerciseId] ?: emptyList()
        if (existingSeries.any { it.serieNumber == serieNumber }) {
            return
        }

        _saveSeriesState.value = SaveSeriesState.Loading

        viewModelScope.launch {
            try {
                val serieId = "serie_${System.currentTimeMillis()}_${serieNumber}_${UUID.randomUUID().toString().substring(0, 8)}"
                val requestId = "req_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
                val serieNumberEncoded = exerciseId * 100 + serieNumber

                Log.d("WorkoutHistory", "Salvataggio serie: esercizio=${exerciseId}, serie=${serieNumber}")

                val seriesData = SeriesData(
                    schedaEsercizioId = exerciseId,
                    peso = peso,
                    ripetizioni = ripetizioni,
                    completata = 1,
                    tempoRecupero = tempoRecupero,
                    note = null,
                    serieNumber = serieNumberEncoded,
                    serieId = serieId
                )

                val result = repository.saveCompletedSeries(
                    allenamentoId = currentAllenamentoId,
                    serie = listOf(seriesData),
                    requestId = requestId
                )

                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            savedSeriesIds.add(serieId)

                            updateCompletedSeriesState(
                                exerciseId = exerciseId,
                                newSeries = CompletedSeries(
                                    id = serieId,
                                    serieNumber = serieNumber,
                                    peso = peso,
                                    ripetizioni = ripetizioni,
                                    tempoRecupero = tempoRecupero,
                                    timestamp = Date().toString()
                                )
                            )

                            saveExerciseValues(exerciseId, peso, ripetizioni)

                            val workout = (workoutState.value as? ActiveWorkoutState.Success)?.workout
                            val exercise = workout?.esercizi?.find { it.id == exerciseId }

                            if (exercise != null && serieNumber < exercise.serie) {
                                val nextSeriesIndex = serieNumber
                                Log.d("WorkoutHistory", "Preparazione serie successiva: ${nextSeriesIndex}")
                                updateUIForNewSeries(exerciseId, nextSeriesIndex)
                            }

                            handleSupersetNavigation(exerciseId)
                            _currentRecoveryExerciseId.value = exerciseId
                            checkAndHandleExerciseGroup(exerciseId)

                            _saveSeriesState.value = SaveSeriesState.Success

                            // NUOVO: Re-controlla i plateau dopo aver aggiunto una serie
                            val currentState = _workoutState.value
                            if (currentState is ActiveWorkoutState.Success) {
                                delay(100)
                                checkForPlateaus(currentState.workout.esercizi)
                            }
                        } else {
                            _saveSeriesState.value = SaveSeriesState.Error(response.message)
                        }
                    },
                    onFailure = { e ->
                        _saveSeriesState.value = SaveSeriesState.Error(
                            e.message ?: "Errore nel salvataggio della serie"
                        )
                    }
                )
            } catch (e: Exception) {
                _saveSeriesState.value = SaveSeriesState.Error(
                    e.message ?: "Errore nel salvataggio della serie"
                )
            }
        }
    }

    /**
     * Gestisce la navigazione automatica nei superset
     */
    private fun handleSupersetNavigation(exerciseId: Int) {
        val currentState = _workoutState.value

        if (currentState !is ActiveWorkoutState.Success) {
            return
        }

        val workout = currentState.workout
        val currentExercise = workout.esercizi.find { it.id == exerciseId } ?: return

        val isInGroup = currentExercise.setType == "superset" || currentExercise.setType == "circuit"

        if (!isInGroup) {
            return
        }

        val supersetExercises = findExercisesInSameSuperset(workout.esercizi, exerciseId)
        val currentIndex = supersetExercises.indexOfFirst { it.id == exerciseId }

        if (currentIndex == supersetExercises.size - 1) {
            val allExercisesCompleted = isAllSupersetExercisesCompleted(supersetExercises)

            if (allExercisesCompleted) {
                return
            }

            val firstExerciseId = supersetExercises.firstOrNull()?.id ?: return

            viewModelScope.launch {
                val recoveryTime = currentExercise.tempoRecupero
                if (recoveryTime > 0) {
                    startRecoveryTimer(recoveryTime)
                }

                selectExercise(firstExerciseId)
            }
        } else {
            val nextExerciseId = supersetExercises.getOrNull(currentIndex + 1)?.id ?: return
            selectExercise(nextExerciseId)
        }
    }

    /**
     * Verifica se l'esercizio appartiene a un gruppo e gestisce il timer
     */
    private fun checkAndHandleExerciseGroup(exerciseId: Int) {
        val workout = when (val state = _workoutState.value) {
            is ActiveWorkoutState.Success -> state.workout
            else -> return
        }

        val currentExercise = workout.esercizi.find { it.id == exerciseId } ?: return
        val currentExerciseIndex = workout.esercizi.indexOfFirst { it.id == exerciseId }

        val setType = currentExercise.setType

        when (setType) {
            "superset", "circuit" -> {
                if (_currentExerciseGroupId.value == null) {
                    _currentExerciseGroupId.value = "group_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
                }

                val groupExercises = findExercisesInSameGroup(workout.esercizi, currentExerciseIndex)
                val groupIndex = groupExercises.indexOfFirst { it.id == exerciseId }

                if (groupIndex < groupExercises.size - 1) {
                    return
                }

                val tempoRecupero = currentExercise.tempoRecupero
                if (tempoRecupero > 0) {
                    startRecoveryTimer(tempoRecupero)
                }

                _currentExerciseGroupId.value = null
            }
            else -> {
                val tempoRecupero = currentExercise.tempoRecupero
                if (tempoRecupero > 0) {
                    startRecoveryTimer(tempoRecupero)
                }
            }
        }
    }

    /**
     * Trova tutti gli esercizi che appartengono allo stesso gruppo
     */
    private fun findExercisesInSameGroup(
        allExercises: List<WorkoutExercise>,
        startIndex: Int
    ): List<WorkoutExercise> {
        val result = mutableListOf<WorkoutExercise>()

        if (startIndex < 0 || startIndex >= allExercises.size) {
            return result
        }

        val startExercise = allExercises[startIndex]
        val setType = startExercise.setType

        if (setType == "normal") {
            result.add(startExercise)
            return result
        }

        var groupStartIndex = startIndex
        while (groupStartIndex > 0 &&
            allExercises[groupStartIndex - 1].setType == setType &&
            allExercises[groupStartIndex].linkedToPrevious) {
            groupStartIndex--
        }

        result.add(allExercises[groupStartIndex])
        var currentIndex = groupStartIndex + 1

        while (currentIndex < allExercises.size &&
            allExercises[currentIndex].setType == setType &&
            allExercises[currentIndex].linkedToPrevious) {
            result.add(allExercises[currentIndex])
            currentIndex++
        }

        return result
    }

    /**
     * Aggiorna lo stato delle serie completate
     */
    private fun updateCompletedSeriesState(exerciseId: Int, newSeries: CompletedSeries) {
        val currentState = _seriesState.value

        if (currentState is CompletedSeriesState.Success) {
            val updatedMap = currentState.series.toMutableMap()
            val exerciseSeries = updatedMap[exerciseId]?.toMutableList() ?: mutableListOf()

            exerciseSeries.add(newSeries)
            updatedMap[exerciseId] = exerciseSeries

            _seriesState.value = CompletedSeriesState.Success(updatedMap)
        }
    }

    /**
     * Avvia il timer di recupero
     */
    private fun startRecoveryTimer(seconds: Int) {
        _recoveryTime.value = seconds
        _isTimerRunning.value = true

        viewModelScope.launch {
            var remainingSeconds = seconds

            while (remainingSeconds > 0 && _isTimerRunning.value) {
                delay(1000)
                remainingSeconds -= 1
                _recoveryTime.value = remainingSeconds
            }

            _isTimerRunning.value = false
            _currentRecoveryExerciseId.value = null
        }
    }

    /**
     * Interrompe il timer di recupero
     */
    fun stopRecoveryTimer() {
        _isTimerRunning.value = false
        _recoveryTime.value = 0
        _currentRecoveryExerciseId.value = null
    }

    /**
     * Marca l'allenamento come completato
     */
    fun markWorkoutAsCompleted() {
        _workoutCompleted.value = true
    }

    /**
     * Completa l'allenamento
     */
    fun completeWorkout(note: String? = null) {
        val currentAllenamentoId = allenamentoId ?: return
        val durataTotaleMinuti = _elapsedTime.value / 60

        _completeWorkoutState.value = CompleteWorkoutState.Loading

        viewModelScope.launch {
            try {
                val result = repository.completeWorkout(
                    allenamentoId = currentAllenamentoId,
                    durataTotale = durataTotaleMinuti,
                    note = note
                )

                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            _completeWorkoutState.value = CompleteWorkoutState.Success
                        } else {
                            _completeWorkoutState.value = CompleteWorkoutState.Error(response.message)
                        }
                    },
                    onFailure = { e ->
                        _completeWorkoutState.value = CompleteWorkoutState.Error(
                            e.message ?: "Errore nel completamento dell'allenamento"
                        )
                    }
                )
            } catch (e: Exception) {
                _completeWorkoutState.value = CompleteWorkoutState.Error(
                    e.message ?: "Errore nel completamento dell'allenamento"
                )
            }
        }
    }

    /**
     * Cancella l'allenamento corrente
     */
    fun cancelWorkout() {
        val currentAllenamentoId = allenamentoId ?: return

        viewModelScope.launch {
            try {
                repository.deleteWorkout(currentAllenamentoId)
                resetWorkoutState()
            } catch (e: Exception) {
                resetWorkoutState()
            }
        }
    }

    /**
     * Resetta lo stato dell'allenamento
     */
    private fun resetWorkoutState() {
        allenamentoId = null
        sessionStartTime = 0
        _workoutState.value = ActiveWorkoutState.Idle
        _seriesState.value = CompletedSeriesState.Idle
        _saveSeriesState.value = SaveSeriesState.Idle
        _completeWorkoutState.value = CompleteWorkoutState.Idle
        _elapsedTime.value = 0
        _workoutCompleted.value = false
        _recoveryTime.value = 0
        _isTimerRunning.value = false
        _currentRecoveryExerciseId.value = null
        _currentExerciseGroupId.value = null
        _currentSelectedExerciseId.value = null
        _exerciseValues.value = emptyMap()

        // NUOVO: Reset dei plateau
        _plateauInfo.value = emptyMap()
        _dismissedPlateaus.value = emptySet()

        savedSeriesIds.clear()
    }

    /**
     * Formatta il tempo trascorso
     */
    fun getFormattedElapsedTime(): String {
        val totalSeconds = _elapsedTime.value
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Formatta il tempo di recupero
     */
    fun getFormattedRecoveryTime(): String {
        val seconds = _recoveryTime.value
        val minutes = seconds / 60
        val secs = seconds % 60

        return "${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    }

    /**
     * Controlla se un esercizio è completato
     */
    fun isExerciseCompleted(exerciseId: Int, targetSeries: Int): Boolean {
        val currentState = _seriesState.value

        if (currentState is CompletedSeriesState.Success) {
            val exerciseSeries = currentState.series[exerciseId] ?: emptyList()
            return exerciseSeries.size >= targetSeries
        }

        return false
    }

    /**
     * Raggruppa gli esercizi per tipo
     */
    fun groupExercisesByType(): List<List<WorkoutExercise>> {
        val workout = when (val state = _workoutState.value) {
            is ActiveWorkoutState.Success -> state.workout
            else -> return emptyList()
        }

        val result = mutableListOf<List<WorkoutExercise>>()
        var currentGroup = mutableListOf<WorkoutExercise>()

        workout.esercizi.forEachIndexed { index, exercise ->
            if (index == 0 || !exercise.linkedToPrevious) {
                if (currentGroup.isNotEmpty()) {
                    result.add(currentGroup.toList())
                }
                currentGroup = mutableListOf(exercise)
            } else {
                currentGroup.add(exercise)
            }
        }

        if (currentGroup.isNotEmpty()) {
            result.add(currentGroup.toList())
        }

        return result
    }

    /**
     * Calcola il progresso dell'allenamento
     */
    fun calculateWorkoutProgress(): Float {
        val currentState = _workoutState.value

        if (currentState is ActiveWorkoutState.Success) {
            val workout = currentState.workout
            val exerciseCount = workout.esercizi.size

            if (exerciseCount == 0) return 0f

            var completedCount = 0

            workout.esercizi.forEach { exercise ->
                if (isExerciseCompleted(exercise.id, exercise.serie)) {
                    completedCount++
                }
            }

            return completedCount.toFloat() / exerciseCount.toFloat()
        }

        return 0f
    }
}