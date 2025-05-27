// Crea questo file: app/src/main/java/com/fitgymtrack/app/repository/FeedbackRepository.kt
package com.fitgymtrack.app.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.models.*
import com.fitgymtrack.app.utils.DeviceInfoUtils
import com.fitgymtrack.app.utils.FileAttachmentManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class FeedbackRepository(private val context: Context? = null) {

    private val apiService = ApiClient.feedbackApiService
    private val gson = Gson()

    /**
     * Invia un feedback con possibili allegati
     */
    suspend fun submitFeedback(
        type: FeedbackType,
        title: String,
        description: String,
        email: String,
        severity: FeedbackSeverity = FeedbackSeverity.MEDIUM,
        attachments: List<LocalAttachment> = emptyList()
    ): Result<FeedbackResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FeedbackRepository", "Invio feedback: tipo=$type, titolo=$title, allegati=${attachments.size}")

                val deviceInfo = collectDeviceInfo()
                Log.d("FeedbackRepository", "Device info raccolte: $deviceInfo")

                if (attachments.isEmpty()) {
                    // Invio senza allegati
                    return@withContext submitFeedbackWithoutAttachments(
                        type, title, description, email, severity, deviceInfo
                    )
                } else {
                    // Invio con allegati
                    return@withContext submitFeedbackWithAttachments(
                        type, title, description, email, severity, deviceInfo, attachments
                    )
                }

            } catch (e: Exception) {
                Log.e("FeedbackRepository", "Errore invio feedback: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Invia feedback senza allegati
     */
    private suspend fun submitFeedbackWithoutAttachments(
        type: FeedbackType,
        title: String,
        description: String,
        email: String,
        severity: FeedbackSeverity,
        deviceInfo: DeviceInfo
    ): Result<FeedbackResponse> {
        val feedbackRequest = FeedbackRequest(
            type = type.value,
            title = title,
            description = description,
            email = email,
            severity = severity.value,
            deviceInfo = deviceInfo
        )

        Log.d("FeedbackRepository", "Request senza allegati: $feedbackRequest")

        val response = apiService.submitFeedback(feedbackRequest)
        Log.d("FeedbackRepository", "Risposta feedback: $response")

        return Result.success(response)
    }

    /**
     * Invia feedback con allegati
     */
    private suspend fun submitFeedbackWithAttachments(
        type: FeedbackType,
        title: String,
        description: String,
        email: String,
        severity: FeedbackSeverity,
        deviceInfo: DeviceInfo,
        attachments: List<LocalAttachment>
    ): Result<FeedbackResponse> {
        if (context == null) {
            return Result.failure(Exception("Context necessario per gli allegati"))
        }

        try {
            // Prepara i campi del form
            val typeBody = type.value.toRequestBody("text/plain".toMediaTypeOrNull())
            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
            val severityBody = severity.value.toRequestBody("text/plain".toMediaTypeOrNull())
            val deviceInfoBody = gson.toJson(deviceInfo).toRequestBody("application/json".toMediaTypeOrNull())

            // Prepara gli allegati
            val attachmentParts = mutableListOf<MultipartBody.Part>()

            attachments.forEach { attachment ->
                try {
                    val uri = Uri.parse(attachment.uri)
                    val file = FileAttachmentManager.copyFileToCache(context, uri, attachment.name)

                    if (file != null && file.exists()) {
                        val requestBody = file.asRequestBody(attachment.mimeType.toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData(
                            "attachments[]",
                            attachment.name,
                            requestBody
                        )
                        attachmentParts.add(part)
                        Log.d("FeedbackRepository", "Allegato preparato: ${attachment.name}")
                    } else {
                        Log.w("FeedbackRepository", "Impossibile preparare allegato: ${attachment.name}")
                    }
                } catch (e: Exception) {
                    Log.e("FeedbackRepository", "Errore preparazione allegato ${attachment.name}: ${e.message}")
                }
            }

            Log.d("FeedbackRepository", "Invio feedback con ${attachmentParts.size} allegati")

            val response = apiService.submitFeedbackWithAttachments(
                type = typeBody,
                title = titleBody,
                description = descriptionBody,
                email = emailBody,
                severity = severityBody,
                deviceInfo = deviceInfoBody,
                attachments = attachmentParts
            )

            // Pulisci file temporanei
            FileAttachmentManager.cleanupTempFiles(context)

            Log.d("FeedbackRepository", "Risposta feedback con allegati: $response")
            return Result.success(response)

        } catch (e: Exception) {
            Log.e("FeedbackRepository", "Errore invio feedback con allegati: ${e.message}", e)
            // Pulisci file temporanei anche in caso di errore
            context?.let { FileAttachmentManager.cleanupTempFiles(it) }
            return Result.failure(e)
        }
    }

    /**
     * Recupera tutti i feedback (solo per admin)
     */
    suspend fun getFeedbacks(): Result<List<Feedback>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFeedbacks()

                if (response["success"] as? Boolean == true) {
                    @Suppress("UNCHECKED_CAST")
                    val feedbacks = response["feedbacks"] as? List<Map<String, Any>> ?: emptyList()

                    // Qui dovresti convertire i Map in oggetti Feedback
                    // Per ora restituiamo lista vuota
                    Result.success(emptyList<Feedback>())
                } else {
                    val message = response["message"] as? String ?: "Errore sconosciuto"
                    Result.failure(Exception(message))
                }
            } catch (e: Exception) {
                Log.e("FeedbackRepository", "Errore recupero feedback: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Raccoglie informazioni del dispositivo automaticamente
     */
    private fun collectDeviceInfo(): DeviceInfo {
        return if (context != null) {
            DeviceInfoUtils.collectDeviceInfo(context)
        } else {
            // Fallback se non abbiamo il context
            DeviceInfo(
                androidVersion = "Unknown",
                deviceModel = "Unknown",
                deviceManufacturer = "Unknown",
                appVersion = "Unknown",
                screenSize = "Unknown",
                apiLevel = 0
            )
        }
    }

    /**
     * Valida i dati del feedback prima dell'invio
     */
    fun validateFeedback(
        title: String,
        description: String,
        email: String,
        attachments: List<LocalAttachment> = emptyList()
    ): ValidationResult {
        val errors = mutableListOf<String>()

        if (title.isBlank()) {
            errors.add("Il titolo è obbligatorio")
        }

        if (description.isBlank()) {
            errors.add("La descrizione è obbligatoria")
        } else if (description.length < 10) {
            errors.add("La descrizione è troppo breve (minimo 10 caratteri)")
        }

        if (email.isBlank()) {
            errors.add("L'email è obbligatoria")
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errors.add("Formato email non valido")
        }

        // Valida allegati
        if (attachments.isNotEmpty()) {
            val attachmentValidation = FileAttachmentManager.validateAttachments(attachments)
            if (!attachmentValidation.isValid) {
                errors.addAll(attachmentValidation.errors)
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}

/**
 * Risultato della validazione
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)