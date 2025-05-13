package com.fitgymtrack.app.repository

import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class AuthRepository {
    private val apiService = ApiClient.apiService

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Tentativo di login per: $username")
                val loginRequest = LoginRequest(username, password)
                Log.d("AuthRepository", "Request: $loginRequest")

                val response = apiService.login(
                    action = "login",
                    loginRequest = loginRequest
                )

                Log.d("AuthRepository", "Risposta login: $response")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Errore login: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun register(username: String, password: String, email: String, name: String): Result<RegisterResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.register(
                    RegisterRequest(username, password, email, name)
                )
                Result.success(response)
            } catch (e: Exception) {
                // Gestione specifica dell'errore 409 Conflict
                if (e is retrofit2.HttpException && e.code() == 409) {
                    // Creiamo una risposta personalizzata per questo errore
                    val errorResponse = RegisterResponse(
                        success = false,
                        message = "Username o email già in uso. Prova con credenziali diverse."
                    )
                    Result.success(errorResponse)
                } else {
                    Log.e("AuthRepository", "Errore registrazione: ${e.message}", e)
                    Result.failure(e)
                }
            }
        }
    }

    suspend fun requestPasswordReset(email: String): Result<PasswordResetResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val resetRequest = PasswordResetRequest(email)
                val response = apiService.requestPasswordReset(
                    action = "request",
                    resetRequest = resetRequest
                )
                Result.success(response)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Errore richiesta reset password: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun confirmPasswordReset(token: String, code: String, newPassword: String): Result<PasswordResetConfirmResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val resetConfirmRequest = PasswordResetConfirmRequest(token, code, newPassword)
                val response = apiService.confirmPasswordReset(
                    action = "reset",
                    resetConfirmRequest = resetConfirmRequest
                )
                Result.success(response)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Errore conferma reset password: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}