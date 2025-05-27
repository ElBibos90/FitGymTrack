// Crea questo file: app/src/main/java/com/fitgymtrack/app/api/FeedbackApiService.kt
package com.fitgymtrack.app.api

import com.fitgymtrack.app.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface FeedbackApiService {

    /**
     * Invia un feedback senza allegati
     */
    @POST("feedback_api.php")
    suspend fun submitFeedback(
        @Body feedbackRequest: FeedbackRequest
    ): FeedbackResponse

    /**
     * Invia un feedback con allegati (multipart)
     */
    @Multipart
    @POST("feedback_api.php")
    suspend fun submitFeedbackWithAttachments(
        @Part("type") type: RequestBody,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("email") email: RequestBody,
        @Part("severity") severity: RequestBody,
        @Part("device_info") deviceInfo: RequestBody,
        @Part attachments: List<MultipartBody.Part>
    ): FeedbackResponse

    /**
     * Recupera tutti i feedback (solo per admin)
     */
    @GET("feedback_api.php")
    suspend fun getFeedbacks(): Map<String, Any>

    /**
     * Aggiorna lo stato di un feedback (solo per admin)
     */
    @POST("feedback_api.php")
    suspend fun updateFeedbackStatus(
        @Body request: Map<String, Any>
    ): Map<String, Any>

    /**
     * Aggiorna le note admin di un feedback (solo per admin)
     */
    @POST("feedback_api.php")
    suspend fun updateFeedbackNotes(
        @Body request: Map<String, Any>
    ): Map<String, Any>
}