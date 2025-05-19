package com.fitgymtrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.api.ExerciseItem
import com.fitgymtrack.app.models.WorkoutExerciseToRemove
import com.fitgymtrack.app.models.WorkoutPlan
import com.fitgymtrack.app.repository.WorkoutRepository
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.utils.SubscriptionLimitChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.fitgymtrack.app.models.WorkoutExercise

class WorkoutViewModel(
    private val repository: WorkoutRepository = WorkoutRepository()
) : ViewModel() {

    // Stato per la lista delle schede
    private val _workoutPlansState = MutableStateFlow<WorkoutPlansState>(WorkoutPlansState.Loading)
    val workoutPlansState: StateFlow<WorkoutPlansState> = _workoutPlansState.asStateFlow()

    // Lista attuale delle schede
    private val _workoutPlans = MutableStateFlow<List<WorkoutPlan>>(emptyList())
    val workoutPlans = _workoutPlans.asStateFlow()

    // Stato per l'operazione di eliminazione
    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    // Stato per i dettagli di una scheda (esercizi)
    private val _workoutDetailsState = MutableStateFlow<WorkoutDetailsState>(WorkoutDetailsState.Idle)
    val workoutDetailsState: StateFlow<WorkoutDetailsState> = _workoutDetailsState.asStateFlow()

    // Stato per il controllo dei limiti
    private val _limitsState = MutableStateFlow<LimitsState>(LimitsState.Initial)
    val limitsState: StateFlow<LimitsState> = _limitsState.asStateFlow()

    // Dettagli della scheda selezionata
    private val _selectedWorkoutExercises = MutableStateFlow<List<WorkoutExercise>>(emptyList())
    val selectedWorkoutExercises = _selectedWorkoutExercises.asStateFlow()

    // ID della scheda espansa
    private val _expandedWorkoutId = MutableStateFlow<Int?>(null)
    val expandedWorkoutId = _expandedWorkoutId.asStateFlow()

    /**
     * Controlla i limiti prima di creare una scheda
     */
    fun checkLimitsBeforeCreate() {
        _limitsState.value = LimitsState.Loading

        viewModelScope.launch {
            try {
                val (limitReached, currentCount, maxAllowed) = SubscriptionLimitChecker.canCreateWorkout()

                if (limitReached) {
                    _limitsState.value = LimitsState.LimitReached(currentCount, maxAllowed)
                } else {
                    _limitsState.value = LimitsState.CanProceed
                }
            } catch (e: Exception) {
                // In caso di errore, permettiamo la creazione
                _limitsState.value = LimitsState.CanProceed
            }
        }
    }

    /**
     * Carica tutte le schede dell'utente
     */
    fun loadWorkoutPlans(sessionManager: SessionManager) {
        _workoutPlansState.value = WorkoutPlansState.Loading

        viewModelScope.launch {
            try {
                val userData = sessionManager.getUserData().first()
                if (userData != null) {
                    val result = repository.getWorkoutPlans(userData.id)

                    result.fold(
                        onSuccess = { plans ->
                            _workoutPlans.value = plans
                            _workoutPlansState.value = WorkoutPlansState.Success
                        },
                        onFailure = { e ->
                            _workoutPlansState.value = WorkoutPlansState.Error(e.message ?: "Errore nel caricamento delle schede")
                        }
                    )
                } else {
                    _workoutPlansState.value = WorkoutPlansState.Error("Utente non autenticato")
                }
            } catch (e: Exception) {
                _workoutPlansState.value = WorkoutPlansState.Error(e.message ?: "Si è verificato un errore")
            }
        }
    }

    /**
     * Elimina una scheda
     */
    fun deleteWorkoutPlan(schedaId: Int) {
        _deleteState.value = DeleteState.Loading

        viewModelScope.launch {
            try {
                val result = repository.deleteWorkoutPlan(schedaId)

                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            // Rimuovi la scheda dalla lista
                            _workoutPlans.value = _workoutPlans.value.filter { it.id != schedaId }
                            _deleteState.value = DeleteState.Success(response.message)
                        } else {
                            _deleteState.value = DeleteState.Error(response.message)
                        }
                    },
                    onFailure = { e ->
                        _deleteState.value = DeleteState.Error(e.message ?: "Errore durante l'eliminazione")
                    }
                )
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Error(e.message ?: "Si è verificato un errore")
            }
        }
    }

    /**
     * Carica gli esercizi di una scheda
     */
    fun loadWorkoutExercises(schedaId: Int) {
        if (_expandedWorkoutId.value == schedaId) {
            // Se la scheda è già espansa, la chiudiamo
            _expandedWorkoutId.value = null
            return
        }

        _workoutDetailsState.value = WorkoutDetailsState.Loading

        viewModelScope.launch {
            try {
                val result = repository.getWorkoutExercises(schedaId)

                result.fold(
                    onSuccess = { exercises ->
                        _selectedWorkoutExercises.value = exercises
                        _expandedWorkoutId.value = schedaId
                        _workoutDetailsState.value = WorkoutDetailsState.Success
                    },
                    onFailure = { e ->
                        _workoutDetailsState.value = WorkoutDetailsState.Error(e.message ?: "Errore nel caricamento degli esercizi")
                    }
                )
            } catch (e: Exception) {
                _workoutDetailsState.value = WorkoutDetailsState.Error(e.message ?: "Si è verificato un errore")
            }
        }
    }

    /**
     * Resetta lo stato di eliminazione
     */
    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }

    /**
     * Resetta lo stato dei dettagli
     */
    fun resetWorkoutDetailsState() {
        _workoutDetailsState.value = WorkoutDetailsState.Idle
    }

    /**
     * Resetta lo stato dei limiti
     */
    fun resetLimitsState() {
        _limitsState.value = LimitsState.Initial
    }

    // Stati per le schede
    sealed class WorkoutPlansState {
        object Loading : WorkoutPlansState()
        object Success : WorkoutPlansState()
        data class Error(val message: String) : WorkoutPlansState()
    }

    // Stati per l'eliminazione
    sealed class DeleteState {
        object Idle : DeleteState()
        object Loading : DeleteState()
        data class Success(val message: String) : DeleteState()
        data class Error(val message: String) : DeleteState()
    }

    // Stati per i dettagli della scheda
    sealed class WorkoutDetailsState {
        object Idle : WorkoutDetailsState()
        object Loading : WorkoutDetailsState()
        object Success : WorkoutDetailsState()
        data class Error(val message: String) : WorkoutDetailsState()
    }

    // Stati per il controllo dei limiti
    sealed class LimitsState {
        object Initial : LimitsState()
        object Loading : LimitsState()
        object CanProceed : LimitsState()
        data class LimitReached(val currentCount: Int, val maxAllowed: Int?) : LimitsState()
    }
}