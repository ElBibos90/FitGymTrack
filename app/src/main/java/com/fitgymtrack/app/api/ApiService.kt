package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.*
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import com.fitgymtrack.app.models.Exercise
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.ResponseBody
import retrofit2.Response

interface ApiService {
    @POST("auth.php")
    suspend fun login(
        @Query("action") action: String = "login",
        @Body loginRequest: LoginRequest
    ): LoginResponse

    @POST("standalone_register.php")
    suspend fun register(
        @Body registerRequest: RegisterRequest
    ): RegisterResponse

    @GET("utente_profilo.php")
    suspend fun getUserProfile(): UserProfile

    @PUT("utente_profilo.php")
    suspend fun updateUserProfile(
        @Body userProfile: UserProfile
    ): Map<String, Any>

    // Modificato per ricevere ResponseBody invece di un oggetto tipizzato
    @POST("password_reset.php")
    suspend fun requestPasswordReset(
        @Query("action") action: String = "request",
        @Body resetRequest: PasswordResetRequest
    ): Response<ResponseBody>

    // Modificato per ricevere ResponseBody invece di un oggetto tipizzato
    @POST("reset_simple.php")
    suspend fun confirmPasswordReset(
        @Query("action") action: String = "reset",
        @Body resetConfirmRequest: PasswordResetConfirmRequest
    ): Response<ResponseBody>

    @GET("subscription_api.php")
    suspend fun getCurrentSubscription(
        @Query("action") action: String = "current_subscription"
    ): Map<String, Any>

    @GET("subscription_api.php")
    suspend fun checkResourceLimits(
        @Query("resource_type") resourceType: String,
        @Query("action") action: String = "check_limits"
    ): Map<String, Any>

    @POST("subscription_api.php")
    suspend fun updatePlan(
        @Body request: Map<String, Any>,
        @Query("action") action: String = "update_plan"
    ): Map<String, Any>

    // Per la gestione dei pagamenti (opzionale se implementi pagamenti PayPal)
    @POST("paypal_payment.php")
    suspend fun initializePayment(
        @Body paymentRequest: PaymentRequest
    ): PaymentResponse
}
