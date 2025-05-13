package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.LoginRequest
import com.fitgymtrack.app.models.LoginResponse
import com.fitgymtrack.app.models.RegisterRequest
import com.fitgymtrack.app.models.RegisterResponse
import com.fitgymtrack.app.models.UserProfile
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

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

    @GET("subscription_api.php")
    suspend fun getCurrentSubscription(
        @Query("action") action: String = "current_subscription"
    ): Map<String, Any>
}