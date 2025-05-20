package com.fitgymtrack.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val repository: SubscriptionRepository = SubscriptionRepository()
) : ViewModel() {

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Initial)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private val _resourceLimitState = MutableStateFlow<ResourceLimitState>(ResourceLimitState.Initial)
    val resourceLimitState: StateFlow<ResourceLimitState> = _resourceLimitState.asStateFlow()

    private val _updatePlanState = MutableStateFlow<UpdatePlanState>(UpdatePlanState.Initial)
    val updatePlanState: StateFlow<UpdatePlanState> = _updatePlanState.asStateFlow()

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Initial)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    /**
     * Carica l'abbonamento corrente
     */
    fun loadSubscription() {
        _subscriptionState.value = SubscriptionState.Loading

        viewModelScope.launch {
            try {
                val result = repository.getCurrentSubscription()

                result.fold(
                    onSuccess = { subscription ->
                        _subscriptionState.value = SubscriptionState.Success(subscription)
                    },
                    onFailure = { error ->
                        _subscriptionState.value = SubscriptionState.Error(error.message ?: "Errore sconosciuto")
                    }
                )
            } catch (e: Exception) {
                _subscriptionState.value = SubscriptionState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }

    /**
     * Verifica i limiti per un tipo di risorsa
     */
    fun checkLimits(resourceType: String) {
        _resourceLimitState.value = ResourceLimitState.Loading

        viewModelScope.launch {
            try {
                val result = repository.checkResourceLimits(resourceType)

                result.fold(
                    onSuccess = { response ->
                        val limitReached = response["limit_reached"] as? Boolean ?: false
                        val currentCount = response["current_count"] as? Int ?: 0
                        val maxAllowed = response["max_allowed"] as? Int
                        val remaining = response["remaining"] as? Int ?: 0

                        _resourceLimitState.value = ResourceLimitState.Success(
                            limitReached = limitReached,
                            currentCount = currentCount,
                            maxAllowed = maxAllowed,
                            remaining = remaining
                        )
                    },
                    onFailure = { error ->
                        _resourceLimitState.value = ResourceLimitState.Error(error.message ?: "Errore sconosciuto")
                    }
                )
            } catch (e: Exception) {
                _resourceLimitState.value = ResourceLimitState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }

    /**
     * Aggiorna il piano di abbonamento
     */
    fun updatePlan(planId: Int) {
        _updatePlanState.value = UpdatePlanState.Loading

        viewModelScope.launch {
            try {
                val result = repository.updatePlan(planId)

                result.fold(
                    onSuccess = { response ->
                        val success = response["success"] as? Boolean ?: false
                        val message = response["message"] as? String ?: "Piano aggiornato con successo"

                        if (success) {
                            _updatePlanState.value = UpdatePlanState.Success(message)
                            // Ricarica l'abbonamento
                            loadSubscription()
                        } else {
                            _updatePlanState.value = UpdatePlanState.Error(message)
                        }
                    },
                    onFailure = { error ->
                        _updatePlanState.value = UpdatePlanState.Error(error.message ?: "Errore sconosciuto")
                    }
                )
            } catch (e: Exception) {
                _updatePlanState.value = UpdatePlanState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }

    /**
     * Inizializza un pagamento PayPal
     */
    fun initializePayment(amount: Double, planId: Int) {
        _paymentState.value = PaymentState.Loading

        viewModelScope.launch {
            try {
                // Qui dovresti chiamare il repository per inizializzare il pagamento
                // Ma per ora lo simuliamo
                Log.d("SubscriptionViewModel", "Inizializzazione pagamento: $amount, piano: $planId")

                // In un caso reale, qui chiameresti il PaymentRepository
                val approvalUrl = "fitgymtrack://payment/success"

                _paymentState.value = PaymentState.Success(approvalUrl)
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }

    fun resetSubscriptionState() {
        _subscriptionState.value = SubscriptionState.Initial
    }

    fun resetResourceLimitState() {
        _resourceLimitState.value = ResourceLimitState.Initial
    }

    fun resetUpdatePlanState() {
        _updatePlanState.value = UpdatePlanState.Initial
    }

    fun resetPaymentState() {
        _paymentState.value = PaymentState.Initial
    }

    // Stati per le diverse operazioni
    sealed class SubscriptionState {
        object Initial : SubscriptionState()
        object Loading : SubscriptionState()
        data class Success(val subscription: Subscription) : SubscriptionState()
        data class Error(val message: String) : SubscriptionState()
    }

    sealed class ResourceLimitState {
        object Initial : ResourceLimitState()
        object Loading : ResourceLimitState()
        data class Success(
            val limitReached: Boolean,
            val currentCount: Int,
            val maxAllowed: Int?,
            val remaining: Int
        ) : ResourceLimitState()
        data class Error(val message: String) : ResourceLimitState()
    }

    sealed class UpdatePlanState {
        object Initial : UpdatePlanState()
        object Loading : UpdatePlanState()
        data class Success(val message: String) : UpdatePlanState()
        data class Error(val message: String) : UpdatePlanState()
    }

    sealed class PaymentState {
        object Initial : PaymentState()
        object Loading : PaymentState()
        data class Success(val approvalUrl: String) : PaymentState()
        data class Error(val message: String) : PaymentState()
    }
}