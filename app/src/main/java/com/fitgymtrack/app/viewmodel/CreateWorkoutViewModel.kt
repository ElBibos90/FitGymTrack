package com.fitgymtrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.api.ExerciseItem
import com.fitgymtrack.app.models.*
import com.fitgymtrack.app.repository.WorkoutRepository
import com.fitgymtrack.app.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CreateWorkoutViewModel(
    private val repository: WorkoutRepository = WorkoutRepository()
) : ViewModel() {

    // Stato del caricamento
    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    // Dati della scheda
    private val _workoutName = MutableStateFlow("")
    val workoutName = _workoutName.asStateFlow()

    private val _workoutDescription = MutableStateFlow("")
    val workoutDescription = _workoutDescription.asStateFlow()

    // Esercizi selezionati
    private val _selectedExercises = MutableStateFlow<List<WorkoutExercise>>(emptyList())
    val selectedExercises = _selectedExercises.asStateFlow()

    // Esercizi disponibili
    private val _availableExercises = MutableStateFlow<List<ExerciseItem>>(emptyList())
    val availableExercises = _availableExercises.asStateFlow()

    // Stato del caricamento esercizi
    private val _exercisesLoadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val exercisesLoadingState: StateFlow<LoadingState> = _exercisesLoadingState.asStateFlow()

    // Aggiorna il nome della scheda
    fun updateWorkoutName(name: String) {
        _workoutName.value = name
    }

    // Aggiorna la descrizione della scheda
    fun updateWorkoutDescription(description: String) {
        _workoutDescription.value = description
    }

    // Carica gli esercizi disponibili
    fun loadAvailableExercises(userId: Int) {
        _exercisesLoadingState.value = LoadingState.Loading

        viewModelScope.launch {
            try {
                val result = repository.getAvailableExercises(userId)

                result.fold(
                    onSuccess = { exercises ->
                        _availableExercises.value = exercises
                        _exercisesLoadingState.value = LoadingState.Success
                    },
                    onFailure = { e ->
                        _exercisesLoadingState.value = LoadingState.Error(e.message ?: "Errore nel caricamento degli esercizi")
                    }
                )
            } catch (e: Exception) {
                _exercisesLoadingState.value = LoadingState.Error(e.message ?: "Si è verificato un errore")
            }
        }
    }

    // Aggiunge un esercizio alla scheda - MODIFICATO per usare la factory function
    fun addExercise(exercise: ExerciseItem) {
        val newExercise = createWorkoutExercise(
            id = exercise.id,
            nome = exercise.nome,
            gruppoMuscolare = exercise.gruppo_muscolare,
            attrezzatura = exercise.attrezzatura,
            descrizione = exercise.descrizione,
            serie = exercise.serie_default ?: 3,
            ripetizioni = exercise.ripetizioni_default ?: 10,
            peso = exercise.peso_default ?: 0.0,
            ordine = _selectedExercises.value.size + 1,
            isIsometric = exercise.is_isometric
        )

        _selectedExercises.value = _selectedExercises.value + newExercise
    }

    // Rimuove un esercizio dalla scheda - MODIFICATO per usare safeCopy
    fun removeExercise(index: Int) {
        val updatedList = _selectedExercises.value.toMutableList()
        updatedList.removeAt(index)

        // Aggiorna l'ordine degli esercizi
        _selectedExercises.value = updatedList.mapIndexed { i, exercise ->
            exercise.safeCopy(ordine = i + 1)
        }
    }

    // Aggiorna i dettagli di un esercizio - MODIFICATO per usare safeCopy
    fun updateExerciseDetails(index: Int, updatedExercise: WorkoutExercise) {
        val updatedList = _selectedExercises.value.toMutableList()
        updatedList[index] = updatedExercise
        _selectedExercises.value = updatedList
    }

    // Sposta un esercizio su - MODIFICATO per usare safeCopy
    fun moveExerciseUp(index: Int) {
        if (index <= 0 || index >= _selectedExercises.value.size) return

        val updatedList = _selectedExercises.value.toMutableList()
        val temp = updatedList[index]
        updatedList[index] = updatedList[index - 1]
        updatedList[index - 1] = temp

        // Aggiorna l'ordine degli esercizi
        _selectedExercises.value = updatedList.mapIndexed { i, exercise ->
            exercise.safeCopy(ordine = i + 1)
        }
    }

    // Sposta un esercizio giù - MODIFICATO per usare safeCopy
    fun moveExerciseDown(index: Int) {
        if (index < 0 || index >= _selectedExercises.value.size - 1) return

        val updatedList = _selectedExercises.value.toMutableList()
        val temp = updatedList[index]
        updatedList[index] = updatedList[index + 1]
        updatedList[index + 1] = temp

        // Aggiorna l'ordine degli esercizi
        _selectedExercises.value = updatedList.mapIndexed { i, exercise ->
            exercise.safeCopy(ordine = i + 1)
        }
    }

    // Crea una nuova scheda
    fun createWorkout(userId: Int) {
        if (_workoutName.value.isBlank()) {
            _loadingState.value = LoadingState.Error("Il nome della scheda è obbligatorio")
            return
        }

        if (_selectedExercises.value.isEmpty()) {
            _loadingState.value = LoadingState.Error("Aggiungi almeno un esercizio alla scheda")
            return
        }

        _loadingState.value = LoadingState.Loading

        val exerciseRequests = _selectedExercises.value.map { exercise ->
            WorkoutExerciseRequest(
                id = exercise.id,
                serie = exercise.serie,
                ripetizioni = exercise.ripetizioni,
                peso = exercise.peso,
                ordine = exercise.ordine,
                tempo_recupero = exercise.tempoRecupero,
                note = exercise.note,
                set_type = exercise.setType,
                linked_to_previous = if (exercise.linkedToPrevious) 1 else 0
            )
        }

        val createRequest = CreateWorkoutPlanRequest(
            user_id = userId,
            nome = _workoutName.value,
            descrizione = _workoutDescription.value,
            esercizi = exerciseRequests
        )

        viewModelScope.launch {
            try {
                val result = repository.createWorkoutPlan(createRequest)

                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            _loadingState.value = LoadingState.Success
                        } else {
                            _loadingState.value = LoadingState.Error(response.message)
                        }
                    },
                    onFailure = { e ->
                        _loadingState.value = LoadingState.Error(e.message ?: "Errore durante il salvataggio")
                    }
                )
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "Si è verificato un errore")
            }
        }
    }

    // Resetta lo stato di caricamento
    fun resetLoadingState() {
        _loadingState.value = LoadingState.Idle
    }

    // Stato di caricamento
    sealed class LoadingState {
        object Idle : LoadingState()
        object Loading : LoadingState()
        object Success : LoadingState()
        data class Error(val message: String) : LoadingState()
    }
}