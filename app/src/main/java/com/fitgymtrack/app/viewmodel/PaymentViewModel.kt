package com.fitgymtrack.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.api.PaymentResponse
import com.fitgymtrack.app.api.PaymentStatus
import com.fitgymtrack.app.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PaymentViewModel(
    private val repository: PaymentRepository = PaymentRepository()
) : ViewModel() {

    private val _paymentInitState = MutableStateFlow<PaymentInitState>(PaymentInitState.Initial)
    val paymentInitState: StateFlow<PaymentInitState> = _paymentInitState.asStateFlow()

    private val _paymentStatusState = MutableStateFlow<PaymentStatusState>(PaymentStatusState.Initial)
    val paymentStatusState: StateFlow<PaymentStatusState> = _paymentStatusState.asStateFlow()

    /**
     * Inizializza un pagamento PayPal
     */
    fun initializePayment(
        amount: Double,
        type: String = "subscription",
        planId: Int? = null,
        message: String? = null,
        displayName: Boolean = true
    ) {
        _paymentInitState.value = PaymentInitState.Loading

        viewModelScope.launch {
            try {
                val result = repository.initializePayment(
                    amount = amount,
                    type = type,
                    planId = planId,
                    message = message,
                    displayName = displayName
                )

                result.fold(
                    onSuccess = { response ->
                        _paymentInitState.value = PaymentInitState.Success(response)
                    },
                    onFailure = { error ->
                        _paymentInitState.value = PaymentInitState.Error(error.message ?: "Errore sconosciuto")
                    }
                )
            } catch (e: Exception) {
                _paymentInitState.value = PaymentInitState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }

    /**
     * Verifica lo stato di un pagamento
     */
    fun checkPaymentStatus(orderId: String) {
        _paymentStatusState.value = PaymentStatusState.Loading

        viewModelScope.launch {
            try {
                val result = repository.checkPaymentStatus(orderId)

                result.fold(
                    onSuccess = { status ->
                        _paymentStatusState.value = PaymentStatusState.Success(status)
                    },
                    onFailure = { error ->
                        _paymentStatusState.value = PaymentStatusState.Error(error.message ?: "Errore sconosciuto")
                    }
                )
            } catch (e: Exception) {
                _paymentStatusState.value = PaymentStatusState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }

    fun resetPaymentInitState() {
        _paymentInitState.value = PaymentInitState.Initial
    }

    fun resetPaymentStatusState() {
        _paymentStatusState.value = PaymentStatusState.Initial
    }

    // Stati per le diverse operazioni
    sealed class PaymentInitState {
        object Initial : PaymentInitState()
        object Loading : PaymentInitState()
        data class Success(val response: PaymentResponse) : PaymentInitState()
        data class Error(val message: String) : PaymentInitState()
    }

    sealed class PaymentStatusState {
        object Initial : PaymentStatusState()
        object Loading : PaymentStatusState()
        data class Success(val status: PaymentStatus) : PaymentStatusState()
        data class Error(val message: String) : PaymentStatusState()
    }
}