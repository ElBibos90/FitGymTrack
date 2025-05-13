package com.fitgymtrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.models.User
import com.fitgymtrack.app.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import java.io.IOException
import retrofit2.HttpException

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    fun login(username: String, password: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            val result = repository.login(username, password)

            result.fold(
                onSuccess = { response ->
                    if (response.token != null && response.user != null) {
                        // Ignoriamo il campo success e controlliamo solo se ci sono dati significativi
                        _loginState.value = LoginState.Success(response.user, response.token)
                    } else {
                        _loginState.value = LoginState.Error(response.error ?: "Unknown error")
                    }
                },
                onFailure = { e ->
                    _loginState.value = LoginState.Error(e.message ?: "Unknown error")
                }
            )
        }
    }

    fun register(username: String, password: String, email: String, name: String) {
        _registerState.value = RegisterState.Loading

        viewModelScope.launch {
            val result = repository.register(username, password, email, name)

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        _registerState.value = RegisterState.Success(response.message)
                    } else {
                        // Ora gestiremo meglio i messaggi di errore
                        val errorMessage = response.message.takeIf { it.isNotBlank() }
                            ?: "Si è verificato un errore durante la registrazione"
                        _registerState.value = RegisterState.Error(errorMessage)
                    }
                },
                onFailure = { e ->
                    // Per gli errori generici, forniamo messaggi più descrittivi
                    val errorMessage = when (e) {
                        is IOException -> "Impossibile connettersi al server. Verifica la tua connessione."
                        is HttpException -> {
                            when (e.code()) {
                                409 -> "Username o email già in uso. Prova con credenziali diverse."
                                else -> "Errore dal server: ${e.code()}"
                            }
                        }
                        else -> e.message ?: "Si è verificato un errore sconosciuto"
                    }
                    _registerState.value = RegisterState.Error(errorMessage)
                }
            )
        }

    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val user: User, val token: String) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    sealed class RegisterState {
        object Idle : RegisterState()
        object Loading : RegisterState()
        data class Success(val message: String) : RegisterState()
        data class Error(val message: String) : RegisterState()
    }
}