package com.fitgymtrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.models.PaymentResponse
import com.fitgymtrack.app.models.PaymentStatus
import com.fitgymtrack.app.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de pagos
 */
class PaymentViewModel(
    private val repository: PaymentRepository = PaymentRepository()
) : ViewModel() {

    private val _paymentInitState = MutableStateFlow<PaymentInitState>(PaymentInitState.Initial)
    val paymentInitState: StateFlow<PaymentInitState> = _paymentInitState.asStateFlow()

    private val _paymentStatusState = MutableStateFlow<PaymentStatusState>(PaymentStatusState.Initial)
    val paymentStatusState: StateFlow<PaymentStatusState> = _paymentStatusState.asStateFlow()

    /**
     * Inicializa un pago PayPal
     */
    fun initializePayment(
        amount: Double,
        type: String,
        planId: Int? = null,
        message: String? = null,
        displayName: Boolean = true
    ) {
        _paymentInitState.value = PaymentInitState.Loading

        viewModelScope.launch {
            try {
                val result = repository.initializePayment(
                    amount, type, planId, message, displayName
                )

                result.fold(
                    onSuccess = { response ->
                        _paymentInitState.value = PaymentInitState.Success(response)
                    },
                    onFailure = { error ->
                        _paymentInitState.value = PaymentInitState.Error(error.message ?: "Error desconocido")
                    }
                )
            } catch (e: Exception) {
                _paymentInitState.value = PaymentInitState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Verifica el estado de un pago
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
                        _paymentStatusState.value = PaymentStatusState.Error(error.message ?: "Error desconocido")
                    }
                )
            } catch (e: Exception) {
                _paymentStatusState.value = PaymentStatusState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Resetea el estado de inicialización de pago
     */
    fun resetPaymentInitState() {
        _paymentInitState.value = PaymentInitState.Initial
    }

    /**
     * Resetea el estado de verificación de pago
     */
    fun resetPaymentStatusState() {
        _paymentStatusState.value = PaymentStatusState.Initial
    }

    // Estados para la inicialización del pago
    sealed class PaymentInitState {
        object Initial : PaymentInitState()
        object Loading : PaymentInitState()
        data class Success(val response: PaymentResponse) : PaymentInitState()
        data class Error(val message: String) : PaymentInitState()
    }

    // Estados para la verificación del estado del pago
    sealed class PaymentStatusState {
        object Initial : PaymentStatusState()
        object Loading : PaymentStatusState()
        data class Success(val status: PaymentStatus) : PaymentStatusState()
        data class Error(val message: String) : PaymentStatusState()
    }
}