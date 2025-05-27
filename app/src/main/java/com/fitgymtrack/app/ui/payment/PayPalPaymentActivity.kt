package com.fitgymtrack.app.ui.payment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.fitgymtrack.app.ui.theme.FitGymTrackTheme
import com.fitgymtrack.app.viewmodel.PaymentViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity per gestire i pagamenti PayPal
 */
class PayPalPaymentActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PayPalPayment"
        const val PAYMENT_SUCCESSFUL = "payment_successful"
        const val ORDER_ID = "order_id"
        const val ERROR_MESSAGE = "error_message"
        const val TIMEOUT = "timeout"
        const val CANCELLED = "cancelled"
    }

    private lateinit var viewModel: PaymentViewModel
    private var paymentOrderId: String? = null
    private var showWebView = mutableStateOf(false)
    private var approvalUrl = mutableStateOf("")
    private var isPaymentProcessing = mutableStateOf(false)
    private var statusCheckRunning = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inizializza il ViewModel
        viewModel = PaymentViewModel()

        // Recupera i dati dall'intent
        val amount = intent.getDoubleExtra("amount", 0.0)
        val type = intent.getStringExtra("type") ?: "subscription"
        val planId = if (intent.hasExtra("plan_id")) intent.getIntExtra("plan_id", 0) else null
        val message = intent.getStringExtra("message")
        val displayName = intent.getBooleanExtra("display_name", true)

        if (amount <= 0) {
            finishWithError("Importo non valido")
            return
        }

        setContent {
            FitGymTrackTheme {
                PayPalPaymentScreen(
                    onClose = { finish() }
                )
            }
        }

        // Osserva gli stati del ViewModel
        lifecycleScope.launch {
            viewModel.paymentInitState.collect { state ->
                when (state) {
                    is PaymentViewModel.PaymentInitState.Success -> {
                        Log.d(TAG, "Inizializzazione pagamento riuscita: ${state.response.order_id}")
                        // Salva l'ID dell'ordine per verifiche future
                        paymentOrderId = state.response.order_id
                        approvalUrl.value = state.response.approval_url
                        showWebView.value = true
                        isPaymentProcessing.value = true

                        // Avvia il polling per verificare lo stato del pagamento
                        startPaymentStatusPolling(state.response.order_id)
                    }
                    is PaymentViewModel.PaymentInitState.Error -> {
                        Log.e(TAG, "Errore inizializzazione pagamento: ${state.message}")
                        finishWithError(state.message)
                    }
                    is PaymentViewModel.PaymentInitState.Loading -> {
                        // Mostra il caricamento
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.paymentStatusState.collect { state ->
                if (state is PaymentViewModel.PaymentStatusState.Success) {
                    Log.d(TAG, "Stato pagamento: ${state.status.status}")
                    if (state.status.status == "completed") {
                        statusCheckRunning = false
                        isPaymentProcessing.value = false

                        // Invia il risultato e chiudi l'activity
                        val resultIntent = Intent()
                        resultIntent.putExtra(PAYMENT_SUCCESSFUL, true)
                        resultIntent.putExtra(ORDER_ID, state.status.order_id)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                }
            }
        }

        // Inizializza il pagamento PayPal
        viewModel.initializePayment(amount, type, planId, message, displayName)
    }

    private fun startPaymentStatusPolling(orderId: String) {
        if (statusCheckRunning) return

        statusCheckRunning = true

        lifecycleScope.launch {
            var attempts = 0
            while (statusCheckRunning && attempts < 20) { // Max 20 tentativi (5 minuti)
                delay(15000) // Controlla ogni 15 secondi
                attempts++

                try {
                    viewModel.checkPaymentStatus(orderId)
                } catch (e: Exception) {
                    Log.e(TAG, "Errore controllo stato: ${e.message}")
                }
            }

            // Se dopo 20 tentativi il pagamento non è ancora completato
            if (statusCheckRunning) {
                statusCheckRunning = false
                finishWithTimeout()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        statusCheckRunning = false
    }

    private fun finishWithError(message: String) {
        val resultIntent = Intent()
        resultIntent.putExtra(PAYMENT_SUCCESSFUL, false)
        resultIntent.putExtra(ERROR_MESSAGE, message)
        setResult(RESULT_CANCELED, resultIntent)
        finish()
    }

    private fun finishWithTimeout() {
        val resultIntent = Intent()
        resultIntent.putExtra(PAYMENT_SUCCESSFUL, false)
        resultIntent.putExtra(TIMEOUT, true)
        setResult(RESULT_CANCELED, resultIntent)
        finish()
    }

    private fun finishWithCancellation() {
        val resultIntent = Intent()
        resultIntent.putExtra(PAYMENT_SUCCESSFUL, false)
        resultIntent.putExtra(CANCELLED, true)
        setResult(RESULT_CANCELED, resultIntent)
        finish()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PayPalPaymentScreen(onClose: () -> Unit) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!showWebView.value) {
                // Mostra schermata di caricamento
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Inizializzazione pagamento...")
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedButton(onClick = onClose) {
                            Text("Annulla")
                        }
                    }
                }
            } else {
                // Mostra WebView con la pagina di pagamento PayPal
                Column(modifier = Modifier.fillMaxSize()) {
                    // Barra superiore
                    CenterAlignedTopAppBar(
                        title = { Text("Pagamento PayPal") },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (isPaymentProcessing.value) {
                                    finishWithCancellation()
                                } else {
                                    onClose()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Indietro"
                                )
                            }
                        }
                    )

                    // WebView
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val url = request?.url.toString()
                                            Log.d(TAG, "URL: $url")

                                            // Gestisci il ritorno dal pagamento PayPal
                                            if (url.contains("fitgymtrack://payment/success")) {
                                                // Il pagamento potrebbe essere completato, verifica lo stato
                                                paymentOrderId?.let {
                                                    viewModel.checkPaymentStatus(it)
                                                }
                                                return true
                                            } else if (url.contains("fitgymtrack://payment/cancel")) {
                                                // Pagamento annullato
                                                finishWithCancellation()
                                                return true
                                            }

                                            // Lascia che il WebView gestisca gli altri URL
                                            return false
                                        }
                                    }
                                    loadUrl(approvalUrl.value)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Indicatore di caricamento sovrapposto
                        if (isPaymentProcessing.value) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .align(Alignment.TopCenter)
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}