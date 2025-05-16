package com.fitgymtrack.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.models.*
import com.fitgymtrack.app.repository.ActiveWorkoutRepository
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

    // NUOVO: State flow per memorizzare i dati storici dell'ultimo allenamento
    private val _historicWorkoutData = MutableStateFlow<Map<Int, List<CompletedSeries>>>(emptyMap())
    val historicWorkoutData: StateFlow<Map<Int, List<CompletedSeries>>> = _historicWorkoutData.asStateFlow()

    // ID del gruppo di esercizi corrente per superset/circuit
    private val _currentExerciseGroupId = MutableStateFlow<String?>(null)
    val currentExerciseGroupId: StateFlow<String?> = _currentExerciseGroupId.asStateFlow()

    // NUOVO: Memorizza i valori di peso e ripetizioni per esercizio
    private val _exerciseValues = MutableStateFlow<Map<Int, Pair<Float, Int>>>(emptyMap())
    val exerciseValues: StateFlow<Map<Int, Pair<Float, Int>>> = _exerciseValues.asStateFlow()

    // Data e ora di inizio dell'allenamento
    private var sessionStartTime: Long = 0

    // ID dell'allenamento corrente
    private var allenamentoId: Int? = null

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
                val result = repository.getWorkoutExercises(schedaId)

                result.fold(
                    onSuccess = { exercises ->
                        // Crea un ActiveWorkout temporaneo con i dati disponibili
                        val workout = ActiveWorkout(
                            id = allenamentoId ?: 0,
                            schedaId = schedaId,
                            dataAllenamento = Date().toString(),
                            userId = 0, // Questo verrà aggiornato più tardi se necessario
                            esercizi = exercises
                        )

                        _workoutState.value = ActiveWorkoutState.Success(workout)

                        // NUOVO: Pre-carica i valori di peso e ripetizioni utilizzando lo storico
                        preloadExerciseValues(exercises)
                    },
                    onFailure = { e ->
                        _workoutState.value = ActiveWorkoutState.Error(
                            e.message ?: "Errore nel caricamento degli esercizi"
                        )
                    }
                )
            } catch (e: Exception) {
                _workoutState.value = ActiveWorkoutState.Error(
                    e.message ?: "Errore nel caricamento degli esercizi"
                )
            }
        }
    }

    /**
     * NUOVO: Pre-carica i valori di peso e ripetizioni per tutti gli esercizi
     */
    private fun preloadExerciseValues(exercises: List<WorkoutExercise>) {
        val valueMap = mutableMapOf<Int, Pair<Float, Int>>()

        exercises.forEach { exercise ->
            // Per ogni esercizio, tenta di caricare i valori dalla sessione precedente o dall'esercizio stesso
            val initialValues = getInitialValues(exercise.id, 0)
            valueMap[exercise.id] = initialValues
        }

        _exerciseValues.value = valueMap
    }

    /**
     * NUOVO: Ottiene i valori iniziali per un esercizio basandosi sullo storico o sui valori di default
     * @param exerciseId ID dell'esercizio
     * @param seriesIndex Indice della serie (0-based)
     * @return Coppia di (peso, ripetizioni)
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

        // Serie corrente (1-based)
        val currentSerieNumber = seriesIndex + 1

        // Log per debug
        Log.d("WorkoutHistory", "getInitialValues: esercizio=${exerciseId}, serie=${currentSerieNumber}")

        // 1. Prima verifica i dati storici dell'ultimo allenamento
        if (historicData.isNotEmpty()) {
            val historicSeries = historicData[exerciseId]
            if (historicSeries != null && historicSeries.isNotEmpty()) {
                // Cerca la serie con lo stesso numero nel dati storici
                val historicSeriesWithSameNumber = historicSeries.firstOrNull { it.serieNumber == currentSerieNumber }
                if (historicSeriesWithSameNumber != null) {
                    Log.d("WorkoutHistory", "Trovata serie storica ${currentSerieNumber} per esercizio ${exerciseId}: peso=${historicSeriesWithSameNumber.peso}, rip=${historicSeriesWithSameNumber.ripetizioni}")
                    return Pair(historicSeriesWithSameNumber.peso, historicSeriesWithSameNumber.ripetizioni)
                }

                // Se non troviamo una serie con lo stesso numero, usiamo l'ultima serie storica
                val lastHistoricSeries = historicSeries.lastOrNull()
                if (lastHistoricSeries != null) {
                    Log.d("WorkoutHistory", "Usando ultima serie storica: peso=${lastHistoricSeries.peso}, rip=${lastHistoricSeries.ripetizioni}")
                    return Pair(lastHistoricSeries.peso, lastHistoricSeries.ripetizioni)
                }
            }
        }

        // 2. Se non ci sono dati storici, verifichiamo serie già completate nell'allenamento corrente
        if (seriesState is CompletedSeriesState.Success) {
            val completedSeries = seriesState.series[exerciseId] ?: emptyList()

            if (completedSeries.isNotEmpty()) {
                // Cerca una serie con lo stesso numero
                val seriesWithSameNumber = completedSeries.firstOrNull { it.serieNumber == currentSerieNumber }

                if (seriesWithSameNumber != null) {
                    Log.d("WorkoutHistory", "Trovata serie ${currentSerieNumber} per esercizio ${exerciseId}: peso=${seriesWithSameNumber.peso}, rip=${seriesWithSameNumber.ripetizioni}")
                    return Pair(seriesWithSameNumber.peso, seriesWithSameNumber.ripetizioni)
                }

                // Se non abbiamo trovato una serie con lo stesso numero, usiamo l'ultima serie completata
                val lastCompletedSeries = completedSeries.lastOrNull()
                if (lastCompletedSeries != null) {
                    Log.d("WorkoutHistory", "Usando ultima serie disponibile: peso=${lastCompletedSeries.peso}, rip=${lastCompletedSeries.ripetizioni}")
                    return Pair(lastCompletedSeries.peso, lastCompletedSeries.ripetizioni)
                }
            }
        }

        // 3. Se non ci sono serie completate o storiche, usa i valori di default dell'esercizio
        Log.d("WorkoutHistory", "Usando valori default: peso=${exercise.peso}, rip=${exercise.ripetizioni}")
        return Pair(exercise.peso.toFloat(), exercise.ripetizioni)
    }

    /**
     * NUOVO: Salva i valori correnti di peso e ripetizioni per un esercizio
     */
    fun saveExerciseValues(exerciseId: Int, weight: Float, reps: Int) {
        val currentValues = _exerciseValues.value.toMutableMap()
        currentValues[exerciseId] = Pair(weight, reps)
        _exerciseValues.value = currentValues
    }

    /**
     * NUOVO: Seleziona un esercizio in un superset o circuito
     */
    fun selectExercise(exerciseId: Int) {
        _currentSelectedExerciseId.value = exerciseId
    }

    /**
     * NUOVO: Trova il prossimo esercizio in un superset
     */
    fun findNextExerciseInSuperset(currentExerciseId: Int): Int? {
        val currentState = _workoutState.value

        if (currentState !is ActiveWorkoutState.Success) {
            return null
        }

        val workout = currentState.workout
        val currentExercise = workout.esercizi.find { it.id == currentExerciseId } ?: return null

        // Verifica se l'esercizio corrente fa parte di un superset
        val isInSuperset = currentExercise.setType == "superset" || currentExercise.setType == "1"

        if (!isInSuperset) {
            return null
        }

        // Trova tutti gli esercizi nello stesso superset
        val supersetExercises = findExercisesInSameSuperset(workout.esercizi, currentExerciseId)

        // Trova l'indice dell'esercizio corrente
        val currentIndex = supersetExercises.indexOfFirst { it.id == currentExerciseId }

        // Se non trovato o è l'ultimo, ritorna il primo esercizio del superset
        if (currentIndex == -1 || currentIndex >= supersetExercises.size - 1) {
            return supersetExercises.firstOrNull()?.id
        }

        // Altrimenti, ritorna il prossimo esercizio
        return supersetExercises[currentIndex + 1].id
    }

    /**
     * NUOVO: Trova tutti gli esercizi in uno stesso superset
     */
    private fun findExercisesInSameSuperset(
        allExercises: List<WorkoutExercise>,
        startExerciseId: Int
    ): List<WorkoutExercise> {
        val result = mutableListOf<WorkoutExercise>()

        // Trova l'esercizio di partenza
        val startExercise = allExercises.find { it.id == startExerciseId } ?: return result
        val startIndex = allExercises.indexOfFirst { it.id == startExerciseId }

        // Verifica se l'esercizio fa parte di un superset
        val isSuperset = startExercise.setType == "superset" || startExercise.setType == "1"

        if (!isSuperset) {
            result.add(startExercise)
            return result
        }

        // Trova l'inizio del superset
        var supersetStartIndex = startIndex
        while (supersetStartIndex > 0 &&
            allExercises[supersetStartIndex - 1].setType == startExercise.setType &&
            allExercises[supersetStartIndex].linkedToPrevious) {
            supersetStartIndex--
        }

        // Aggiungi tutti gli esercizi del superset
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
     * NUOVO: Controlla se tutti gli esercizi in un superset hanno completato tutte le serie
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
     * Avvia il tracking del tempo trascorso
     */
    private fun startElapsedTimeTracking() {
        viewModelScope.launch {
            while (!_workoutCompleted.value) {
                val elapsedMillis = System.currentTimeMillis() - sessionStartTime
                _elapsedTime.value = (elapsedMillis / (1000 * 60)).toInt() // Converti in minuti
                delay(60000) // Aggiorna ogni minuto
            }
        }
    }

    /**
     * Carica le serie già completate per l'allenamento corrente
     * e recupera anche lo storico dell'ultimo allenamento per inizializzare i valori
     */
    private fun loadCompletedSeries() {
        val currentAllenamentoId = allenamentoId ?: return

        _seriesState.value = CompletedSeriesState.Loading

        viewModelScope.launch {
            try {
                // 1. Prima carichiamo le serie dell'allenamento corrente (se ce ne sono)
                val result = repository.getCompletedSeries(currentAllenamentoId)

                result.fold(
                    onSuccess = { seriesDataList ->
                        // Organizza le serie per esercizio
                        val seriesMap = mutableMapOf<Int, MutableList<CompletedSeries>>()

                        // Log per debug
                        Log.d("WorkoutHistory", "Serie nell'allenamento corrente: ${seriesDataList.size}")

                        seriesDataList.forEach { seriesData ->
                            val exId = seriesData.esercizioId ?: 0
                            Log.d("WorkoutHistory", "Serie trovata: esercizio=${exId}, peso=${seriesData.peso}, rip=${seriesData.ripetizioni}, serieNumber=${seriesData.serieNumber}, realSerieNumber=${seriesData.realSerieNumber}")

                            if (!seriesMap.containsKey(exId)) {
                                seriesMap[exId] = mutableListOf()
                            }

                            // Converti SeriesData in CompletedSeries
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

                            // Registra questo ID come già salvato
                            savedSeriesIds.add(seriesData.id)
                        }

                        _seriesState.value = CompletedSeriesState.Success(seriesMap)

                        // 2. Ora carichiamo lo storico dell'ultimo allenamento per inizializzare i valori
                        // Questo potrebbe essere fatto con una chiamata API separata se necessario
                        loadLastWorkoutHistory()
                    },
                    onFailure = { e ->
                        // Se è un 404, significa che non ci sono serie (allenamento nuovo)
                        // Lo consideriamo un caso di successo con lista vuota
                        if (e.message?.contains("404") == true) {
                            _seriesState.value = CompletedSeriesState.Success(emptyMap())

                            // Se non ci sono serie, carichiamo comunque lo storico
                            loadLastWorkoutHistory()
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
     * NUOVO: Carica lo storico dell'ultimo allenamento per inizializzare i valori
     * Implementazione temporanea con valori hardcoded per i test
     */
    private fun loadLastWorkoutHistory() {
        Log.d("WorkoutHistory", "Caricamento storico allenamento precedente (temp)")

        try {
            // Soluzione temporanea: carichiamo manualmente i valori storici dai dati forniti
            // Questi valori dovrebbero venire da un'API in una implementazione reale

            // Creiamo una mappa per memorizzare i dati storici
            val historicData = mutableMapOf<Int, MutableList<CompletedSeries>>()

            // Aggiungiamo i dati storici dell'esercizio 89 (Affondi)
            val affondiList = mutableListOf<CompletedSeries>()
            affondiList.add(CompletedSeries(
                id = "historic_89_1",
                serieNumber = 1,
                peso = 1.0f,
                ripetizioni = 11,
                tempoRecupero = 60,
                timestamp = "2025-05-16 17:19:09"
            ))
            affondiList.add(CompletedSeries(
                id = "historic_89_2",
                serieNumber = 2,
                peso = 3.0f,
                ripetizioni = 13,
                tempoRecupero = 60,
                timestamp = "2025-05-16 17:19:23"
            ))
            affondiList.add(CompletedSeries(
                id = "historic_89_3",
                serieNumber = 3,
                peso = 5.0f,
                ripetizioni = 15,
                tempoRecupero = 60,
                timestamp = "2025-05-16 17:19:31"
            ))
            historicData[89] = affondiList

            // Aggiungiamo i dati storici dell'esercizio 65 (Arnold Press)
            val arnoldList = mutableListOf<CompletedSeries>()
            arnoldList.add(CompletedSeries(
                id = "historic_65_1",
                serieNumber = 1,
                peso = 2.0f,
                ripetizioni = 12,
                tempoRecupero = 60,
                timestamp = "2025-05-16 17:19:11"
            ))
            arnoldList.add(CompletedSeries(
                id = "historic_65_2",
                serieNumber = 2,
                peso = 4.0f,
                ripetizioni = 14,
                tempoRecupero = 60,
                timestamp = "2025-05-16 17:19:24"
            ))
            arnoldList.add(CompletedSeries(
                id = "historic_65_3",
                serieNumber = 3,
                peso = 6.0f,
                ripetizioni = 16,
                tempoRecupero = 60,
                timestamp = "2025-05-16 17:19:32"
            ))
            historicData[65] = arnoldList

            // Aggiungiamo i dati storici dell'esercizio 64 (Alzate frontali)
            val alzateList = mutableListOf<CompletedSeries>()
            alzateList.add(CompletedSeries(
                id = "historic_64_1",
                serieNumber = 1,
                peso = 7.0f,
                ripetizioni = 17,
                tempoRecupero = 60,
                timestamp = "2025-05-16 17:19:52"
            ))
            alzateList.add(CompletedSeries(
                id = "historic_64_2",
                serieNumber = 2,
                peso = 8.0f,
                ripetizioni = 18,
                tempoRecupero = 60,
                timestamp = "2025-05-16 17:19:55"
            ))
            alzateList.add(CompletedSeries(
                id = "historic_64_3",
                serieNumber = 3,
                peso = 9.0f,
                ripetizioni = 19,
                tempoRecupero = 60,
                timestamp = "2025-05-16 17:19:57"
            ))
            historicData[64] = alzateList

            // Salviamo questi dati storici in una mappa che possiamo usare in getInitialValues
            _historicWorkoutData.value = historicData

            Log.d("WorkoutHistory", "Storico caricato: ${historicData.keys.size} esercizi")
            historicData.forEach { (exId, series) ->
                Log.d("WorkoutHistory", "Esercizio $exId: ${series.size} serie")
                series.forEach { serie ->
                    Log.d("WorkoutHistory", "- Serie ${serie.serieNumber}: peso=${serie.peso}, rip=${serie.ripetizioni}")
                }
            }
        } catch (e: Exception) {
            Log.e("WorkoutHistory", "Errore caricamento storico: ${e.message}", e)
        }
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

        // Previeni duplicati basati sul numero di serie
        val seriesMap = when (val state = _seriesState.value) {
            is CompletedSeriesState.Success -> state.series
            else -> emptyMap()
        }

        val existingSeries = seriesMap[exerciseId] ?: emptyList()
        if (existingSeries.any { it.serieNumber == serieNumber }) {
            // Serie già salvata, ignora la richiesta
            return
        }

        _saveSeriesState.value = SaveSeriesState.Loading

        viewModelScope.launch {
            try {
                // Genera un ID univoco per questa serie
                val serieId = "serie_${System.currentTimeMillis()}_${serieNumber}_${UUID.randomUUID().toString().substring(0, 8)}"

                // Genera l'ID univoco per la richiesta
                val requestId = "req_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"

                // Calcola il serie_number nel formato corretto: esercizioId * 100 + serieNumber
                // Questo garantisce che ogni serie abbia un identificatore univoco che include sia l'ID dell'esercizio che il numero di serie
                val serieNumberEncoded = exerciseId * 100 + serieNumber

                Log.d("WorkoutHistory", "Salvataggio serie: esercizio=${exerciseId}, serie=${serieNumber}, serieNumberEncoded=${serieNumberEncoded}")

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
                            // Registra questo ID come già salvato
                            savedSeriesIds.add(serieId)

                            // Aggiorna lo stato delle serie completate
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

                            // Salva i valori attuali per questo esercizio
                            saveExerciseValues(exerciseId, peso, ripetizioni)

                            // NUOVO: Verifica se l'esercizio fa parte di un superset
                            handleSupersetNavigation(exerciseId)

                            // Salva l'ID dell'esercizio corrente per il recupero
                            _currentRecoveryExerciseId.value = exerciseId

                            // Verifica se questo esercizio fa parte di un superset o circuit
                            checkAndHandleExerciseGroup(exerciseId)

                            _saveSeriesState.value = SaveSeriesState.Success
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
     * NUOVO: Gestisce la navigazione automatica nei superset
     */
    private fun handleSupersetNavigation(exerciseId: Int) {
        val currentState = _workoutState.value

        if (currentState !is ActiveWorkoutState.Success) {
            return
        }

        val workout = currentState.workout
        val currentExercise = workout.esercizi.find { it.id == exerciseId } ?: return

        // Verifica se l'esercizio fa parte di un superset
        val isInSuperset = currentExercise.setType == "superset" || currentExercise.setType == "1"

        if (!isInSuperset) {
            return
        }

        // Trova gli esercizi nel superset
        val supersetExercises = findExercisesInSameSuperset(workout.esercizi, exerciseId)

        // Trova l'indice dell'esercizio corrente nel superset
        val currentIndex = supersetExercises.indexOfFirst { it.id == exerciseId }

        // Se è l'ultimo esercizio del superset
        if (currentIndex == supersetExercises.size - 1) {
            // Verifica se tutti gli esercizi del superset hanno completato tutte le serie
            val allExercisesCompleted = isAllSupersetExercisesCompleted(supersetExercises)

            if (allExercisesCompleted) {
                // Se tutti gli esercizi hanno completato tutte le serie, non facciamo nulla
                return
            }

            // Altrimenti, torna al primo esercizio del superset per la prossima serie
            val firstExerciseId = supersetExercises.firstOrNull()?.id ?: return

            // Imposta un breve timer prima di passare al primo esercizio
            viewModelScope.launch {
                // Se siamo alla fine del superset, applichiamo un timer di recupero
                val recoveryTime = currentExercise.tempoRecupero
                if (recoveryTime > 0) {
                    startRecoveryTimer(recoveryTime)
                }

                // Seleziona il primo esercizio del superset
                selectExercise(firstExerciseId)
            }
        } else {
            // Non è l'ultimo esercizio, passa al prossimo
            val nextExerciseId = supersetExercises.getOrNull(currentIndex + 1)?.id ?: return

            // Seleziona il prossimo esercizio
            selectExercise(nextExerciseId)
        }
    }

    /**
     * Verifica se l'esercizio appartiene a un gruppo (superset/circuit)
     * e gestisce il timer di conseguenza
     */
    private fun checkAndHandleExerciseGroup(exerciseId: Int) {
        val workout = when (val state = _workoutState.value) {
            is ActiveWorkoutState.Success -> state.workout
            else -> return
        }

        // Trova l'esercizio corrente
        val currentExercise = workout.esercizi.find { it.id == exerciseId } ?: return
        val currentExerciseIndex = workout.esercizi.indexOfFirst { it.id == exerciseId }

        // Determina il tipo di set
        val setType = currentExercise.setType

        when (setType) {
            "superset", "circuit" -> {
                // Crea un ID di gruppo se non esiste
                if (_currentExerciseGroupId.value == null) {
                    _currentExerciseGroupId.value = "group_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
                }

                // Trova tutti gli esercizi che appartengono a questo gruppo
                val groupExercises = findExercisesInSameGroup(workout.esercizi, currentExerciseIndex)

                // Trova l'indice dell'esercizio corrente nel gruppo
                val groupIndex = groupExercises.indexOfFirst { it.id == exerciseId }

                // Se non è l'ultimo del gruppo, non avviare il timer di recupero
                if (groupIndex < groupExercises.size - 1) {
                    return
                }

                // È l'ultimo esercizio del gruppo, avvia il timer di recupero
                val tempoRecupero = currentExercise.tempoRecupero
                if (tempoRecupero > 0) {
                    startRecoveryTimer(tempoRecupero)
                }

                // Reset dell'ID del gruppo quando finisce un giro
                _currentExerciseGroupId.value = null
            }
            else -> {
                // Esercizio normale, avvia il timer di recupero normalmente
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

        // Se l'indice è fuori dai limiti, restituisci una lista vuota
        if (startIndex < 0 || startIndex >= allExercises.size) {
            return result
        }

        // Ottieni l'esercizio iniziale
        val startExercise = allExercises[startIndex]
        val setType = startExercise.setType

        // Se è un esercizio normale, restituisci solo quello
        if (setType == "normal") {
            result.add(startExercise)
            return result
        }

        // Trova l'inizio del gruppo (primo esercizio del gruppo)
        var groupStartIndex = startIndex
        while (groupStartIndex > 0 &&
            allExercises[groupStartIndex - 1].setType == setType &&
            allExercises[groupStartIndex].linkedToPrevious) {
            groupStartIndex--
        }

        // Aggiungi tutti gli esercizi del gruppo
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

            // Aggiungi la nuova serie
            exerciseSeries.add(newSeries)

            // Aggiorna la mappa
            updatedMap[exerciseId] = exerciseSeries

            // Aggiorna lo stato
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
            while (_recoveryTime.value > 0 && _isTimerRunning.value) {
                delay(1000)
                _recoveryTime.value = max(0, _recoveryTime.value - 1)
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
        val durataTotale = _elapsedTime.value

        _completeWorkoutState.value = CompleteWorkoutState.Loading

        viewModelScope.launch {
            try {
                val result = repository.completeWorkout(
                    allenamentoId = currentAllenamentoId,
                    durataTotale = durataTotale,
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
                // Non ci interessa il risultato, torniamo indietro comunque
                resetWorkoutState()
            } catch (e: Exception) {
                // Ignora gli errori, torniamo indietro comunque
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
        savedSeriesIds.clear()
    }

    /**
     * Formatta il tempo trascorso come ore:minuti
     */
    fun getFormattedElapsedTime(): String {
        val minutes = _elapsedTime.value
        val hours = minutes / 60
        val mins = minutes % 60

        return if (hours > 0) {
            "$hours:${mins.toString().padStart(2, '0')}"
        } else {
            "$mins min"
        }
    }

    /**
     * Formatta il tempo di recupero come minuti:secondi
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
     * Raggruppa gli esercizi per tipo (superset, circuit, normal)
     */
    fun groupExercisesByType(): List<List<WorkoutExercise>> {
        val workout = when (val state = _workoutState.value) {
            is ActiveWorkoutState.Success -> state.workout
            else -> return emptyList()
        }

        val result = mutableListOf<List<WorkoutExercise>>()
        var currentGroup = mutableListOf<WorkoutExercise>()

        workout.esercizi.forEachIndexed { index, exercise ->
            // Se è il primo esercizio o non è collegato al precedente, inizia un nuovo gruppo
            if (index == 0 || !exercise.linkedToPrevious) {
                // Se avevamo già un gruppo, aggiungiamolo al risultato
                if (currentGroup.isNotEmpty()) {
                    result.add(currentGroup.toList())
                }
                // Inizia un nuovo gruppo con questo esercizio
                currentGroup = mutableListOf(exercise)
            } else {
                // Questo esercizio è collegato al precedente, aggiungilo al gruppo corrente
                currentGroup.add(exercise)
            }
        }

        // Aggiungi l'ultimo gruppo se non è vuoto
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