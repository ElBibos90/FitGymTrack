package com.fitgymtrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.models.User
import com.fitgymtrack.app.models.UserProfile
import com.fitgymtrack.app.models.UserStats
import com.fitgymtrack.app.repository.StatsRepository
import com.fitgymtrack.app.repository.UserRepository
import com.fitgymtrack.app.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: UserRepository = UserRepository(),
    private val statsRepository: StatsRepository = StatsRepository()
) : ViewModel() {

    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _subscription = MutableStateFlow<Subscription?>(null)
    val subscription = _subscription.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    // Nuovo: Statistiche utente
    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats = _userStats.asStateFlow()

    private val _statsLoading = MutableStateFlow(false)
    val statsLoading = _statsLoading.asStateFlow()

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

                // Carica le statistiche se l'utente ha il piano Premium
                userData?.let { user ->
                    loadUserStats(user.id)
                }
            } catch (e: Exception) {
                _dashboardState.value = DashboardState.Error(e.message ?: "Si è verificato un errore")
            }
        }
    }

    /**
     * Carica le statistiche dell'utente
     */
    fun loadUserStats(userId: Int, forceReload: Boolean = false) {
        // Non caricare se già in caricamento
        if (_statsLoading.value) return

        viewModelScope.launch {
            _statsLoading.value = true

            try {
                // Per ora, usa statistiche demo per testing
                // Rimuovi questa linea quando l'API è pronta
                val demoStats = statsRepository.createDemoStats()
                _userStats.value = demoStats

                // Codice per API reale (da attivare quando l'API è pronta):
                /*
                val result = statsRepository.getUserStats(userId)
                result.fold(
                    onSuccess = { stats ->
                        _userStats.value = stats
                    },
                    onFailure = { error ->
                        // In caso di errore, mantieni le statistiche esistenti
                        // o mostra stats vuote
                        if (_userStats.value == null) {
                            _userStats.value = UserStats() // Statistiche vuote
                        }
                    }
                )
                */

            } catch (e: Exception) {
                // Gestisci l'errore mantenendo le statistiche esistenti
                if (_userStats.value == null) {
                    _userStats.value = UserStats() // Statistiche vuote
                }
            } finally {
                _statsLoading.value = false
            }
        }
    }

    /**
     * Ricarica le statistiche
     */
    fun refreshStats() {
        _user.value?.let { user ->
            loadUserStats(user.id, forceReload = true)
        }
    }

    /**
     * Resetta lo stato delle statistiche
     */
    fun resetStatsState() {
        _userStats.value = null
        _statsLoading.value = false
    }

    sealed class DashboardState {
        object Loading : DashboardState()
        object Success : DashboardState()
        data class Error(val message: String) : DashboardState()
    }
}