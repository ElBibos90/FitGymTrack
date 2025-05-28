package com.fitgymtrack.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitgymtrack.app.extensions.NotificationHelper
import com.fitgymtrack.app.extensions.WorkoutNotificationExtensions
import com.fitgymtrack.app.models.Subscription
import com.fitgymtrack.app.services.NotificationCleanupSystem
import com.fitgymtrack.app.services.NotificationIntegrationService
import com.fitgymtrack.app.ui.theme.Indigo600
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Schermata per testare l'integrazione completa Step 3
 * DA RIMUOVERE IN PRODUZIONE
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step3TestingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Servizi
    val integrationService = remember { NotificationIntegrationService.getInstance(context) }
    val cleanupSystem = remember { NotificationCleanupSystem.getInstance(context) }

    // Stati
    var cleanupStats by remember { mutableStateOf<com.fitgymtrack.app.services.CleanupStats?>(null) }
    var testResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // FIX: Funzione helper per aggiungere risultati
    fun addTestResult(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        testResults = testResults + "[$timestamp] $message"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 3 Integration Test") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "🚀 Step 3 Integration Tests",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Indigo600
                )
            }

            item {
                Text(
                    text = "Testa l'integrazione completa del sistema notifiche con banner sostituiti, notifiche automatiche e cleanup avanzato.",
                    color = Color.Gray
                )
            }

            // === SUBSCRIPTION INTEGRATION TESTS ===
            item {
                TestSection(
                    title = "🔔 Subscription Integration",
                    description = "Testa sostituzione banner con notifiche"
                ) {
                    TestButton(
                        text = "Test Subscription Expired",
                        icon = Icons.Default.Warning
                    ) {
                        coroutineScope.launch {
                            try {
                                integrationService.checkSubscriptionStatus(
                                    createMockExpiredSubscription()
                                )
                                addTestResult("✅ Subscription expired notification created")
                            } catch (e: Exception) {
                                addTestResult("❌ Error: ${e.message}")
                            }
                        }
                    }

                    TestButton(
                        text = "Test Subscription Expiring",
                        icon = Icons.Default.Schedule
                    ) {
                        coroutineScope.launch {
                            try {
                                integrationService.checkSubscriptionStatus(
                                    createMockExpiringSubscription()
                                )
                                addTestResult("✅ Subscription expiring notification created")
                            } catch (e: Exception) {
                                addTestResult("❌ Error: ${e.message}")
                            }
                        }
                    }

                    TestButton(
                        text = "Test Limit Reached",
                        icon = Icons.Default.Block
                    ) {
                        coroutineScope.launch {
                            try {
                                integrationService.checkResourceLimits(
                                    resourceType = "workouts",
                                    currentCount = 3,
                                    maxAllowed = 3,
                                    isLimitReached = true
                                )
                                addTestResult("✅ Limit reached notification created")
                            } catch (e: Exception) {
                                addTestResult("❌ Error: ${e.message}")
                            }
                        }
                    }
                }
            }

            // === WORKOUT INTEGRATION TESTS ===
            item {
                TestSection(
                    title = "💪 Workout Integration",
                    description = "Testa notifiche automatiche workout"
                ) {
                    TestButton(
                        text = "Test Workout Completed",
                        icon = Icons.Default.FitnessCenter
                    ) {
                        coroutineScope.launch {
                            try {
                                WorkoutNotificationExtensions.notifyWorkoutCompleted(
                                    context = context,
                                    workoutName = "Push Day Test",
                                    durationMinutes = 45,
                                    exerciseCount = 8
                                )
                                addTestResult("✅ Workout completed notification created")
                            } catch (e: Exception) {
                                addTestResult("❌ Workout notification error: ${e.message}")
                            }
                        }
                    }

                    TestButton(
                        text = "Test Long Workout Achievement",
                        icon = Icons.Default.Star
                    ) {
                        coroutineScope.launch {
                            try {
                                WorkoutNotificationExtensions.notifyWorkoutCompleted(
                                    context = context,
                                    workoutName = "Marathon Session",
                                    durationMinutes = 75,
                                    exerciseCount = 12
                                )
                                addTestResult("✅ Achievement notification created")
                            } catch (e: Exception) {
                                addTestResult("❌ Achievement error: ${e.message}")
                            }
                        }
                    }

                    TestButton(
                        text = "Test Limits Check",
                        icon = Icons.Default.Security
                    ) {
                        coroutineScope.launch {
                            try {
                                WorkoutNotificationExtensions.checkLimitsBeforeCreation(
                                    context = context,
                                    resourceType = "workouts",
                                    onLimitReached = {
                                        addTestResult("🚨 Limit reached callback triggered")
                                    },
                                    onCanProceed = {
                                        addTestResult("✅ Can proceed callback triggered")
                                    }
                                )
                                addTestResult("✅ Limits check initiated")
                            } catch (e: Exception) {
                                addTestResult("❌ Limits check error: ${e.message}")
                            }
                        }
                    }
                }
            }

            // === CLEANUP SYSTEM TESTS ===
            item {
                TestSection(
                    title = "🧹 Cleanup System",
                    description = "Testa sistema cleanup avanzato"
                ) {
                    TestButton(
                        text = "Run Full Cleanup",
                        icon = Icons.Default.CleaningServices
                    ) {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                cleanupSystem.performCleanup()
                                addTestResult("✅ Full cleanup completed")
                            } catch (e: Exception) {
                                addTestResult("❌ Cleanup error: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }

                    TestButton(text = "Get Cleanup Stats", icon = Icons.Default.Analytics) {
                        coroutineScope.launch {
                            try {
                                Log.d("DEBUG", "🔍 Inizio get cleanup stats")
                                cleanupStats = cleanupSystem.getCleanupStats()
                                Log.d("DEBUG", "✅ Stats ottenute: $cleanupStats")
                                addTestResult("📊 Cleanup stats loaded")
                            } catch (e: Exception) {
                                Log.e("DEBUG", "❌ Errore stats: ${e.message}", e)
                                addTestResult("❌ Stats error: ${e.message}")
                            }
                        }
                    }

                    TestButton(
                        text = "Cleanup Read Notifications",
                        icon = Icons.Default.MarkEmailRead
                    ) {
                        coroutineScope.launch {
                            try {
                                cleanupSystem.cleanupReadNotifications(3)
                                addTestResult("✅ Read notifications cleaned")
                            } catch (e: Exception) {
                                addTestResult("❌ Read cleanup error: ${e.message}")
                            }
                        }
                    }
                }
            }

            // === HELPER TESTS ===
            item {
                TestSection(
                    title = "🔧 Helper Functions",
                    description = "Testa funzioni helper e utility"
                ) {
                    TestButton(
                        text = "Create Test Notifications",
                        icon = Icons.Default.BugReport
                    ) {
                        coroutineScope.launch {
                            try {
                                NotificationHelper.createTestNotifications(context)
                                addTestResult("🧪 Test notifications created")
                            } catch (e: Exception) {
                                addTestResult("❌ Test creation error: ${e.message}")
                            }
                        }
                    }

                    TestButton(
                        text = "Initialize Service",
                        icon = Icons.Default.Rocket
                    ) {
                        coroutineScope.launch {
                            try {
                                integrationService.initialize()
                                addTestResult("🚀 Integration service initialized")
                            } catch (e: Exception) {
                                addTestResult("❌ Init error: ${e.message}")
                            }
                        }
                    }

                    TestButton(
                        text = "Quick Subscription Check",
                        icon = Icons.Default.Security
                    ) {
                        coroutineScope.launch {
                            try {
                                NotificationHelper.checkSubscriptionAndNotify(context)
                                addTestResult("✅ Quick subscription check completed")
                            } catch (e: Exception) {
                                addTestResult("❌ Quick check error: ${e.message}")
                            }
                        }
                    }

                    TestButton(
                        text = "Clear Test Results",
                        icon = Icons.Default.Clear
                    ) {
                        testResults = emptyList()
                        addTestResult("🧹 Test results cleared")
                    }
                }
            }

            // === CLEANUP STATS DISPLAY ===
            cleanupStats?.let { stats ->
                item {
                    StatsCard(stats = stats)
                }
            }

            // === TEST RESULTS ===
            if (testResults.isNotEmpty()) {
                item {
                    TestResultsCard(results = testResults)
                }
            }

            // Loading indicator
            if (isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Processing cleanup...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TestSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun TestButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Indigo600
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun StatsCard(stats: com.fitgymtrack.app.services.CleanupStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Cleanup Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Total", stats.totalNotifications.toString())
                StatItem("Unread", stats.unreadNotifications.toString())
                StatItem("Urgent", stats.urgentNotifications.toString())
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Expired", stats.expiredNotifications.toString())
                StatItem("Old", stats.oldNotifications.toString())
                StatItem("Health", "${stats.getHealthScore()}%")
            }

            if (stats.needsCleanup()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Sistema needs cleanup!",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Indigo600
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun TestResultsCard(results: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📝 Test Results (${results.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            results.takeLast(15).forEach { result ->
                Text(
                    text = result,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 2.dp),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            if (results.size > 15) {
                Text(
                    text = "... and ${results.size - 15} more results",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// === HELPER FUNCTIONS ===

private fun createMockExpiredSubscription(): Subscription {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -5) // Scaduto 5 giorni fa

    return Subscription(
        id = 1,
        user_id = 1,
        plan_id = 2,
        planName = "Premium",
        status = "expired",
        price = 4.99,
        maxWorkouts = null,
        maxCustomExercises = null,
        currentCount = 0,
        currentCustomExercises = 0,
        advancedStats = true,
        cloudBackup = true,
        noAds = true,
        start_date = "2024-01-01 00:00:00",
        end_date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)
    )
}

private fun createMockExpiringSubscription(): Subscription {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, 2) // Scade tra 2 giorni

    return Subscription(
        id = 1,
        user_id = 1,
        plan_id = 2,
        planName = "Premium",
        status = "active",
        price = 4.99,
        maxWorkouts = null,
        maxCustomExercises = null,
        currentCount = 0,
        currentCustomExercises = 0,
        advancedStats = true,
        cloudBackup = true,
        noAds = true,
        start_date = "2024-01-01 00:00:00",
        end_date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)
    )
}