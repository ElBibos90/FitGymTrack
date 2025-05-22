package com.fitgymtrack.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.models.UserStats
import com.fitgymtrack.app.repository.StatsRepository
import com.fitgymtrack.app.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StatsViewModel(
    private val statsRepository: StatsRepository = StatsRepository()
) : ViewModel() {

    private val _statsState = MutableStateFlow<StatsState>(StatsState.Loading)
    val statsState: StateFlow<StatsState> = _statsState.asStateFlow()

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId = _currentUserId.asStateFlow()

    // SessionManager verrà passato quando necessario
    private var sessionManager: SessionManager? = null

    /**
     * Imposta il SessionManager
     */
    fun setSessionManager(sessionManager: SessionManager) {
        this.sessionManager = sessionManager
    }

    /**
     * Carica le statistiche dell'utente corrente
     */
    fun loadStats(userId: Int? = null, forceReload: Boolean = false) {
        viewModelScope.launch {
            try {
                _statsState.value = StatsState.Loading

                // Se non è fornito userId, prendi quello dalla sessione
                val targetUserId = userId ?: _currentUserId.value ?: run {
                    // Ottieni l'utente dalla sessione se disponibile
                    sessionManager?.let { sm ->
                        val user = sm.getUserData().first()
                        user?.id
                    } ?: run {
                        _statsState.value = StatsState.Error("Utente non trovato")
                        return@launch
                    }
                }

                _currentUserId.value = targetUserId

                Log.d("StatsViewModel", "Caricamento statistiche per utente: $targetUserId")

                // Carica le statistiche
                val result = statsRepository.getUserStats(targetUserId)

                result.fold(
                    onSuccess = { stats ->
                        Log.d("StatsViewModel", "Statistiche caricate con successo: ${stats.totalWorkouts} allenamenti")
                        _statsState.value = StatsState.Success(stats)
                    },
                    onFailure = { error ->
                        Log.e("StatsViewModel", "Errore nel caricamento statistiche: ${error.message}")
                        _statsState.value = StatsState.Error(error.message ?: "Errore sconosciuto")
                    }
                )

            } catch (e: Exception) {
                Log.e("StatsViewModel", "Eccezione nel caricamento statistiche", e)
                _statsState.value = StatsState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }

    /**
     * Ricalcola le statistiche
     */
    fun recalculateStats() {
        viewModelScope.launch {
            try {
                val userId = _currentUserId.value ?: return@launch

                _statsState.value = StatsState.Loading

                val result = statsRepository.calculateStats(userId, recalculateAll = true)

                result.fold(
                    onSuccess = { stats ->
                        _statsState.value = StatsState.Success(stats)
                    },
                    onFailure = { error ->
                        _statsState.value = StatsState.Error(error.message ?: "Errore nel ricalcolo")
                    }
                )

            } catch (e: Exception) {
                _statsState.value = StatsState.Error(e.message ?: "Errore nel ricalcolo")
            }
        }
    }

    /**
     * Refresh delle statistiche
     */
    fun refreshStats() {
        _currentUserId.value?.let { userId ->
            loadStats(userId, forceReload = true)
        }
    }

    /**
     * Reset dello stato
     */
    fun resetState() {
        _statsState.value = StatsState.Loading
    }

    sealed class StatsState {
        object Loading : StatsState()
        data class Success(val stats: UserStats) : StatsState()
        data class Error(val message: String) : StatsState()
    }
}