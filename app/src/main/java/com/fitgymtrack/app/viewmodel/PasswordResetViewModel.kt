package com.fitgymtrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import retrofit2.HttpException

class PasswordResetViewModel : ViewModel() {
    private val repository = AuthRepository()

    // Stato per la richiesta di reset password
    private val _resetRequestState = MutableStateFlow<ResetRequestState>(ResetRequestState.Idle)
    val resetRequestState: StateFlow<ResetRequestState> = _resetRequestState.asStateFlow()

    // Stato per il reset effettivo della password
    private val _resetState = MutableStateFlow<ResetState>(ResetState.Idle)
    val resetState: StateFlow<ResetState> = _resetState.asStateFlow()

    /**
     * Richiede il reset della password inviando un'email all'utente
     */
    fun requestPasswordReset(email: String) {
        if (email.isBlank()) {
            _resetRequestState.value = ResetRequestState.Error("Inserisci un indirizzo email valido")
            return
        }

        // Verifica basilare del formato email
        if (!email.contains("@") || !email.contains(".")) {
            _resetRequestState.value = ResetRequestState.Error("Inserisci un indirizzo email valido")
            return
        }

        _resetRequestState.value = ResetRequestState.Loading

        viewModelScope.launch {
            val result = repository.requestPasswordReset(email)

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        _resetRequestState.value = ResetRequestState.Success(response.token ?: "")
                    } else {
                        _resetRequestState.value = ResetRequestState.Error(
                            response.message.takeIf { it.isNotBlank() }
                                ?: "Si è verificato un errore durante la richiesta"
                        )
                    }
                },
                onFailure = { e ->
                    // Gestione più descrittiva degli errori
                    val errorMessage = when (e) {
                        is IOException -> "Impossibile connettersi al server. Verifica la tua connessione."
                        is HttpException -> {
                            when (e.code()) {
                                404 -> "Email non trovata nel sistema"
                                429 -> "Troppe richieste. Riprova più tardi."
                                else -> "Errore dal server: ${e.code()}"
                            }
                        }
                        else -> e.message ?: "Si è verificato un errore sconosciuto"
                    }
                    _resetRequestState.value = ResetRequestState.Error(errorMessage)
                }
            )
        }
    }

    /**
     * Effettua il reset della password con il codice di verifica e la nuova password
     */
    fun resetPassword(token: String, code: String, newPassword: String) {
        if (code.isBlank()) {
            _resetState.value = ResetState.Error("Inserisci il codice di verifica")
            return
        }

        if (newPassword.isBlank()) {
            _resetState.value = ResetState.Error("Inserisci una nuova password")
            return
        }

        if (newPassword.length < 8) {
            _resetState.value = ResetState.Error("La password deve essere di almeno 8 caratteri")
            return
        }

        // Verifica che la password abbia almeno un numero e una lettera maiuscola
        val hasNumber = newPassword.any { it.isDigit() }
        val hasUpperCase = newPassword.any { it.isUpperCase() }

        if (!hasNumber || !hasUpperCase) {
            _resetState.value = ResetState.Error("La password deve contenere almeno un numero e una lettera maiuscola")
            return
        }

        _resetState.value = ResetState.Loading

        viewModelScope.launch {
            val result = repository.confirmPasswordReset(token, code, newPassword)

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        _resetState.value = ResetState.Success
                    } else {
                        _resetState.value = ResetState.Error(
                            response.message.takeIf { it.isNotBlank() }
                                ?: "Si è verificato un errore durante il reset della password"
                        )
                    }
                },
                onFailure = { e ->
                    // Gestione più descrittiva degli errori
                    val errorMessage = when (e) {
                        is IOException -> "Impossibile connettersi al server. Verifica la tua connessione."
                        is HttpException -> {
                            when (e.code()) {
                                400 -> "Codice di verifica non valido o scaduto"
                                429 -> "Troppe richieste. Riprova più tardi."
                                else -> "Errore dal server: ${e.code()}"
                            }
                        }
                        else -> e.message ?: "Si è verificato un errore sconosciuto"
                    }
                    _resetState.value = ResetState.Error(errorMessage)
                }
            )
        }
    }

    fun resetRequestState() {
        _resetRequestState.value = ResetRequestState.Idle
    }

    fun resetResetState() {
        _resetState.value = ResetState.Idle
    }

    // Stati per la richiesta di reset
    sealed class ResetRequestState {
        object Idle : ResetRequestState()
        object Loading : ResetRequestState()
        data class Success(val token: String) : ResetRequestState()
        data class Error(val message: String) : ResetRequestState()
    }

    // Stati per il reset effettivo
    sealed class ResetState {
        object Idle : ResetState()
        object Loading : ResetState()
        object Success : ResetState()
        data class Error(val message: String) : ResetState()
    }
}