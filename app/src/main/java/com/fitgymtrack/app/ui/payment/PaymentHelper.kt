package com.fitgymtrack.app.ui.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.fitgymtrack.app.ui.payment.PayPalPaymentActivity.Companion.ERROR_MESSAGE
import com.fitgymtrack.app.ui.payment.PayPalPaymentActivity.Companion.ORDER_ID
import com.fitgymtrack.app.ui.payment.PayPalPaymentActivity.Companion.PAYMENT_SUCCESSFUL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Helper per avviare il processo di pagamento PayPal
 */
object PaymentHelper {

    /**
     * Avvia il pagamento PayPal dall'activity chiamante
     *
     * @param context Il context dell'activity chiamante
     * @param amount L'importo del pagamento
     * @param type Il tipo di pagamento (default: "subscription")
     * @param planId L'ID del piano di abbonamento (opzionale)
     * @param message Un messaggio opzionale per il pagamento
     * @param displayName Se mostrare il nome dell'utente nella pagina di pagamento
     * @param resultLauncher L'ActivityResultLauncher per ricevere il risultato
     * @return true se il pagamento è stato avviato, false altrimenti
     */
    fun startPayPalPayment(
        context: Context,
        amount: Double,
        type: String = "subscription",
        planId: Int? = null,
        message: String? = null,
        displayName: Boolean = true,
        resultLauncher: ActivityResultLauncher<Intent>
    ): Boolean {
        if (amount <= 0) {
            Toast.makeText(context, "Importo non valido", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            val intent = Intent(context, PayPalPaymentActivity::class.java).apply {
                putExtra("amount", amount)
                putExtra("type", type)
                if (planId != null) {
                    putExtra("plan_id", planId)
                }
                if (message != null) {
                    putExtra("message", message)
                }
                putExtra("display_name", displayName)
            }

            resultLauncher.launch(intent)
            return true
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Errore nell'avvio del pagamento: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
    }

    /**
     * Elabora il risultato del pagamento
     *
     * @param resultCode Il codice risultato dell'Activity
     * @param data L'Intent con i dati risultanti
     * @param onSuccess Callback da eseguire in caso di successo
     * @param onFailure Callback da eseguire in caso di fallimento
     * @return true se il risultato è stato elaborato, false altrimenti
     */
    fun processPaymentResult(
        resultCode: Int,
        data: Intent?,
        onSuccess: (orderId: String) -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ): Boolean {
        return if (resultCode == Activity.RESULT_OK && data != null) {
            val isSuccessful = data.getBooleanExtra(PAYMENT_SUCCESSFUL, false)

            if (isSuccessful) {
                val orderId = data.getStringExtra(ORDER_ID) ?: ""
                onSuccess(orderId)
                true
            } else {
                val errorMessage = data.getStringExtra(ERROR_MESSAGE) ?: "Pagamento fallito"
                onFailure(errorMessage)
                false
            }
        } else {
            onFailure("Pagamento annullato o fallito")
            false
        }
    }
}