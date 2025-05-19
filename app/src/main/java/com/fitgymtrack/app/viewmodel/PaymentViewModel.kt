package com.fitgymtrack.app.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitgymtrack.app.models.PaymentResponse
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.repository.PaymentRepository
import com.fitgymtrack.app.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for handling payments and subscriptions
 */
class PaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val subscriptionRepository = SubscriptionRepository()
    private val paymentRepository = PaymentRepository()

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Initial)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Initial)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    /**
     * Load the current subscription
     */
    fun loadSubscription() {
        _subscriptionState.value = SubscriptionState.Loading

        viewModelScope.launch {
            try {
                val result = subscriptionRepository.getCurrentSubscription()

                result.fold(
                    onSuccess = { subscription ->
                        _subscriptionState.value = SubscriptionState.Success(subscription)
                    },
                    onFailure = { error ->
                        _subscriptionState.value = SubscriptionState.Error(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _subscriptionState.value = SubscriptionState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Initialize a subscription payment with PayPal
     *
     * @param userId User ID
     * @param planId Plan ID
     * @param amount Payment amount
     */
    fun initiateSubscriptionPayment(userId: Int, planId: Int, amount: Double) {
        _paymentState.value = PaymentState.Loading

        viewModelScope.launch {
            try {
                val result = paymentRepository.initializeSubscriptionPayment(
                    userId = userId,
                    planId = planId,
                    amount = amount
                )

                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.approval_url != null) {
                            // Open the approval URL in a browser
                            openPaymentUrl(response.approval_url)
                            _paymentState.value = PaymentState.Success(response)
                        } else {
                            _paymentState.value = PaymentState.Error(
                                response.message ?: "Failed to initialize payment"
                            )
                        }
                    },
                    onFailure = { error ->
                        _paymentState.value = PaymentState.Error(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Initialize a donation with PayPal
     *
     * @param userId User ID
     * @param amount Donation amount
     * @param message Optional message
     * @param displayName Whether to display the user's name
     */
    fun initiateDonation(userId: Int, amount: Double, message: String? = null, displayName: Boolean = true) {
        _paymentState.value = PaymentState.Loading

        viewModelScope.launch {
            try {
                val result = paymentRepository.initializeDonation(
                    userId = userId,
                    amount = amount,
                    message = message,
                    displayName = displayName
                )

                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.approval_url != null) {
                            // Open the approval URL in a browser
                            openPaymentUrl(response.approval_url)
                            _paymentState.value = PaymentState.Success(response)
                        } else {
                            _paymentState.value = PaymentState.Error(
                                response.message ?: "Failed to initialize donation"
                            )
                        }
                    },
                    onFailure = { error ->
                        _paymentState.value = PaymentState.Error(error.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Open the PayPal approval URL in a browser
     *
     * @param url The PayPal approval URL
     */
    private fun openPaymentUrl(url: String) {
        try {
            // Try to use Chrome Custom Tabs first (better user experience)
            try {
                val builder = CustomTabsIntent.Builder()
                val customTabsIntent = builder.build()
                customTabsIntent.launchUrl(context, Uri.parse(url))
            } catch (e: Exception) {
                // Fallback to regular browser intent
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("PaymentViewModel", "Failed to open browser", e)
            _paymentState.value = PaymentState.Error("Failed to open browser: ${e.message}")
        }
    }

    fun resetPaymentState() {
        _paymentState.value = PaymentState.Initial
    }

    // States for different operations
    sealed class PaymentState {
        object Initial : PaymentState()
        object Loading : PaymentState()
        data class Success(val response: PaymentResponse) : PaymentState()
        data class Error(val message: String) : PaymentState()
    }

    sealed class SubscriptionState {
        object Initial : SubscriptionState()
        object Loading : SubscriptionState()
        data class Success(val subscription: Subscription) : SubscriptionState()
        data class Error(val message: String) : SubscriptionState()
    }
}