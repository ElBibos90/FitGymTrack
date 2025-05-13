package com.fitgymtrack.app.repository

import android.util.Log
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

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

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""
                    Log.d("AuthRepository", "Password reset response: $responseBody")

                    // Verifica se la risposta contiene errori PHP
                    if (responseBody.contains("<b>Warning</b>") ||
                        responseBody.contains("<b>Fatal error</b>") ||
                        responseBody.contains("<br />")) {

                        Log.e("AuthRepository", "Risposta contiene errori PHP: $responseBody")
                        return@withContext Result.success(PasswordResetResponse(
                            success = false,
                            message = "Errore del server. Contatta l'amministratore del sistema."
                        ))
                    }

                    // Tentiamo di parsare la risposta come JSON
                    try {
                        val jsonObject = JSONObject(responseBody)
                        val success = jsonObject.optBoolean("success", false)
                        val message = jsonObject.optString("message", "")
                        val token = if (jsonObject.has("token")) jsonObject.optString("token") else null

                        Result.success(PasswordResetResponse(success, message, token))
                    } catch (jsonEx: JSONException) {
                        // Se non è JSON valido, consideriamo la risposta come un messaggio di errore
                        Log.e("AuthRepository", "Risposta non è JSON valido: $responseBody", jsonEx)
                        Result.success(PasswordResetResponse(
                            success = false,
                            message = "Errore nel formato della risposta. Riprova più tardi."
                        ))
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Errore dal server: ${response.code()}"
                    Log.e("AuthRepository", "Reset password fallito: $errorMsg")
                    Result.success(PasswordResetResponse(
                        success = false,
                        message = "Errore dal server: ${response.code()}"
                    ))
                }

            } catch (e: Exception) {
                Log.e("AuthRepository", "Errore richiesta reset password: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun confirmPasswordReset(token: String, code: String, newPassword: String): Result<PasswordResetConfirmResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Log dettagliati per debugging
                Log.d("AuthRepository", "Tentativo di reset password con: code=$code, token=$token, lunghezza password=${newPassword.length}")

                val resetConfirmRequest = PasswordResetConfirmRequest(token, code, newPassword)

                // Log della richiesta
                Log.d("AuthRepository", "Richiesta: action=reset, URL=reset_simple.php")

                val response = apiService.confirmPasswordReset(
                    action = "reset",
                    resetConfirmRequest = resetConfirmRequest
                )

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""
                    Log.d("AuthRepository", "Reset password response: $responseBody")

                    // Verifica se la risposta contiene errori PHP
                    if (responseBody.contains("<b>Warning</b>") ||
                        responseBody.contains("<b>Fatal error</b>") ||
                        responseBody.contains("<br />")) {

                        Log.e("AuthRepository", "Risposta contiene errori PHP: $responseBody")
                        return@withContext Result.success(PasswordResetConfirmResponse(
                            success = false,
                            message = "Errore del server. Contatta l'amministratore del sistema."
                        ))
                    }

                    // Tentiamo di parsare la risposta come JSON
                    try {
                        val jsonObject = JSONObject(responseBody)
                        val success = jsonObject.optBoolean("success", false)
                        val message = jsonObject.optString("message", "")

                        Log.d("AuthRepository", "Risposta parsata: success=$success, message=$message")
                        return@withContext Result.success(PasswordResetConfirmResponse(success, message))
                    } catch (jsonEx: JSONException) {
                        // Se non è JSON valido, consideriamo la risposta come un messaggio di errore
                        Log.e("AuthRepository", "Risposta non è JSON valido: $responseBody", jsonEx)
                        return@withContext Result.success(PasswordResetConfirmResponse(
                            success = false,
                            message = "Errore nel formato della risposta. Riprova più tardi."
                        ))
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Errore dal server: ${response.code()}"
                    Log.e("AuthRepository", "Conferma reset password fallito: $errorMsg")
                    return@withContext Result.success(PasswordResetConfirmResponse(
                        success = false,
                        message = "Errore dal server: ${response.code()}"
                    ))
                }

            } catch (e: Exception) {
                Log.e("AuthRepository", "Errore conferma reset password: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }
    }
}