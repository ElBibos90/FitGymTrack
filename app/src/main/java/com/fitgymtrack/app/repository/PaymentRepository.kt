package com.fitgymtrack.app.repository

import android.util.Log
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.PaymentRequest
import com.fitgymtrack.app.models.PaymentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for handling PayPal payments
 */
class PaymentRepository {
    private val TAG = "PaymentRepository"

    /**
     * Initialize a subscription payment
     * @param userId User ID
     * @param planId Subscription plan ID
     * @param amount Payment amount
     * @return Result containing PaymentResponse or error
     */
    suspend fun initializeSubscriptionPayment(
        userId: Int,
        planId: Int,
        amount: Double
    ): Result<PaymentResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing subscription payment: userId=$userId, planId=$planId, amount=$amount")

                // Create payment request
                val request = PaymentRequest(
                    user_id = userId,
                    plan_id = planId,
                    amount = amount,
                    type = "subscription"
                )

                // Send API request
                val paypalService = ApiClient.paypalApiService
                val response = paypalService.initializePayment(request)

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Payment initialized successfully: ${response.body()}")
                    return@withContext Result.success(response.body()!!)
                } else {
                    Log.e(TAG, "Payment initialization failed: ${response.errorBody()?.string()}")
                    return@withContext Result.failure(
                        Exception(response.errorBody()?.string() ?: "Unknown error")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during payment initialization", e)
                return@withContext Result.failure(e)
            }
        }
    }

    /**
     * Initialize a donation payment
     * @param userId User ID
     * @param amount Donation amount
     * @param message Optional message for donation
     * @param displayName Whether to display user's name
     * @return Result containing PaymentResponse or error
     */
    suspend fun initializeDonation(
        userId: Int,
        amount: Double,
        message: String? = null,
        displayName: Boolean = true
    ): Result<PaymentResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing donation: userId=$userId, amount=$amount")

                // Create payment request
                val request = PaymentRequest(
                    user_id = userId,
                    plan_id = 0, // Not needed for donations
                    amount = amount,
                    type = "donation",
                    message = message,
                    display_name = displayName
                )

                // Send API request
                val paypalService = ApiClient.paypalApiService
                val response = paypalService.initializePayment(request)

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Donation initialized successfully: ${response.body()}")
                    return@withContext Result.success(response.body()!!)
                } else {
                    Log.e(TAG, "Donation initialization failed: ${response.errorBody()?.string()}")
                    return@withContext Result.failure(
                        Exception(response.errorBody()?.string() ?: "Unknown error")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during donation initialization", e)
                return@withContext Result.failure(e)
            }
        }
    }
}