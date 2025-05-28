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

    // NUOVO: Stato per subscription scadute
    private val _expiredSubscriptionState = MutableStateFlow<ExpiredSubscriptionState>(ExpiredSubscriptionState.Initial)
    val expiredSubscriptionState: StateFlow<ExpiredSubscriptionState> = _expiredSubscriptionState.asStateFlow()

    /**
     * Carica l'abbonamento corrente con controllo scadenze automatico
     */
    fun loadSubscription(checkExpired: Boolean = true) {
        _subscriptionState.value = SubscriptionState.Loading

        viewModelScope.launch {
            try {
                // RIABILITATO: Se richiesto, controlla prima le subscription scadute
                if (checkExpired) {
                    checkExpiredSubscriptions()
                }

                val result = repository.getCurrentSubscription()

                result.fold(
                    onSuccess = { subscription ->
                        // LOG DETTAGLIATI: Cosa riceve il ViewModel
                        Log.d("SubscriptionViewModel", "🔍 [RICEVUTO] Piano: ${subscription.planName}")
                        Log.d("SubscriptionViewModel", "🔍 [RICEVUTO] Prezzo: ${subscription.price}")
                        Log.d("SubscriptionViewModel", "🔍 [RICEVUTO] End_date: ${subscription.end_date}")
                        Log.d("SubscriptionViewModel", "🔍 [RICEVUTO] Status: ${subscription.status}")

                        _subscriptionState.value = SubscriptionState.Success(subscription)

                        // Controlla se questa subscription è appena scaduta localmente
                        checkIfJustExpired(subscription)
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
     * NUOVO: Controlla le subscription scadute tramite API
     */
    private suspend fun checkExpiredSubscriptions() {
        try {
            // Chiamata alla nuova API per forzare il controllo delle scadenze
            val result = repository.checkExpiredSubscriptions()

            result.fold(
                onSuccess = { response ->
                    val updatedCount = response["updated_count"] as? Int ?: 0
                    if (updatedCount > 0) {
                        Log.d("SubscriptionViewModel", "Trovate $updatedCount subscription scadute")
                        _expiredSubscriptionState.value = ExpiredSubscriptionState.Found(updatedCount)
                    }
                },
                onFailure = { error ->
                    Log.e("SubscriptionViewModel", "Errore controllo scadenze: ${error.message}")
                }
            )
        } catch (e: Exception) {
            Log.e("SubscriptionViewModel", "Eccezione controllo scadenze: ${e.message}")
        }
    }

    /**
     * NUOVO: Controlla se la subscription è appena scaduta confrontando con l'ultima conosciuta
     */
    private fun checkIfJustExpired(currentSubscription: Subscription) {
        try {
            // Controllo locale della data di scadenza
            val endDate = currentSubscription.end_date
            if (endDate != null && currentSubscription.price > 0.0) {
                // Parsing della data (assumendo formato "YYYY-MM-DD HH:mm:ss")
                // Questo è un controllo semplificato - potresti voler usare una libreria di date
                Log.d("SubscriptionViewModel", "Subscription end_date: $endDate")

                // Se il piano è Premium ma l'end_date è nel passato, potrebbe essere appena scaduto
                // Per ora, log per debugging
                Log.d("SubscriptionViewModel", "Piano ${currentSubscription.planName}, prezzo: ${currentSubscription.price}")
            }

            // Se la subscription attuale è Free e il prezzo era > 0 prima,
            // significa che è appena scaduta
            if (currentSubscription.planName == "Free" && currentSubscription.price == 0.0) {
                // Questo potrebbe indicare una degradazione da Premium a Free
                Log.d("SubscriptionViewModel", "Utente ora su piano Free - possibile scadenza")

                // Per ora non emettiamo eventi di scadenza, dato che l'API non è pronta
                // _expiredSubscriptionState.value = ExpiredSubscriptionState.JustDetected
            }
        } catch (e: Exception) {
            Log.e("SubscriptionViewModel", "Errore nel controllo locale scadenza: ${e.message}")
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
                        val limitReached = response["limit_reached"] as? Boolean == true
                        val currentCount = response["current_count"] as? Int ?: 0
                        val maxAllowed = response["max_allowed"] as? Int
                        val remaining = response["remaining"] as? Int ?: 0
                        val subscriptionStatus = response["subscription_status"] as? String
                        val daysRemaining = response["days_remaining"] as? Int

                        _resourceLimitState.value = ResourceLimitState.Success(
                            limitReached = limitReached,
                            currentCount = currentCount,
                            maxAllowed = maxAllowed,
                            remaining = remaining,
                            subscriptionStatus = subscriptionStatus,
                            daysRemaining = daysRemaining
                        )

                        // NUOVO: Se la subscription è scaduta, aggiorna lo stato
                        if (subscriptionStatus == "expired") {
                            _expiredSubscriptionState.value = ExpiredSubscriptionState.JustDetected
                        }
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
                        val success = response["success"] as? Boolean == true
                        val message = response["message"] as? String ?: "Piano aggiornato con successo"

                        if (success) {
                            _updatePlanState.value = UpdatePlanState.Success(message)
                            // Ricarica l'abbonamento
                            loadSubscription(checkExpired = false) // Non ricontrollare le scadenze dopo un update
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
                Log.d("SubscriptionViewModel", "Inizializzazione pagamento: $amount, piano: $planId")

                val approvalUrl = "fitgymtrack://payment/success"
                _paymentState.value = PaymentState.Success(approvalUrl)
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }

    // NUOVO: Funzioni per resettare lo stato delle subscription scadute
    fun resetExpiredSubscriptionState() {
        _expiredSubscriptionState.value = ExpiredSubscriptionState.Initial
    }

    fun dismissExpiredNotification() {
        _expiredSubscriptionState.value = ExpiredSubscriptionState.Dismissed
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
            val remaining: Int,
            val subscriptionStatus: String? = null, // NUOVO
            val daysRemaining: Int? = null // NUOVO
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

    // NUOVO: Stati per subscription scadute
    sealed class ExpiredSubscriptionState {
        object Initial : ExpiredSubscriptionState()
        data class Found(val expiredCount: Int) : ExpiredSubscriptionState()
        object JustDetected : ExpiredSubscriptionState()
        object Dismissed : ExpiredSubscriptionState()
    }
}