// Crea questo file: app/src/main/java/com/fitgymtrack/app/viewmodel/FeedbackViewModel.kt
package com.fitgymtrack.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.models.FeedbackSeverity
import com.fitgymtrack.app.models.FeedbackType
import com.fitgymtrack.app.models.LocalAttachment
import com.fitgymtrack.app.repository.FeedbackRepository
import com.fitgymtrack.app.utils.FileAttachmentManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedbackViewModel : ViewModel() {

    private lateinit var repository: FeedbackRepository
    private var context: Context? = null

    fun initialize(context: Context) {
        if (!::repository.isInitialized) {
            repository = FeedbackRepository(context)
            this.context = context
        }
    }

    // Stati del form
    private val _feedbackType = MutableStateFlow(FeedbackType.BUG)
    val feedbackType: StateFlow<FeedbackType> = _feedbackType.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _severity = MutableStateFlow(FeedbackSeverity.MEDIUM)
    val severity: StateFlow<FeedbackSeverity> = _severity.asStateFlow()

    // NUOVO: Stati per gli attachment
    private val _attachments = MutableStateFlow<List<LocalAttachment>>(emptyList())
    val attachments: StateFlow<List<LocalAttachment>> = _attachments.asStateFlow()

    // Stati di validazione
    private val _validationErrors = MutableStateFlow<List<String>>(emptyList())
    val validationErrors: StateFlow<List<String>> = _validationErrors.asStateFlow()

    // Stati dell'invio
    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    /**
     * Aggiorna il tipo di feedback
     */
    fun updateFeedbackType(type: FeedbackType) {
        _feedbackType.value = type
        clearValidationErrors()
    }

    /**
     * Aggiorna il titolo
     */
    fun updateTitle(title: String) {
        _title.value = title
        clearValidationErrors()
    }

    /**
     * Aggiorna la descrizione
     */
    fun updateDescription(description: String) {
        _description.value = description
        clearValidationErrors()
    }

    /**
     * Aggiorna l'email
     */
    fun updateEmail(email: String) {
        _email.value = email
        clearValidationErrors()
    }

    /**
     * Aggiorna la gravità
     */
    fun updateSeverity(severity: FeedbackSeverity) {
        _severity.value = severity
    }

    /**
     * NUOVO: Aggiunge un attachment dalla URI
     */
    fun addAttachment(uri: Uri) {
        val currentContext = context
        if (currentContext == null) {
            _validationErrors.value = listOf("Errore di sistema: context non disponibile")
            return
        }

        val attachment = FileAttachmentManager.uriToLocalAttachment(currentContext, uri)
        if (attachment != null) {
            val currentAttachments = _attachments.value.toMutableList()

            // Controlla che non superi il limite
            if (currentAttachments.size >= 3) {
                _validationErrors.value = listOf("Massimo 3 file consentiti")
                return
            }

            // Valida il file
            val validation = FileAttachmentManager.validateAttachment(attachment)
            if (!validation.isValid) {
                _validationErrors.value = validation.errors
                return
            }

            currentAttachments.add(attachment)
            _attachments.value = currentAttachments
            clearValidationErrors()
        } else {
            _validationErrors.value = listOf("Impossibile leggere il file selezionato")
        }
    }

    /**
     * NUOVO: Rimuove un attachment
     */
    fun removeAttachment(index: Int) {
        val currentAttachments = _attachments.value.toMutableList()
        if (index in currentAttachments.indices) {
            currentAttachments.removeAt(index)
            _attachments.value = currentAttachments
            clearValidationErrors()
        }
    }

    /**
     * NUOVO: Pulisce tutti gli attachment
     */
    fun clearAttachments() {
        _attachments.value = emptyList()
        clearValidationErrors()
    }

    /**
     * Invia il feedback
     */
    fun submitFeedback() {
        viewModelScope.launch {
            if (!::repository.isInitialized) {
                _submitState.value = SubmitState.Error("Repository non inizializzato")
                return@launch
            }

            // Validazione (ora include anche gli attachment)
            val validation = repository.validateFeedback(
                title = _title.value,
                description = _description.value,
                email = _email.value,
                attachments = _attachments.value
            )

            if (!validation.isValid) {
                _validationErrors.value = validation.errors
                return@launch
            }

            // Invio
            _submitState.value = SubmitState.Loading

            val result = repository.submitFeedback(
                type = _feedbackType.value,
                title = _title.value,
                description = _description.value,
                email = _email.value,
                severity = _severity.value,
                attachments = _attachments.value // NUOVO: Passa gli attachment
            )

            result.fold(
                onSuccess = { response ->
                    if (response.success) {
                        _submitState.value = SubmitState.Success(response.message)
                        clearForm()
                    } else {
                        _submitState.value = SubmitState.Error(response.message)
                    }
                },
                onFailure = { error ->
                    _submitState.value = SubmitState.Error(
                        error.message ?: "Si è verificato un errore durante l'invio"
                    )
                }
            )
        }
    }

    /**
     * Resetta lo stato di invio
     */
    fun resetSubmitState() {
        _submitState.value = SubmitState.Idle
    }

    /**
     * Pulisce gli errori di validazione
     */
    private fun clearValidationErrors() {
        if (_validationErrors.value.isNotEmpty()) {
            _validationErrors.value = emptyList()
        }
    }

    /**
     * Pulisce il form dopo l'invio riuscito
     */
    private fun clearForm() {
        _feedbackType.value = FeedbackType.BUG
        _title.value = ""
        _description.value = ""
        _severity.value = FeedbackSeverity.MEDIUM
        _attachments.value = emptyList() // NUOVO: Pulisce anche gli attachment
        // Non pulire l'email per comodità dell'utente
    }

    /**
     * Imposta l'email dell'utente corrente
     */
    fun setUserEmail(email: String) {
        if (_email.value.isEmpty()) {
            _email.value = email
        }
    }

    /**
     * Imposta l'email basandosi su username se non c'è email
     */
    fun setUserEmailFromUsername(username: String) {
        if (_email.value.isEmpty()) {
            _email.value = "$username@fitgymtrack.com"
        }
    }

    /**
     * Stati possibili per l'invio del feedback
     */
    sealed class SubmitState {
        object Idle : SubmitState()
        object Loading : SubmitState()
        data class Success(val message: String) : SubmitState()
        data class Error(val message: String) : SubmitState()
    }
}