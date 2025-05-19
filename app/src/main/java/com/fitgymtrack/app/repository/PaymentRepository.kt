package com.fitgymtrack.app.repository

import android.util.Log
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.PaymentRequest
import com.fitgymtrack.app.models.PaymentResponse
import com.fitgymtrack.app.models.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository para la gestión de pagos
 */
class PaymentRepository {
    private val TAG = "PaymentRepository"
    private val apiService = ApiClient.paymentApiService

    /**
     * Inicializa un pago PayPal
     * @param amount Importe del pago
     * @param type Tipo de pago ("subscription" o "donation")
     * @param planId ID del plan para suscripciones
     * @param message Mensaje para donaciones
     * @param displayName Mostrar nombre para donaciones
     * @return Resultado con URL de aprobación PayPal e ID del pedido
     */
    suspend fun initializePayment(
        amount: Double,
        type: String,
        planId: Int? = null,
        message: String? = null,
        displayName: Boolean = true
    ): Result<PaymentResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Inicialización pago: $amount EUR, tipo: $type")

                val paymentRequest = PaymentRequest(
                    amount = amount,
                    type = type,
                    plan_id = planId,
                    message = message,
                    display_name = displayName
                )

                val response = apiService.initializePayment(paymentRequest)

                if (response.success && response.data != null) {
                    Log.d(TAG, "Pago inicializado con éxito: ${response.data.order_id}")
                    Result.success(response.data)
                } else {
                    val errorMsg = response.message ?: "Error desconocido"
                    Log.e(TAG, "Error en la inicialización del pago: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción durante la inicialización del pago", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Verifica el estado de un pago
     * @param orderId ID del pedido a verificar
     * @return Resultado con el estado del pago
     */
    suspend fun checkPaymentStatus(orderId: String): Result<PaymentStatus> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Verificación estado pago: $orderId")

                val response = apiService.checkPaymentStatus(orderId)

                if (response.success && response.data != null) {
                    Log.d(TAG, "Estado pago: ${response.data.status}")
                    Result.success(response.data)
                } else {
                    val errorMsg = response.message ?: "Error desconocido"
                    Log.e(TAG, "Error en la verificación del estado: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción durante la verificación del estado", e)
                Result.failure(e)
            }
        }
    }
}