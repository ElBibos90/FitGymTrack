package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.ApiResponse
import com.fitgymtrack.app.models.PaymentRequest
import com.fitgymtrack.app.models.PaymentResponse
import com.fitgymtrack.app.models.PaymentStatus
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Interfaz para las API de pago
 */
interface PaymentApiService {
    /**
     * Inicializa un pago PayPal
     */
    @POST("android_paypal_payment.php")
    suspend fun initializePayment(
        @Body paymentRequest: PaymentRequest
    ): ApiResponse<PaymentResponse>

    /**
     * Verifica el estado de un pago
     */
    @GET("android_payment_status.php")
    suspend fun checkPaymentStatus(
        @Query("order_id") orderId: String
    ): ApiResponse<PaymentStatus>
}