package com.fitgymtrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.models.*
import com.fitgymtrack.app.repository.UserExerciseRepository
import com.fitgymtrack.app.utils.SubscriptionLimitChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import retrofit2.HttpException

/**
 * ViewModel per la gestione degli esercizi personalizzati
 */
class UserExerciseViewModel : ViewModel() {
    private val repository = UserExerciseRepository()

    // Stato per l'elenco degli esercizi
    private val _exercisesState = MutableStateFlow<ExercisesState>(ExercisesState.Idle)
    val exercisesState: StateFlow<ExercisesState> = _exercisesState.asStateFlow()

    // Stato per la creazione di un esercizio
    private val _createState = MutableStateFlow<OperationState>(OperationState.Idle)
    val createState: StateFlow<OperationState> = _createState.asStateFlow()

    // Stato per l'aggiornamento di un esercizio
    private val _updateState = MutableStateFlow<OperationState>(OperationState.Idle)
    val updateState: StateFlow<OperationState> = _updateState.asStateFlow()

    // Stato per l'eliminazione di un esercizio
    private val _deleteState = MutableStateFlow<OperationState>(OperationState.Idle)
    val deleteState: StateFlow<OperationState> = _deleteState.asStateFlow()

    // Stato per il controllo dei limiti
    private val _limitsState = MutableStateFlow<LimitsState>(LimitsState.Initial)
    val limitsState: StateFlow<LimitsState> = _limitsState.asStateFlow()

    // Cache degli esercizi
    private val _exercises = MutableStateFlow<List<UserExercise>>(emptyList())
    val exercises = _exercises.asStateFlow()

    // Recupera tutti gli esercizi dell'utente
    fun loadUserExercises(userId: Int) {
        _exercisesState.value = ExercisesState.Loading

        viewModelScope.launch {
            val result = repository.getUserExercises(userId)

            result.fold(
                onSuccess = { exercises ->
                    _exercises.value = exercises
                    _exercisesState.value = ExercisesState.Success(exercises)
                },
                onFailure = { e ->
                    val errorMessage = when (e) {
                        is IOException -> "Impossibile connettersi al server. Verifica la tua connessione."
                        is HttpException -> "Errore dal server: ${e.code()}"
                        else -> e.message ?: "Si è verificato un errore sconosciuto"
                    }
                    _exercisesState.value = ExercisesState.Error(errorMessage)
                }
            )
        }
    }

    // Controlla i limiti prima di creare un esercizio
    fun checkLimitsBeforeCreate() {
        _limitsState.value = LimitsState.Loading

        viewModelScope.launch {
            try {
                val (limitReached, currentCount, maxAllowed) = SubscriptionLimitChecker.canCreateCustomExercise()

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

    // Crea un nuovo esercizio
    fun createExercise(
        nome: String,
        gruppoMuscolare: String,
        descrizione: String?,
        attrezzatura: String?,
        isIsometric: Boolean,
        userId: Int
    ) {
        if (nome.isBlank() || gruppoMuscolare.isBlank()) {
            _createState.value = OperationState.Error("Nome e gruppo muscolare sono obbligatori")
            return
        }

        _createState.value = OperationState.Loading

        viewModelScope.launch {
            val request = CreateUserExerciseRequest(
                nome = nome,
                gruppoMuscolare = gruppoMuscolare,
                descrizione = descrizione?.takeIf { it.isNotBlank() },
                attrezzatura = attrezzatura?.takeIf { it.isNotBlank() },
                isIsometric = isIsometric,
                createdByUserId = userId,
                status = "pending_review"
            )

            val result = repository.createUserExercise(request)

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        _createState.value = OperationState.Success(response.message)
                        // Ricarica gli esercizi dopo la creazione
                        loadUserExercises(userId)
                    } else {
                        _createState.value = OperationState.Error(response.message)
                    }
                },
                onFailure = { e ->
                    _createState.value = OperationState.Error(e.message ?: "Errore durante la creazione dell'esercizio")
                }
            )
        }
    }

    // Aggiorna un esercizio esistente
    fun updateExercise(
        id: Int,
        nome: String,
        gruppoMuscolare: String,
        descrizione: String?,
        attrezzatura: String?,
        isIsometric: Boolean,
        userId: Int
    ) {
        if (nome.isBlank() || gruppoMuscolare.isBlank()) {
            _updateState.value = OperationState.Error("Nome e gruppo muscolare sono obbligatori")
            return
        }

        _updateState.value = OperationState.Loading

        viewModelScope.launch {
            val request = UpdateUserExerciseRequest(
                id = id,
                nome = nome,
                gruppoMuscolare = gruppoMuscolare,
                descrizione = descrizione?.takeIf { it.isNotBlank() },
                attrezzatura = attrezzatura?.takeIf { it.isNotBlank() },
                isIsometric = isIsometric,
                userId = userId
            )

            val result = repository.updateUserExercise(request)

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        _updateState.value = OperationState.Success(response.message)
                        // Ricarica gli esercizi dopo l'aggiornamento
                        loadUserExercises(userId)
                    } else {
                        _updateState.value = OperationState.Error(response.message)
                    }
                },
                onFailure = { e ->
                    _updateState.value = OperationState.Error(e.message ?: "Errore durante l'aggiornamento dell'esercizio")
                }
            )
        }
    }

    // Elimina un esercizio
    fun deleteExercise(exerciseId: Int, userId: Int) {
        _deleteState.value = OperationState.Loading

        viewModelScope.launch {
            val result = repository.deleteUserExercise(exerciseId, userId)

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        _deleteState.value = OperationState.Success(response.message)
                        // Ricarica gli esercizi dopo l'eliminazione
                        loadUserExercises(userId)
                    } else {
                        _deleteState.value = OperationState.Error(response.message)
                    }
                },
                onFailure = { e ->
                    _deleteState.value = OperationState.Error(e.message ?: "Errore durante l'eliminazione dell'esercizio")
                }
            )
        }
    }

    // Resetta gli stati
    fun resetCreateState() {
        _createState.value = OperationState.Idle
    }

    fun resetUpdateState() {
        _updateState.value = OperationState.Idle
    }

    fun resetDeleteState() {
        _deleteState.value = OperationState.Idle
    }

    fun resetExercisesState() {
        _exercisesState.value = ExercisesState.Idle
    }

    // Resetta lo stato dei limiti
    fun resetLimitsState() {
        _limitsState.value = LimitsState.Initial
    }

    // Definizione degli stati possibili
    sealed class ExercisesState {
        object Idle : ExercisesState()
        object Loading : ExercisesState()
        data class Success(val exercises: List<UserExercise>) : ExercisesState()
        data class Error(val message: String) : ExercisesState()
    }

    sealed class OperationState {
        object Idle : OperationState()
        object Loading : OperationState()
        data class Success(val message: String) : OperationState()
        data class Error(val message: String) : OperationState()
    }

    // Stati per il controllo dei limiti
    sealed class LimitsState {
        object Initial : LimitsState()
        object Loading : LimitsState()
        object CanProceed : LimitsState()
        data class LimitReached(val currentCount: Int, val maxAllowed: Int?) : LimitsState()
    }
}