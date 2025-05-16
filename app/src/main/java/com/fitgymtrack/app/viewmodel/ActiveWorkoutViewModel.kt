package com.fitgymtrack.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.api.ApiClient
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

                            // Carica lo storico degli allenamenti precedenti
                            loadLastWorkoutHistory(userId, schedaId)
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
                            userId = userId ?: 0,
                            esercizi = exercises
                        )

                        _workoutState.value = ActiveWorkoutState.Success(workout)

                        // Pre-carica i valori di peso e ripetizioni dai default
                        // (verranno poi aggiornati quando saranno disponibili i dati storici)
                        preloadDefaultValues()
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
     * Carica lo storico dell'ultimo allenamento per inizializzare i valori
     * @param userId ID dell'utente
     * @param schedaId ID della scheda corrente
     */
    private fun loadLastWorkoutHistory(userId: Int, schedaId: Int) {
        Log.d("WorkoutHistory", "Caricamento storico allenamento precedente per userId=$userId, schedaId=$schedaId")

        viewModelScope.launch {
            try {
                // 1. Ottieni l'elenco degli allenamenti precedenti per questo utente
                Log.d("WorkoutHistory", "CHIAMATA API: getWorkoutHistory per userId=$userId")
                val workoutHistoryApiService = ApiClient.workoutHistoryApiService
                val allWorkoutsResponse = workoutHistoryApiService.getWorkoutHistory(userId)
                Log.d("WorkoutHistory", "RISPOSTA API getWorkoutHistory: success=${allWorkoutsResponse["success"]}, " +
                        "count=${allWorkoutsResponse["count"]}")

                // Estraiamo dati dalla risposta di tipo Map<String, Any>
                val success = allWorkoutsResponse["success"] as? Boolean ?: false
                val allenamenti = allWorkoutsResponse["allenamenti"] as? List<Map<String, Any>> ?: emptyList()

                if (!success || allenamenti.isEmpty()) {
                    Log.d("WorkoutHistory", "Nessun allenamento precedente trovato")
                    return@launch
                }

                Log.d("WorkoutHistory", "Trovati ${allenamenti.size} allenamenti precedenti per l'utente")

                // Log dettagliato di tutti gli allenamenti
                allenamenti.forEachIndexed { index, workout ->
                    val workoutId = workout["id"]?.toString() ?: "N/A"
                    val scheda = workout["scheda_id"]?.toString() ?: "N/A"
                    val data = workout["data_allenamento"]?.toString() ?: "N/A"
                    val durata = workout["durata_totale"]?.toString() ?: "N/A"
                    val note = workout["note"]?.toString() ?: "N/A"
                    val schedaNome = workout["scheda_nome"]?.toString() ?: "N/A"

                    Log.d("WorkoutHistory", "Allenamento[$index]: ID=$workoutId, Scheda=$scheda(${schedaNome}), " +
                            "Data=$data, Durata=$durata, Note=$note")
                }

                // Ottieni la lista di esercizi per la scheda corrente
                val currentExercises = when (val state = _workoutState.value) {
                    is ActiveWorkoutState.Success -> state.workout.esercizi
                    else -> emptyList()
                }

                // Crea una mappa di ID esercizi per verificare rapidamente quali esercizi sono nella scheda corrente
                val exerciseIds = currentExercises.map { it.id }.toSet()
                Log.d("WorkoutHistory", "Esercizi nella scheda corrente: $exerciseIds")

                // TEST SPECIFICO: tenta di caricare direttamente le serie per l'allenamento 689
                // per verificare che l'API funzioni correttamente
                Log.d("WorkoutHistory", "TEST: Tentativo di caricare direttamente le serie per allenamento 689")
                try {
                    val testWorkoutId = 689
                    Log.d("WorkoutHistory", "CHIAMATA API DIRETTA: getWorkoutSeriesDetail($testWorkoutId)")
                    val testSeriesResponse = workoutHistoryApiService.getWorkoutSeriesDetail(testWorkoutId)
                    Log.d("WorkoutHistory", "RISPOSTA API DIRETTA: success=${testSeriesResponse.success}, " +
                            "count=${testSeriesResponse.serie.size}")

                    if (testSeriesResponse.success && testSeriesResponse.serie.isNotEmpty()) {
                        Log.d("WorkoutHistory", "TEST RIUSCITO: Trovate ${testSeriesResponse.serie.size} serie per allenamento 689")

                        // Log di tutte le serie trovate
                        testSeriesResponse.serie.forEach { serie ->
                            Log.d("WorkoutHistory", "Serie di test: id=${serie.id}, " +
                                    "schedaEsercizioId=${serie.schedaEsercizioId}, " +
                                    "peso=${serie.peso}, rip=${serie.ripetizioni}, " +
                                    "serieNumber=${serie.serieNumber}, " +
                                    "esercizioId=${serie.esercizioId}, " +
                                    "realSerieNumber=${serie.realSerieNumber}")

                            // Verifica se questa serie è per un esercizio che ci interessa
                            if (serie.schedaEsercizioId in exerciseIds) {
                                Log.d("WorkoutHistory", "  ✓ Serie rilevante per esercizio ${serie.schedaEsercizioId}")
                            } else {
                                Log.d("WorkoutHistory", "  ✗ Serie NON rilevante per scheda corrente")
                            }
                        }

                        // Proviamo a creare direttamente lo storico con le serie trovate
                        val historicData = mutableMapOf<Int, MutableList<CompletedSeries>>()

                        // Raggruppa le serie per esercizio
                        val seriesByExercise = testSeriesResponse.serie.groupBy { it.schedaEsercizioId }

                        Log.d("WorkoutHistory", "Serie raggruppate per esercizio: ${seriesByExercise.keys}")

                        // Per ogni esercizio, crea una lista di CompletedSeries
                        seriesByExercise.forEach { (exerciseId, series) ->
                            Log.d("WorkoutHistory", "Creazione CompletedSeries per esercizio $exerciseId")

                            if (exerciseId in exerciseIds) {
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

                                    Log.d("WorkoutHistory", "  Creata CompletedSeries: id=${completed.id}, " +
                                            "serieNumber=${completed.serieNumber}, " +
                                            "peso=${completed.peso}, rip=${completed.ripetizioni}")

                                    completedSeries.add(completed)
                                }

                                historicData[exerciseId] = completedSeries.sortedBy { it.serieNumber }.toMutableList()
                                Log.d("WorkoutHistory", "Aggiunte ${completedSeries.size} serie ordinate per esercizio $exerciseId")
                            }
                        }

                        // Aggiorna lo stato con i dati storici
                        if (historicData.isNotEmpty()) {
                            _historicWorkoutData.value = historicData
                            Log.d("WorkoutHistory", "STORICO AGGIORNATO con ${historicData.size} esercizi")

                            // Log dettagliato dello storico creato
                            historicData.forEach { (exId, series) ->
                                Log.d("WorkoutHistory", "Storico finale - Esercizio $exId: ${series.size} serie")
                                series.forEach { serie ->
                                    Log.d("WorkoutHistory", "  Serie ${serie.serieNumber}: " +
                                            "peso=${serie.peso}, rip=${serie.ripetizioni}")
                                }
                            }

                            // Aggiorna i valori iniziali per gli esercizi
                            preloadExerciseValues(currentExercises)
                            return@launch
                        }
                    } else {
                        Log.d("WorkoutHistory", "TEST FALLITO: Nessuna serie trovata per allenamento 689 o risposta non valida")
                    }
                } catch (e: Exception) {
                    Log.e("WorkoutHistory", "ERRORE NEL TEST DIRETTO: ${e.message}")
                    e.printStackTrace()
                }

                // Se il test diretto fallisce, procedi con l'approccio standard
                Log.d("WorkoutHistory", "Procedendo con l'approccio standard...")

                // APPROCCIO STANDARD: Controlla ciascun allenamento individualmente

                // FASE 1: Controlla allenamenti con la stessa scheda (priorità)
                val sameSchemaWorkouts = allenamenti.filter {
                    it["scheda_id"]?.toString()?.toIntOrNull() == schedaId
                }.sortedByDescending {
                    it["data_allenamento"]?.toString() ?: ""
                }

                Log.d("WorkoutHistory", "Allenamenti con scheda $schedaId: ${sameSchemaWorkouts.size}")

                // Variabile per tracciare se abbiamo trovato dati utili
                var foundUsefulData = false

                // FASE 2: Per ogni allenamento con la stessa scheda, controlla le serie
                for (workout in sameSchemaWorkouts) {
                    val workoutId = workout["id"]?.toString()?.toIntOrNull() ?: continue

                    Log.d("WorkoutHistory", "Controllo serie in allenamento $workoutId")

                    try {
                        // Ottieni le serie completate per questo allenamento
                        Log.d("WorkoutHistory", "CHIAMATA API: getWorkoutSeriesDetail($workoutId)")
                        val seriesResponse = workoutHistoryApiService.getWorkoutSeriesDetail(workoutId)

                        Log.d("WorkoutHistory", "RISPOSTA API: success=${seriesResponse.success}, " +
                                "serie=${seriesResponse.serie.size}")

                        if (seriesResponse.success && seriesResponse.serie.isNotEmpty()) {
                            // Ci sono serie! Log delle prime 5 serie
                            seriesResponse.serie.take(5).forEach { serie ->
                                Log.d("WorkoutHistory", "Serie trovata: id=${serie.id}, " +
                                        "schedaEsercizioId=${serie.schedaEsercizioId}, " +
                                        "peso=${serie.peso}, rip=${serie.ripetizioni}, " +
                                        "serieNumber=${serie.serieNumber}, " +
                                        "esercizioId=${serie.esercizioId}, " +
                                        "realSerieNumber=${serie.realSerieNumber}")
                            }
                            if (seriesResponse.serie.size > 5) {
                                Log.d("WorkoutHistory", "... e altre ${seriesResponse.serie.size - 5} serie")
                            }

                            // Verifica quali serie sono rilevanti per gli esercizi che ci interessano
                            val relevantSeries = seriesResponse.serie.filter { serie ->
                                // L'ID dell'esercizio è memorizzato in schedaEsercizioId
                                val exerciseId = serie.schedaEsercizioId
                                exerciseId in exerciseIds
                            }

                            Log.d("WorkoutHistory", "Serie rilevanti per gli esercizi correnti: ${relevantSeries.size}")

                            if (relevantSeries.isNotEmpty()) {
                                // Raggruppare le serie per esercizio
                                val seriesByExercise = relevantSeries.groupBy { it.schedaEsercizioId }

                                // Creare un nuovo storico
                                val historicData = mutableMapOf<Int, MutableList<CompletedSeries>>()

                                // Per ogni esercizio, convertire le serie in CompletedSeries
                                seriesByExercise.forEach { (exerciseId, series) ->
                                    val completedSeries = mutableListOf<CompletedSeries>()

                                    series.forEach { serie ->
                                        // Il numero di serie è in realSerieNumber o calcolato da serieNumber
                                        val serieNumber = serie.realSerieNumber ?:
                                        (serie.serieNumber?.rem(100)) ?:
                                        (completedSeries.size + 1)

                                        Log.d("WorkoutHistory", "Creando CompletedSeries: exerciseId=$exerciseId, " +
                                                "serieNumber=$serieNumber, peso=${serie.peso}, rip=${serie.ripetizioni}")

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

                                    historicData[exerciseId] = completedSeries.sortedBy { it.serieNumber }.toMutableList()
                                }

                                // Aggiorna lo stato con i dati storici
                                _historicWorkoutData.value = historicData

                                Log.d("WorkoutHistory", "Storico aggiornato con ${historicData.size} esercizi da allenamento $workoutId")

                                // Aggiorna i valori iniziali per gli esercizi
                                preloadExerciseValues(currentExercises)

                                // Abbiamo trovato dati utili!
                                foundUsefulData = true
                                break
                            }
                        } else {
                            Log.d("WorkoutHistory", "Nessuna serie trovata nell'allenamento $workoutId")
                        }
                    } catch (e: Exception) {
                        Log.e("WorkoutHistory", "Errore nel controllare l'allenamento $workoutId: ${e.message}")
                        e.printStackTrace()
                    }
                }

                // Se non abbiamo trovato dati, usa i valori di default
                if (!foundUsefulData) {
                    Log.d("WorkoutHistory", "Nessun dato storico trovato per gli esercizi richiesti, uso i valori di default")
                    preloadExerciseValues(currentExercises)
                }

            } catch (e: Exception) {
                Log.e("WorkoutHistory", "Errore generale nel caricamento dello storico: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    /**
     * Carica le serie da un allenamento specifico
     * @param workoutId ID dell'allenamento
     * @param exerciseIds Set di ID degli esercizi da considerare
     * @param replaceAll Se true, sostituisce tutti i dati storici, altrimenti aggiunge a quelli esistenti
     */
    private suspend fun loadSeriesFromWorkout(workoutId: Int, exerciseIds: Set<Int>, replaceAll: Boolean) {
        Log.d("WorkoutHistory", "loadSeriesFromWorkout: workoutId=$workoutId, exerciseIds=$exerciseIds, replaceAll=$replaceAll")

        try {
            val workoutHistoryApiService = ApiClient.workoutHistoryApiService
            val completedSeriesResponse = workoutHistoryApiService.getWorkoutSeriesDetail(workoutId)

            if (!completedSeriesResponse.success || completedSeriesResponse.serie.isEmpty()) {
                Log.d("WorkoutHistory", "Nessuna serie completata trovata nell'allenamento $workoutId")
                return
            }

            Log.d("WorkoutHistory", "Trovate ${completedSeriesResponse.serie.size} serie nell'allenamento $workoutId")

            // Elabora le serie completate - creiamo una nuova mappa con liste mutabili
            val historicData = mutableMapOf<Int, MutableList<CompletedSeries>>()

            // Se non stiamo sostituendo tutti i dati, copiamo i dati esistenti nelle liste mutabili
            if (!replaceAll) {
                _historicWorkoutData.value.forEach { (exerciseId, seriesList) ->
                    historicData[exerciseId] = seriesList.toMutableList()
                }
            }

            // Filtra le serie che ci interessano
            val relevantSeries = completedSeriesResponse.serie.filter {
                it.schedaEsercizioId in exerciseIds
            }

            Log.d("WorkoutHistory", "Serie rilevanti per gli esercizi richiesti: ${relevantSeries.size}")

            // Elabora le serie
            relevantSeries.forEach { seriesData ->
                val exerciseId = seriesData.schedaEsercizioId

                // Estrai il numero di serie dal codice serie_number
                val serieNumber = if (seriesData.serieNumber != null) {
                    seriesData.serieNumber % 100
                } else {
                    seriesData.realSerieNumber ?:
                    (historicData[exerciseId]?.size ?: 0) + 1
                }

                Log.d("WorkoutHistory", "Serie trovata: esercizio=$exerciseId, peso=${seriesData.peso}, " +
                        "rip=${seriesData.ripetizioni}, serieNumber=${seriesData.serieNumber}, " +
                        "decodedSerieNumber=$serieNumber")

                // Assicuriamoci che ci sia una lista mutabile per questo esercizio
                if (!historicData.containsKey(exerciseId)) {
                    historicData[exerciseId] = mutableListOf()
                }

                // Converti in CompletedSeries e aggiungi alla lista
                val completedSeries = CompletedSeries(
                    id = seriesData.id,
                    serieNumber = serieNumber,
                    peso = seriesData.peso,
                    ripetizioni = seriesData.ripetizioni,
                    tempoRecupero = seriesData.tempoRecupero ?: 60,
                    timestamp = seriesData.timestamp,
                    note = seriesData.note
                )

                // Ora questa lista è sicuramente mutabile
                historicData[exerciseId]!!.add(completedSeries)
            }

            // Ordina le serie per numero
            historicData.forEach { (exerciseId, series) ->
                historicData[exerciseId] = series.sortedBy { it.serieNumber }.toMutableList()
            }

            // Aggiorna lo stato con i dati storici
            _historicWorkoutData.value = historicData

            Log.d("WorkoutHistory", "Storico caricato per ${historicData.keys.size} esercizi")
            historicData.forEach { (exId, series) ->
                Log.d("WorkoutHistory", "Esercizio $exId: ${series.size} serie")
                series.forEach { serie ->
                    Log.d("WorkoutHistory", "- Serie ${serie.serieNumber}: peso=${serie.peso}, rip=${serie.ripetizioni}")
                }
            }

            // Quando vengono caricati i dati, aggiorna i valori di default
            Log.d("WorkoutHistory", "Aggiornando i valori iniziali dopo aver caricato lo storico")
            preloadExerciseValues(
                when (val state = _workoutState.value) {
                    is ActiveWorkoutState.Success -> state.workout.esercizi
                    else -> emptyList()
                }
            )
        } catch (e: Exception) {
            Log.e("WorkoutHistory", "Errore nel caricamento delle serie per allenamento $workoutId: ${e.message}")
            e.printStackTrace() // Importante: stampa lo stack trace per diagnosticare
        }
    }

    /**
     * Pre-carica i valori di peso e ripetizioni per tutti gli esercizi
     */
    private fun preloadExerciseValues(exercises: List<WorkoutExercise>) {
        val valueMap = mutableMapOf<Int, Pair<Float, Int>>()

        exercises.forEach { exercise ->
            // Per ogni esercizio, tenta di caricare i valori dalla sessione precedente o dall'esercizio stesso
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
            // Usa direttamente i valori di default senza cercare dati storici
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
     * @param exerciseId ID dell'esercizio
     * @param seriesIndex Indice della serie (0-based, cioè la prima serie ha indice 0)
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

        // Serie corrente (1-based, come memorizzata nel DB)
        val currentSerieNumber = seriesIndex + 1

        // Log per debug
        Log.d("WorkoutHistory", "getInitialValues: esercizio=$exerciseId, serie=$currentSerieNumber (index=$seriesIndex)")

        // 1. Prima verifica i dati storici dell'ultimo allenamento
        if (historicData.isNotEmpty() && historicData.containsKey(exerciseId)) {
            val historicSeries = historicData[exerciseId] ?: emptyList()

            if (historicSeries.isNotEmpty()) {
                // Log debug dello storico disponibile
                Log.d("WorkoutHistory", "Storico disponibile per esercizio $exerciseId: ${historicSeries.size} serie")
                historicSeries.forEach { serie ->
                    Log.d("WorkoutHistory", "  Serie ${serie.serieNumber}: peso=${serie.peso}, rip=${serie.ripetizioni}")
                }

                // Cerca la serie con lo stesso numero nei dati storici
                val historicSeriesWithSameNumber = historicSeries.firstOrNull {
                    it.serieNumber == currentSerieNumber
                }

                if (historicSeriesWithSameNumber != null) {
                    Log.d("WorkoutHistory", "Trovata serie storica $currentSerieNumber per esercizio $exerciseId: " +
                            "peso=${historicSeriesWithSameNumber.peso}, rip=${historicSeriesWithSameNumber.ripetizioni}")
                    return Pair(historicSeriesWithSameNumber.peso, historicSeriesWithSameNumber.ripetizioni)
                } else {
                    Log.d("WorkoutHistory", "Nessuna serie storica con numero $currentSerieNumber trovata")
                }

                // Se non troviamo una serie con lo stesso numero, usiamo l'ultima serie storica
                // Solo se l'indice richiesto è superiore all'ultimo indice disponibile
                if (currentSerieNumber > historicSeries.maxOfOrNull { it.serieNumber } ?: 0) {
                    val lastHistoricSeries = historicSeries.maxByOrNull { it.serieNumber }
                    if (lastHistoricSeries != null) {
                        Log.d("WorkoutHistory", "Usando ultima serie storica disponibile: peso=${lastHistoricSeries.peso}, " +
                                "rip=${lastHistoricSeries.ripetizioni}")
                        return Pair(lastHistoricSeries.peso, lastHistoricSeries.ripetizioni)
                    }
                }
            }
        } else {
            Log.d("WorkoutHistory", "Nessun dato storico disponibile per esercizio $exerciseId")
        }

        // 2. Se non ci sono dati storici per questa serie, verifichiamo serie già completate nell'allenamento corrente
        if (seriesState is CompletedSeriesState.Success) {
            val completedSeries = seriesState.series[exerciseId] ?: emptyList()

            if (completedSeries.isNotEmpty()) {
                // Log debug delle serie completate
                Log.d("WorkoutHistory", "Serie già completate nell'allenamento corrente per esercizio $exerciseId: ${completedSeries.size}")

                // Cerca una serie con lo stesso numero
                val seriesWithSameNumber = completedSeries.firstOrNull { it.serieNumber == currentSerieNumber }

                if (seriesWithSameNumber != null) {
                    Log.d("WorkoutHistory", "Trovata serie già completata $currentSerieNumber per esercizio $exerciseId: " +
                            "peso=${seriesWithSameNumber.peso}, rip=${seriesWithSameNumber.ripetizioni}")
                    return Pair(seriesWithSameNumber.peso, seriesWithSameNumber.ripetizioni)
                }

                // Se non abbiamo trovato una serie con lo stesso numero, cerchiamo la serie immediatamente precedente
                val previousSerieNumber = currentSerieNumber - 1
                val previousCompletedSeries = completedSeries.firstOrNull { it.serieNumber == previousSerieNumber }

                if (previousCompletedSeries != null) {
                    Log.d("WorkoutHistory", "Usando serie precedente completata ($previousSerieNumber): " +
                            "peso=${previousCompletedSeries.peso}, rip=${previousCompletedSeries.ripetizioni}")
                    return Pair(previousCompletedSeries.peso, previousCompletedSeries.ripetizioni)
                }

                // Altrimenti, usiamo l'ultima serie completata
                val lastCompletedSeries = completedSeries.maxByOrNull { it.serieNumber }
                if (lastCompletedSeries != null) {
                    Log.d("WorkoutHistory", "Usando ultima serie disponibile: peso=${lastCompletedSeries.peso}, " +
                            "rip=${lastCompletedSeries.ripetizioni}")
                    return Pair(lastCompletedSeries.peso, lastCompletedSeries.ripetizioni)
                }
            } else {
                Log.d("WorkoutHistory", "Nessuna serie completata nell'allenamento corrente per esercizio $exerciseId")
            }
        }



        // 3. Se non ci sono serie completate o storiche, usa i valori di default dell'esercizio
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
                _elapsedTime.value = (elapsedMillis / (1000 * 60)).toInt() // Converti in minuti
                delay(60000) // Aggiorna ogni minuto
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
                // Carica le serie dell'allenamento corrente (se ce ne sono)
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
     * Trova tutti gli esercizi in uno stesso superset
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
     *
     * Questo metodo deve essere chiamato quando l'utente passa da una serie alla successiva
     * @param exerciseId ID dell'esercizio
     * @param newSeriesIndex Indice della nuova serie (0-based)
     */
    fun updateUIForNewSeries(exerciseId: Int, newSeriesIndex: Int) {
        Log.d("WorkoutHistory", "Aggiornamento UI per nuova serie: esercizio=$exerciseId, nuova serie=${newSeriesIndex + 1}")

        // Carica i valori iniziali per questa nuova serie
        val initialValues = getInitialValues(exerciseId, newSeriesIndex)

        // Aggiorna i valori nel ViewModel
        val currentValues = _exerciseValues.value.toMutableMap()
        currentValues[exerciseId] = initialValues
        _exerciseValues.value = currentValues

        Log.d("WorkoutHistory", "UI aggiornata per serie ${newSeriesIndex + 1}: peso=${initialValues.first}, rip=${initialValues.second}")
    }

    /**
     * Metodo per cambiare serie nel workout
     * @param exerciseId ID dell'esercizio
     * @param seriesIndex Indice della serie a cui passare (0-based)
     */
    fun switchToSeries(exerciseId: Int, seriesIndex: Int) {
        // Aggiorna l'interfaccia utente con i valori corretti per la nuova serie
        updateUIForNewSeries(exerciseId, seriesIndex)

        // Qui puoi aggiungere altra logica necessaria per la navigazione tra serie
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

                            // AGGIUNTO: Prepara i valori per la serie successiva se ne esiste una
                            val workout = (workoutState.value as? ActiveWorkoutState.Success)?.workout
                            val exercise = workout?.esercizi?.find { it.id == exerciseId }

                            if (exercise != null && serieNumber < exercise.serie) {
                                // Passa alla serie successiva
                                val nextSeriesIndex = serieNumber
                                Log.d("WorkoutHistory", "Preparazione serie successiva: ${nextSeriesIndex}")
                                updateUIForNewSeries(exerciseId, nextSeriesIndex)
                            }

                            // Verifica se l'esercizio fa parte di un superset
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
     * Gestisce la navigazione automatica nei superset
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