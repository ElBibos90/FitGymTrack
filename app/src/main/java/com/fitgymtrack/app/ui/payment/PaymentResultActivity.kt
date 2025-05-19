package com.fitgymtrack.app.ui.payment

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.fitgymtrack.app.viewmodel.PaymentViewModel
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Activity per gestire i risultati di pagamento tramite deep link
 * Questa Activity viene aperta quando l'utente ritorna dall'app PayPal
 * attraverso il meccanismo di deep linking
 */
class PaymentResultActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PaymentResult"
    }

    private lateinit var viewModel: PaymentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[PaymentViewModel::class.java]

        // Processa il deep link
        processDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processDeepLink(intent)
    }

    private fun processDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null) {
            Log.d(TAG, "Deep link ricevuto: $data")

            val path = data.path
            val orderId = data.getQueryParameter("order_id")

            if (orderId != null) {
                when (path) {
                    "/payment/success" -> {
                        // Verifica lo stato del pagamento
                        checkPaymentStatus(orderId)
                    }
                    "/payment/cancel" -> {
                        // Pagamento annullato
                        Log.d(TAG, "Pagamento annullato: $orderId")
                        navigateBack(false, null, "Pagamento annullato")
                    }
                    else -> {
                        // Path non riconosciuto
                        Log.d(TAG, "Path non riconosciuto: $path")
                        navigateBack(false, null, "Errore nel processo di pagamento")
                    }
                }
            } else {
                // orderId mancante
                Log.d(TAG, "Order ID mancante nel deep link")
                navigateBack(false, null, "Informazioni di pagamento mancanti")
            }
        } else {
            // Nessun dato nel deep link
            Log.d(TAG, "Nessun dato nel deep link")
            navigateBack(false, null, "Errore nel processo di pagamento")
        }
    }

    private fun checkPaymentStatus(orderId: String) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Verifica stato pagamento: $orderId")
                viewModel.checkPaymentStatus(orderId)

                // Osserva lo stato del pagamento
                viewModel.paymentStatusState.collect { state ->
                    when (state) {
                        is PaymentViewModel.PaymentStatusState.Success -> {
                            if (state.status.status == "completed") {
                                // Pagamento completato con successo
                                Log.d(TAG, "Pagamento completato: ${state.status.order_id}")
                                navigateBack(true, state.status.order_id, null)
                            } else {
                                // Pagamento non ancora completato
                                Log.d(TAG, "Pagamento non completato: ${state.status.status}")
                                navigateBack(false, orderId, "Pagamento in attesa di conferma")
                            }
                        }
                        is PaymentViewModel.PaymentStatusState.Error -> {
                            // Errore nella verifica dello stato
                            Log.e(TAG, "Errore verifica stato: ${state.message}")
                            navigateBack(false, orderId, state.message)
                        }
                        else -> {
                            // Altri stati
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Eccezione durante la verifica dello stato", e)
                navigateBack(false, orderId, e.message ?: "Errore sconosciuto")
            }
        }
    }

    private fun navigateBack(success: Boolean, orderId: String?, errorMessage: String?) {
        // Crea un Intent che sarà inviato alla MainActivity o alla Activity che ha avviato il pagamento
        val intent = Intent()
        intent.putExtra(PayPalPaymentActivity.PAYMENT_SUCCESSFUL, success)

        if (orderId != null) {
            intent.putExtra(PayPalPaymentActivity.ORDER_ID, orderId)
        }

        if (errorMessage != null) {
            intent.putExtra(PayPalPaymentActivity.ERROR_MESSAGE, errorMessage)
        }

        setResult(if (success) RESULT_OK else RESULT_CANCELED, intent)
        finish()
    }
}