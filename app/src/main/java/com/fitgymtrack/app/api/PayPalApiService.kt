package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.PaymentRequest
import com.fitgymtrack.app.models.PaymentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API service for handling Android PayPal payments
 */
interface PayPalApiService {
    /**
     * Initialize a PayPal payment
     * @param request The payment request data
     * @return Response with payment details including approval URL
     */
    @POST("android/android_payment.php")
    suspend fun initializePayment(
        @Body request: PaymentRequest
    ): Response<PaymentResponse>
}