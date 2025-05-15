package com.fitgymtrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.models.CompletedSeriesData
import com.fitgymtrack.app.models.WorkoutHistory
import com.fitgymtrack.app.repository.WorkoutHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import retrofit2.HttpException

class WorkoutHistoryViewModel(
    private val repository: WorkoutHistoryRepository = WorkoutHistoryRepository()
) : ViewModel() {

    // State for workout history list
    private val _workoutHistoryState = MutableStateFlow<WorkoutHistoryState>(WorkoutHistoryState.Idle)
    val workoutHistoryState: StateFlow<WorkoutHistoryState> = _workoutHistoryState.asStateFlow()

    // State for workout detail
    private val _workoutDetailState = MutableStateFlow<WorkoutDetailState>(WorkoutDetailState.Idle)
    val workoutDetailState: StateFlow<WorkoutDetailState> = _workoutDetailState.asStateFlow()

    // State for delete operations
    private val _deleteState = MutableStateFlow<OperationState>(OperationState.Idle)
    val deleteState: StateFlow<OperationState> = _deleteState.asStateFlow()

    // State for update operations
    private val _updateState = MutableStateFlow<OperationState>(OperationState.Idle)
    val updateState: StateFlow<OperationState> = _updateState.asStateFlow()

    // Currently selected workout history
    private val _selectedWorkout = MutableStateFlow<WorkoutHistory?>(null)
    val selectedWorkout = _selectedWorkout.asStateFlow()

    /**
     * Load all workout history for a user
     */
    fun loadWorkoutHistory(userId: Int) {
        _workoutHistoryState.value = WorkoutHistoryState.Loading

        viewModelScope.launch {
            try {
                val result = repository.getWorkoutHistory(userId)

                result.fold(
                    onSuccess = { history ->
                        _workoutHistoryState.value = WorkoutHistoryState.Success(history)
                    },
                    onFailure = { e ->
                        val errorMessage = when (e) {
                            is IOException -> "Impossibile connettersi al server. Verifica la tua connessione."
                            is HttpException -> "Errore dal server: ${e.code()}"
                            else -> e.message ?: "Si è verificato un errore sconosciuto"
                        }
                        _workoutHistoryState.value = WorkoutHistoryState.Error(errorMessage)
                    }
                )
            } catch (e: Exception) {
                _workoutHistoryState.value = WorkoutHistoryState.Error(
                    e.message ?: "Si è verificato un errore sconosciuto"
                )
            }
        }
    }

    /**
     * Load detail for a specific workout
     */
    fun loadWorkoutDetail(workoutId: Int) {
        _workoutDetailState.value = WorkoutDetailState.Loading

        viewModelScope.launch {
            try {
                val result = repository.getWorkoutSeriesDetail(workoutId)

                result.fold(
                    onSuccess = { series ->
                        _workoutDetailState.value = WorkoutDetailState.Success(series)
                    },
                    onFailure = { e ->
                        val errorMessage = when (e) {
                            is IOException -> "Impossibile connettersi al server. Verifica la tua connessione."
                            is HttpException -> "Errore dal server: ${e.code()}"
                            else -> e.message ?: "Si è verificato un errore sconosciuto"
                        }
                        _workoutDetailState.value = WorkoutDetailState.Error(errorMessage)
                    }
                )
            } catch (e: Exception) {
                _workoutDetailState.value = WorkoutDetailState.Error(
                    e.message ?: "Si è verificato un errore sconosciuto"
                )
            }
        }
    }

    /**
     * Set the selected workout
     */
    fun selectWorkout(workout: WorkoutHistory) {
        // If the workout has no ID or it's 0, it means we're deselecting
        if (workout.id == null || workout.id == 0) {
            _selectedWorkout.value = null
            _workoutDetailState.value = WorkoutDetailState.Idle
        } else {
            _selectedWorkout.value = workout
            // Load the details for this workout
            workout.id.let { loadWorkoutDetail(it) }
        }
    }

    /**
     * Delete a specific series from a workout
     */
    fun deleteCompletedSeries(seriesId: String) {
        _deleteState.value = OperationState.Loading

        viewModelScope.launch {
            try {
                val result = repository.deleteCompletedSeries(seriesId)

                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            _deleteState.value = OperationState.Success(response.message)
                            // Refresh the detail data
                            _selectedWorkout.value?.id?.let { loadWorkoutDetail(it) }
                        } else {
                            _deleteState.value = OperationState.Error(response.message)
                        }
                    },
                    onFailure = { e ->
                        _deleteState.value = OperationState.Error(
                            e.message ?: "Si è verificato un errore durante l'eliminazione"
                        )
                    }
                )
            } catch (e: Exception) {
                _deleteState.value = OperationState.Error(
                    e.message ?: "Si è verificato un errore durante l'eliminazione"
                )
            }
        }
    }

    /**
     * Delete an entire workout
     */
    fun deleteWorkout(workoutId: Int, userId: Int) {
        _deleteState.value = OperationState.Loading

        viewModelScope.launch {
            try {
                val result = repository.deleteWorkout(workoutId)

                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            _deleteState.value = OperationState.Success(response.message)
                            // Clear selected workout
                            _selectedWorkout.value = null
                            // Refresh the list
                            loadWorkoutHistory(userId)
                        } else {
                            _deleteState.value = OperationState.Error(response.message)
                        }
                    },
                    onFailure = { e ->
                        _deleteState.value = OperationState.Error(
                            e.message ?: "Si è verificato un errore durante l'eliminazione"
                        )
                    }
                )
            } catch (e: Exception) {
                _deleteState.value = OperationState.Error(
                    e.message ?: "Si è verificato un errore durante l'eliminazione"
                )
            }
        }
    }

    /**
     * Update a completed series
     */
    fun updateCompletedSeries(seriesId: String, weight: Float, reps: Int) {
        _updateState.value = OperationState.Loading

        viewModelScope.launch {
            try {
                val result = repository.updateCompletedSeries(seriesId, weight, reps)

                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            _updateState.value = OperationState.Success(response.message)
                            // Refresh the detail data
                            _selectedWorkout.value?.id?.let { loadWorkoutDetail(it) }
                        } else {
                            _updateState.value = OperationState.Error(response.message)
                        }
                    },
                    onFailure = { e ->
                        _updateState.value = OperationState.Error(
                            e.message ?: "Si è verificato un errore durante l'aggiornamento"
                        )
                    }
                )
            } catch (e: Exception) {
                _updateState.value = OperationState.Error(
                    e.message ?: "Si è verificato un errore durante l'aggiornamento"
                )
            }
        }
    }

    /**
     * Reset states
     */
    fun resetDeleteState() {
        _deleteState.value = OperationState.Idle
    }

    fun resetUpdateState() {
        _updateState.value = OperationState.Idle
    }

    fun resetDetailState() {
        _workoutDetailState.value = WorkoutDetailState.Idle
    }

    /**
     * State classes
     */
    sealed class WorkoutHistoryState {
        object Idle : WorkoutHistoryState()
        object Loading : WorkoutHistoryState()
        data class Success(val workouts: List<WorkoutHistory>) : WorkoutHistoryState()
        data class Error(val message: String) : WorkoutHistoryState()
    }

    sealed class WorkoutDetailState {
        object Idle : WorkoutDetailState()
        object Loading : WorkoutDetailState()
        data class Success(val series: List<CompletedSeriesData>) : WorkoutDetailState()
        data class Error(val message: String) : WorkoutDetailState()
    }

    sealed class OperationState {
        object Idle : OperationState()
        object Loading : OperationState()
        data class Success(val message: String) : OperationState()
        data class Error(val message: String) : OperationState()
    }
}