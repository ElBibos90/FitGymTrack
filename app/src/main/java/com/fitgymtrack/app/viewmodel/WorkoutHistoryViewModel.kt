package com.fitgymtrack.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.models.*
import com.fitgymtrack.app.repository.WorkoutHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkoutHistoryViewModel : ViewModel() {
    private val repository = WorkoutHistoryRepository()

    // Stati per la cronologia degli allenamenti
    private val _workoutHistoryState = MutableStateFlow<WorkoutHistoryState>(WorkoutHistoryState.Idle)
    val workoutHistoryState: StateFlow<WorkoutHistoryState> = _workoutHistoryState.asStateFlow()

    // Lista degli allenamenti
    private val _workoutHistory = MutableStateFlow<List<WorkoutHistory>>(emptyList())
    val workoutHistory: StateFlow<List<WorkoutHistory>> = _workoutHistory.asStateFlow()

    // Stati per i dettagli di un allenamento specifico
    private val _workoutDetailState = MutableStateFlow<WorkoutDetailState>(WorkoutDetailState.Idle)
    val workoutDetailState: StateFlow<WorkoutDetailState> = _workoutDetailState.asStateFlow()

    // Stati per le operazioni di update
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    // Allenamento selezionato
    private val _selectedWorkout = MutableStateFlow<WorkoutHistory?>(null)
    val selectedWorkout: StateFlow<WorkoutHistory?> = _selectedWorkout.asStateFlow()

    // Stati per i dettagli delle serie
    private val _seriesDetailsState = MutableStateFlow<SeriesDetailsState>(SeriesDetailsState.Idle)
    val seriesDetailsState: StateFlow<SeriesDetailsState> = _seriesDetailsState.asStateFlow()

    // Dettagli delle serie per allenamento
    private val _seriesDetails = MutableStateFlow<Map<Int, List<CompletedSeriesData>>>(emptyMap())
    val seriesDetails: StateFlow<Map<Int, List<CompletedSeriesData>>> = _seriesDetails.asStateFlow()

    // Stato per le operazioni di eliminazione
    private val _deleteState = MutableStateFlow<OperationState>(OperationState.Idle)
    val deleteState: StateFlow<OperationState> = _deleteState.asStateFlow()

    /**
     * Carica la cronologia degli allenamenti per un utente
     */
    fun loadWorkoutHistory(userId: Int) {
        _workoutHistoryState.value = WorkoutHistoryState.Loading

        viewModelScope.launch {
            try {
                val result = repository.getWorkoutHistory(userId)

                result.fold(
                    onSuccess = { workouts ->
                        val workoutList = workouts.mapNotNull { workout ->
                            try {
                                WorkoutHistory.fromMap(workout)
                            } catch (e: Exception) {
                                Log.e("WorkoutHistoryViewModel", "Errore parsing workout: ${e.message}")
                                null
                            }
                        }
                        _workoutHistory.value = workoutList
                        _workoutHistoryState.value = WorkoutHistoryState.Success
                    },
                    onFailure = { e ->
                        _workoutHistoryState.value = WorkoutHistoryState.Error(
                            e.message ?: "Errore nel caricamento della cronologia allenamenti"
                        )
                    }
                )
            } catch (e: Exception) {
                _workoutHistoryState.value = WorkoutHistoryState.Error(
                    e.message ?: "Errore nel caricamento della cronologia allenamenti"
                )
            }
        }
    }

    /**
     * Carica i dettagli di un allenamento specifico
     */
    fun loadWorkoutDetail(workoutId: Int) {
        _workoutDetailState.value = WorkoutDetailState.Loading(workoutId)

        viewModelScope.launch {
            try {
                // Questo metodo dovrebbe essere implementato con la logica appropriata
                // per caricare i dettagli di un allenamento specifico
                // Per ora facciamo una semplice ricerca nell'elenco degli allenamenti
                val workout = _workoutHistory.value.find { it.id == workoutId }

                if (workout != null) {
                    _workoutDetailState.value = WorkoutDetailState.Success(workout)
                } else {
                    _workoutDetailState.value = WorkoutDetailState.Error(workoutId, "Allenamento non trovato")
                }
            } catch (e: Exception) {
                _workoutDetailState.value = WorkoutDetailState.Error(
                    workoutId,
                    e.message ?: "Errore nel caricamento dei dettagli dell'allenamento"
                )
            }
        }
    }

    /**
     * Seleziona un allenamento
     */
    fun selectWorkout(workout: WorkoutHistory) {
        _selectedWorkout.value = workout
        _workoutDetailState.value = WorkoutDetailState.Idle

        // Carica i dettagli dell'allenamento
        loadWorkoutDetail(workout.id)
    }

    /**
     * Carica i dettagli delle serie per un allenamento specifico
     */
    fun loadSeriesDetails(workoutId: Int) {
        // Se i dettagli sono già stati caricati, non ricaricarli
        if (_seriesDetails.value.containsKey(workoutId)) {
            return
        }

        _seriesDetailsState.value = SeriesDetailsState.Loading(workoutId)

        viewModelScope.launch {
            try {
                val result = repository.getWorkoutSeriesDetail(workoutId)

                result.fold(
                    onSuccess = { series ->
                        // Aggiorna la mappa dei dettagli
                        val currentDetails = _seriesDetails.value.toMutableMap()
                        currentDetails[workoutId] = series
                        _seriesDetails.value = currentDetails

                        _seriesDetailsState.value = SeriesDetailsState.Success(workoutId)

                        // Aggiorna anche lo stato dei dettagli dell'allenamento
                        // se l'allenamento selezionato è quello corrente
                        _selectedWorkout.value?.let {
                            if (it.id == workoutId) {
                                loadWorkoutDetail(workoutId)
                            }
                        }
                    },
                    onFailure = { e ->
                        _seriesDetailsState.value = SeriesDetailsState.Error(
                            workoutId,
                            e.message ?: "Errore nel caricamento dei dettagli delle serie"
                        )
                    }
                )
            } catch (e: Exception) {
                _seriesDetailsState.value = SeriesDetailsState.Error(
                    workoutId,
                    e.message ?: "Errore nel caricamento dei dettagli delle serie"
                )
            }
        }
    }

    /**
     * Elimina una singola serie
     */
    fun deleteCompletedSeries(seriesId: String, workoutId: Int) {
        _deleteState.value = OperationState.Loading

        viewModelScope.launch {
            try {
                val result = repository.deleteCompletedSeries(seriesId)

                result.fold(
                    onSuccess = { success ->
                        if (success) {
                            // Rimuovi la serie dalla mappa locale
                            val currentDetails = _seriesDetails.value.toMutableMap()
                            currentDetails[workoutId] = currentDetails[workoutId]?.filter { it.id != seriesId } ?: emptyList()
                            _seriesDetails.value = currentDetails

                            _deleteState.value = OperationState.Success("Serie eliminata con successo")

                            // Aggiorna i dettagli dell'allenamento se necessario
                            _selectedWorkout.value?.let {
                                if (it.id == workoutId) {
                                    loadWorkoutDetail(workoutId)
                                }
                            }
                        } else {
                            _deleteState.value = OperationState.Error("Errore nell'eliminazione della serie")
                        }
                    },
                    onFailure = { e ->
                        _deleteState.value = OperationState.Error(
                            e.message ?: "Errore nell'eliminazione della serie"
                        )
                    }
                )
            } catch (e: Exception) {
                _deleteState.value = OperationState.Error(
                    e.message ?: "Errore nell'eliminazione della serie"
                )
            }
        }
    }

    /**
     * Aggiorna una serie completata
     */
    fun updateCompletedSeries(
        seriesId: String,
        workoutId: Int,
        weight: Float,
        reps: Int,
        recoveryTime: Int? = null,
        notes: String? = null
    ) {
        _updateState.value = UpdateState.Loading

        viewModelScope.launch {
            try {
                val result = repository.updateCompletedSeries(seriesId, weight, reps, recoveryTime, notes)

                result.fold(
                    onSuccess = { success ->
                        if (success) {
                            _updateState.value = UpdateState.Success("Serie aggiornata con successo")

                            // Ricarica i dettagli delle serie
                            _selectedWorkout.value?.let {
                                if (it.id == workoutId) {
                                    loadWorkoutDetail(workoutId)
                                }
                            }
                        } else {
                            _updateState.value = UpdateState.Error("Errore nell'aggiornamento della serie")
                        }
                    },
                    onFailure = { e ->
                        _updateState.value = UpdateState.Error(
                            e.message ?: "Errore nell'aggiornamento della serie"
                        )
                    }
                )
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(
                    e.message ?: "Errore nell'aggiornamento della serie"
                )
            }
        }
    }

    /**
     * Elimina un intero allenamento
     */
    fun deleteWorkout(workoutId: Int) {
        _deleteState.value = OperationState.Loading

        viewModelScope.launch {
            try {
                val result = repository.deleteWorkout(workoutId)

                result.fold(
                    onSuccess = { success ->
                        if (success) {
                            // Rimuovi l'allenamento dalla lista locale
                            _workoutHistory.value = _workoutHistory.value.filter { it.id != workoutId }

                            // Rimuovi i dettagli delle serie
                            val currentDetails = _seriesDetails.value.toMutableMap()
                            currentDetails.remove(workoutId)
                            _seriesDetails.value = currentDetails

                            // Se era l'allenamento selezionato, deselezionalo
                            if (_selectedWorkout.value?.id == workoutId) {
                                _selectedWorkout.value = null
                                _workoutDetailState.value = WorkoutDetailState.Idle
                            }

                            _deleteState.value = OperationState.Success("Allenamento eliminato con successo")
                        } else {
                            _deleteState.value = OperationState.Error("Errore nell'eliminazione dell'allenamento")
                        }
                    },
                    onFailure = { e ->
                        _deleteState.value = OperationState.Error(
                            e.message ?: "Errore nell'eliminazione dell'allenamento"
                        )
                    }
                )
            } catch (e: Exception) {
                _deleteState.value = OperationState.Error(
                    e.message ?: "Errore nell'eliminazione dell'allenamento"
                )
            }
        }
    }

    /**
     * Resetta lo stato della cronologia allenamenti
     */
    fun resetWorkoutHistoryState() {
        _workoutHistoryState.value = WorkoutHistoryState.Idle
    }

    /**
     * Resetta lo stato dei dettagli dell'allenamento
     */
    fun resetWorkoutDetailState() {
        _workoutDetailState.value = WorkoutDetailState.Idle
    }

    /**
     * Resetta lo stato delle operazioni di aggiornamento
     */
    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    /**
     * Resetta lo stato dei dettagli delle serie
     */
    fun resetSeriesDetailsState() {
        _seriesDetailsState.value = SeriesDetailsState.Idle
    }

    /**
     * Resetta lo stato delle operazioni di eliminazione
     */
    fun resetDeleteState() {
        _deleteState.value = OperationState.Idle
    }

    /**
     * Stato della cronologia allenamenti
     */
    sealed class WorkoutHistoryState {
        object Idle : WorkoutHistoryState()
        object Loading : WorkoutHistoryState()
        object Success : WorkoutHistoryState()
        data class Error(val message: String) : WorkoutHistoryState()
    }

    /**
     * Stato dei dettagli dell'allenamento
     */
    sealed class WorkoutDetailState {
        object Idle : WorkoutDetailState()
        data class Loading(val workoutId: Int) : WorkoutDetailState()
        data class Success(val workout: WorkoutHistory) : WorkoutDetailState()
        data class Error(val workoutId: Int, val message: String) : WorkoutDetailState()
    }

    /**
     * Stato dei dettagli delle serie
     */
    sealed class SeriesDetailsState {
        object Idle : SeriesDetailsState()
        data class Loading(val workoutId: Int) : SeriesDetailsState()
        data class Success(val workoutId: Int) : SeriesDetailsState()
        data class Error(val workoutId: Int, val message: String) : SeriesDetailsState()
    }

    /**
     * Stato delle operazioni di aggiornamento
     */
    sealed class UpdateState {
        object Idle : UpdateState()
        object Loading : UpdateState()
        data class Success(val message: String) : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    /**
     * Stato delle operazioni generiche
     */
    sealed class OperationState {
        object Idle : OperationState()
        object Loading : OperationState()
        data class Success(val message: String) : OperationState()
        data class Error(val message: String) : OperationState()
    }
}