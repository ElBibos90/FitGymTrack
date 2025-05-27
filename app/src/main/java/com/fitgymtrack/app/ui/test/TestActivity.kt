package com.fitgymtrack.app.ui.test

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.repository.PaymentRepository
import com.fitgymtrack.app.repository.SubscriptionRepository
import com.fitgymtrack.app.ui.components.SubscriptionLimitBanner
import com.fitgymtrack.app.ui.theme.FitGymTrackTheme
import com.fitgymtrack.app.utils.SubscriptionLimitChecker
import com.fitgymtrack.app.viewmodel.PaymentViewModel
import com.fitgymtrack.app.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.launch

/**
 * Attività di test per verificare i componenti di abbonamento e pagamento
 * Prima di integrare questi componenti nell'applicazione principale
 */
class TestActivity : ComponentActivity() {

    private val TAG = "TestActivity"

    // Repository
    private val subscriptionRepository = SubscriptionRepository()
    private val paymentRepository = PaymentRepository()

    // ViewModel
    private val subscriptionViewModel = SubscriptionViewModel()
    private val paymentViewModel = PaymentViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FitGymTrackTheme {
                TestScreen(
                    onBackClick = { finish() },
                    onSubscriptionTest = { testSubscription() },
                    onLimitTest = { testLimits() },
                    onPaymentTest = { testPayment() },
                    onViewModelTest = { testViewModel() }
                )
            }
        }
    }

    // Test di SubscriptionRepository
    private fun testSubscription() {
        Log.d(TAG, "Test Subscription Repository...")

        lifecycleScope.launch {
            try {
                val result = subscriptionRepository.getCurrentSubscription()

                result.fold(
                    onSuccess = { subscription ->
                        Log.d(TAG, "Subscription fetched successfully: ${subscription.planName}")
                        Log.d(TAG, "Current plan: ${subscription.planName}")
                        Log.d(TAG, "Price: ${subscription.price}€")
                        Log.d(TAG, "Max workouts: ${subscription.maxWorkouts ?: "unlimited"}")
                        Log.d(TAG, "Max custom exercises: ${subscription.maxCustomExercises ?: "unlimited"}")
                        Log.d(TAG, "Current workouts: ${subscription.currentCount}")
                        Log.d(TAG, "Current custom exercises: ${subscription.currentCustomExercises}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to fetch subscription: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception during subscription test: ${e.message}", e)
            }
        }
    }

    // Test di SubscriptionLimitChecker
    private fun testLimits() {
        Log.d(TAG, "Test Subscription Limits...")

        lifecycleScope.launch {
            try {
                // Test limits for workouts
                val (workoutLimitReached, workoutCurrentCount, workoutMaxAllowed) =
                    SubscriptionLimitChecker.canCreateWorkout()

                Log.d(TAG, "Workout limits: reached=$workoutLimitReached, current=$workoutCurrentCount, max=${workoutMaxAllowed ?: "unlimited"}")

                // Test limits for custom exercises
                val (exerciseLimitReached, exerciseCurrentCount, exerciseMaxAllowed) =
                    SubscriptionLimitChecker.canCreateCustomExercise()

                Log.d(TAG, "Custom exercise limits: reached=$exerciseLimitReached, current=$exerciseCurrentCount, max=${exerciseMaxAllowed ?: "unlimited"}")
            } catch (e: Exception) {
                Log.e(TAG, "Exception during limits test: ${e.message}", e)
            }
        }
    }

    // Test di PaymentRepository
    private fun testPayment() {
        Log.d(TAG, "Test Payment Repository...")

        lifecycleScope.launch {
            try {
                // Initialize a test payment with a small amount
                val result = paymentRepository.initializePayment(
                    amount = 0.01,  // Use minimal amount for testing
                    type = "test",
                    planId = null,
                    message = "Test payment",
                    displayName = true
                )

                result.fold(
                    onSuccess = { response ->
                        Log.d(TAG, "Payment initialized successfully")
                        Log.d(TAG, "Order ID: ${response.order_id}")
                        Log.d(TAG, "PayPal Order ID: ${response.paypal_order_id}")
                        Log.d(TAG, "Approval URL: ${response.approval_url}")

                        // Option to open the approval URL in a browser for testing
                        // Note: In a real app, we would use a WebView inside PayPalPaymentActivity
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(response.approval_url))
                        startActivity(browserIntent)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to initialize payment: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception during payment test: ${e.message}", e)
            }
        }
    }

    // Test di SubscriptionViewModel
    private fun testViewModel() {
        Log.d(TAG, "Test ViewModel...")

        // Load subscription
        subscriptionViewModel.loadSubscription()

        // Collect state flows
        lifecycleScope.launch {
            subscriptionViewModel.subscriptionState.collect { state ->
                when (state) {
                    is SubscriptionViewModel.SubscriptionState.Loading -> {
                        Log.d(TAG, "ViewModel: Loading subscription...")
                    }
                    is SubscriptionViewModel.SubscriptionState.Success -> {
                        Log.d(TAG, "ViewModel: Subscription loaded successfully")
                        Log.d(TAG, "Subscription: ${state.subscription.planName}")
                    }
                    is SubscriptionViewModel.SubscriptionState.Error -> {
                        Log.e(TAG, "ViewModel: Error loading subscription: ${state.message}")
                    }
                    else -> {}
                }
            }
        }

        // Check resource limits
        lifecycleScope.launch {
            subscriptionViewModel.checkLimits("max_workouts")

            subscriptionViewModel.resourceLimitState.collect { state ->
                when (state) {
                    is SubscriptionViewModel.ResourceLimitState.Loading -> {
                        Log.d(TAG, "ViewModel: Checking limits...")
                    }
                    is SubscriptionViewModel.ResourceLimitState.Success -> {
                        Log.d(TAG, "ViewModel: Limits checked successfully")
                        Log.d(TAG, "Limit reached: ${state.limitReached}")
                        Log.d(TAG, "Current count: ${state.currentCount}")
                        Log.d(TAG, "Max allowed: ${state.maxAllowed ?: "unlimited"}")
                        Log.d(TAG, "Remaining: ${state.remaining}")
                    }
                    is SubscriptionViewModel.ResourceLimitState.Error -> {
                        Log.e(TAG, "ViewModel: Error checking limits: ${state.message}")
                    }
                    else -> {}
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    onBackClick: () -> Unit,
    onSubscriptionTest: () -> Unit,
    onLimitTest: () -> Unit,
    onPaymentTest: () -> Unit,
    onViewModelTest: () -> Unit
) {
    val scrollState = rememberScrollState()

    // State for UI displaying results
    var showLimitBanner by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Subscription & Payment") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Test Components",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Test Repository Button
            TestButton(
                title = "Test Subscription Repository",
                description = "Tests fetching the current subscription",
                onClick = onSubscriptionTest
            )

            // Test Limits Button
            TestButton(
                title = "Test Subscription Limits",
                description = "Tests checking if user has reached limits",
                onClick = {
                    onLimitTest()
                    showLimitBanner = true
                }
            )

            // Test Payment Button
            TestButton(
                title = "Test Payment Repository",
                description = "Tests initializing a PayPal payment",
                onClick = onPaymentTest
            )

            // Test ViewModel Button
            TestButton(
                title = "Test ViewModel",
                description = "Tests the SubscriptionViewModel state flow",
                onClick = onViewModelTest
            )

            HorizontalDivider()

            Text(
                text = "UI Component Examples",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // Show limit banner
            if (showLimitBanner) {
                SubscriptionLimitBanner(
                    resourceType = "max_workouts",
                    maxAllowed = 3,
                    onDismiss = { showLimitBanner = false },
                    onUpgrade = { /* No-op */ }
                )
            }

            // Show subscription card example
            ExampleSubscriptionCard(
                subscription = Subscription(
                    plan_id = 1,
                    planName = "Free",
                    price = 0.0,
                    maxWorkouts = 3,
                    maxCustomExercises = 5,
                    currentCount = 2,
                    currentCustomExercises = 3
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TestButton(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Run Test")
            }
        }
    }
}

@Composable
fun ExampleSubscriptionCard(subscription: Subscription) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Current Subscription",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Plan:")
                Text(
                    text = subscription.planName,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Price:")
                Text(
                    text = if (subscription.price > 0) "${subscription.price}€/month" else "Free",
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Usage:",
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Workout plans:")
                    Text(
                        text = "${subscription.currentCount}/${subscription.maxWorkouts ?: "∞"}"
                    )
                }

                LinearProgressIndicator(
                    progress = { if (subscription.maxWorkouts != null)
                        (subscription.currentCount.toFloat() / subscription.maxWorkouts).coerceIn(0f, 1f)
                    else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Custom exercises:")
                    Text(
                        text = "${subscription.currentCustomExercises}/${subscription.maxCustomExercises ?: "∞"}"
                    )
                }

                LinearProgressIndicator(
                    progress = { if (subscription.maxWorkouts != null)
                        (subscription.currentCount.toFloat() / subscription.maxWorkouts).coerceIn(0f, 1f)
                    else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}