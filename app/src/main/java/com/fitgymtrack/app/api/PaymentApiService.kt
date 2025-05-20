package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interfaccia per le API di pagamento
 */
interface PaymentApiService {
    /**
     * Inizializza un pagamento PayPal
     */
    @POST("android_paypal_payment.php")
    suspend fun initializePayment(
        @Body paymentRequest: PaymentRequest
    ): ApiResponse<PaymentResponse>

    /**
     * Verifica lo stato di un pagamento
     */
    @GET("android_payment_status.php")
    suspend fun checkPaymentStatus(
        @Query("order_id") orderId: String
    ): ApiResponse<PaymentStatus>
}

/**
 * Modello per la richiesta di pagamento
 */
data class PaymentRequest(
    val amount: Double,
    val type: String = "subscription",
    val plan_id: Int? = null,
    val message: String? = null,
    val display_name: Boolean = true
)

/**
 * Modello per la risposta di pagamento
 */
data class PaymentResponse(
    val order_id: String,
    val paypal_order_id: String,
    val approval_url: String
)

/**
 * Modello per lo stato di un pagamento
 */
data class PaymentStatus(
    val success: Boolean,
    val order_id: String,
    val paypal_order_id: String,
    val status: String,
    val amount: Double,
    val type: String,
    val plan_id: Int?,
    val paypal_status: String?
)