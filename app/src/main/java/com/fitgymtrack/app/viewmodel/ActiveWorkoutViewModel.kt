package com.fitgymtrack.app.viewmodel

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

    // ID del gruppo di esercizi corrente per superset/circuit
    private val _currentExerciseGroupId = MutableStateFlow<String?>(null)
    val currentExerciseGroupId: StateFlow<String?> = _currentExerciseGroupId.asStateFlow()

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
     * Carica le serie già completate
     */
    private fun loadCompletedSeries() {
        val currentAllenamentoId = allenamentoId ?: return

        _seriesState.value = CompletedSeriesState.Loading

        viewModelScope.launch {
            try {
                val result = repository.getCompletedSeries(currentAllenamentoId)

                result.fold(
                    onSuccess = { seriesDataList ->
                        // Organizza le serie per esercizio
                        val seriesMap = mutableMapOf<Int, MutableList<CompletedSeries>>()

                        seriesDataList.forEach { seriesData ->
                            val exId = seriesData.esercizioId ?: 0

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
                    },
                    onFailure = { e ->
                        // Se è un 404, significa che non ci sono serie (allenamento nuovo)
                        // Lo consideriamo un caso di successo con lista vuota
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

                // Calcola il serie_number nel formato richiesto dal backend (esercizioId * 100 + serieNumber)
                val serieNumberEncoded = exerciseId * 100 + serieNumber

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