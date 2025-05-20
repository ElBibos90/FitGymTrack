package com.fitgymtrack.app.repository

import android.util.Log
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.api.PaymentRequest
import com.fitgymtrack.app.api.PaymentResponse
import com.fitgymtrack.app.api.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository per la gestione dei pagamenti
 */
class PaymentRepository {
    private val apiService = ApiClient.paymentApiService
    private val TAG = "PaymentRepository"

    /**
     * Inizializza un pagamento PayPal
     */
    suspend fun initializePayment(
        amount: Double,
        type: String = "subscription",
        planId: Int? = null,
        message: String? = null,
        displayName: Boolean = true
    ): Result<PaymentResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Inizializzazione pagamento: $amount, tipo: $type, piano: $planId")

                val paymentRequest = PaymentRequest(
                    amount = amount,
                    type = type,
                    plan_id = planId,
                    message = message,
                    display_name = displayName
                )

                val response = apiService.initializePayment(paymentRequest)

                if (response.success && response.data != null) {
                    Log.d(TAG, "Pagamento inizializzato con successo: ${response.data.order_id}")
                    Result.success(response.data)
                } else {
                    Log.e(TAG, "Errore nell'inizializzazione del pagamento: ${response.message}")
                    Result.failure(Exception(response.message ?: "Errore sconosciuto"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione nell'inizializzazione del pagamento: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Verifica lo stato di un pagamento
     */
    suspend fun checkPaymentStatus(orderId: String): Result<PaymentStatus> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Verifica stato pagamento: $orderId")

                val response = apiService.checkPaymentStatus(orderId)

                if (response.success && response.data != null) {
                    Log.d(TAG, "Stato pagamento: ${response.data.status}")
                    Result.success(response.data)
                } else {
                    Log.e(TAG, "Errore nella verifica dello stato: ${response.message}")
                    Result.failure(Exception(response.message ?: "Errore sconosciuto"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione nella verifica dello stato: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}