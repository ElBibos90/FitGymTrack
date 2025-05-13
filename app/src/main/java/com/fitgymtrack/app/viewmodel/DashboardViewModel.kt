package com.fitgymtrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.models.User
import com.fitgymtrack.app.models.UserProfile
import com.fitgymtrack.app.repository.UserRepository
import com.fitgymtrack.app.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _subscription = MutableStateFlow<Subscription?>(null)
    val subscription = _subscription.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    fun loadDashboardData(sessionManager: SessionManager) {
        _dashboardState.value = DashboardState.Loading

        viewModelScope.launch {
            try {
                // Carica i dati dell'utente dalla sessione
                val userData = sessionManager.getUserData().first()
                _user.value = userData

                // Carica profilo e abbonamento in parallelo
                val profileResult = repository.getUserProfile()
                val subscriptionResult = repository.getCurrentSubscription()

                // Processa i risultati
                profileResult.fold(
                    onSuccess = { _userProfile.value = it },
                    onFailure = { /* Gestisci l'errore */ }
                )

                subscriptionResult.fold(
                    onSuccess = { _subscription.value = it },
                    onFailure = { /* Gestisci l'errore */ }
                )

                _dashboardState.value = DashboardState.Success
            } catch (e: Exception) {
                _dashboardState.value = DashboardState.Error(e.message ?: "Si è verificato un errore")
            }
        }
    }

    sealed class DashboardState {
        object Loading : DashboardState()
        object Success : DashboardState()
        data class Error(val message: String) : DashboardState()
    }
}