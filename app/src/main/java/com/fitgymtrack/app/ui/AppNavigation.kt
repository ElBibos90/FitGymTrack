package com.fitgymtrack.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitgymtrack.app.models.User
import com.fitgymtrack.app.ui.screens.ActiveWorkoutScreen
import com.fitgymtrack.app.ui.screens.CreateWorkoutScreen
import com.fitgymtrack.app.ui.screens.EditWorkoutScreen
import com.fitgymtrack.app.ui.screens.FeedbackScreen
import com.fitgymtrack.app.ui.screens.ForgotPasswordScreen
import com.fitgymtrack.app.ui.screens.LoginScreen
import com.fitgymtrack.app.ui.screens.NotificationScreen
import com.fitgymtrack.app.ui.screens.NotificationTestScreen
import com.fitgymtrack.app.ui.screens.RegisterScreen
import com.fitgymtrack.app.ui.screens.SimpleResetPasswordScreen
import com.fitgymtrack.app.ui.screens.StatsScreen
import com.fitgymtrack.app.ui.screens.Step3TestingScreen // AGGIUNTO: Import Step3TestingScreen
import com.fitgymtrack.app.ui.screens.SubscriptionScreen
import com.fitgymtrack.app.ui.screens.UserExerciseScreen
import com.fitgymtrack.app.ui.screens.UserProfileScreen
import com.fitgymtrack.app.ui.screens.WorkoutHistoryScreen
import com.fitgymtrack.app.ui.screens.WorkoutPlansScreen
import com.fitgymtrack.app.ui.screens.WorkoutsScreen
import com.fitgymtrack.app.ui.theme.FitGymTrackTheme
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.utils.ThemeManager
import com.fitgymtrack.app.viewmodel.StatsViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "login",
    themeManager: ThemeManager? = null
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // Osserva l'utente dalla sessione
    var currentUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(key1 = Unit) {
        sessionManager.getUserData().collect { user ->
            currentUser = user
        }
    }

    // Mostra la TopBar solo quando non siamo nella schermata di login o registrazione
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    currentRoute != null &&
            currentRoute != "login" &&
            currentRoute != "register" &&
            currentRoute != "profile" &&
            currentRoute != "forgot_password" &&
            !currentRoute.toString().startsWith("reset_password") &&
            !currentRoute.toString().startsWith("create_workout") &&
            !currentRoute.toString().startsWith("edit_workout") &&
            !currentRoute.toString().startsWith("user_exercises") &&
            !currentRoute.toString().startsWith("active_workout") &&
            currentRoute != "stats" &&
            currentRoute != "feedback"

    // Ottieni il tema corrente
    val themeMode = if (themeManager != null) {
        themeManager.themeFlow.collectAsState(initial = ThemeManager.ThemeMode.SYSTEM).value
    } else {
        ThemeManager.ThemeMode.SYSTEM
    }

    when (themeMode) {
        ThemeManager.ThemeMode.LIGHT -> false
        ThemeManager.ThemeMode.DARK -> true
        ThemeManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        else -> isSystemInDarkTheme()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            // Forza tema chiaro solo per il login
            FitGymTrackTheme(darkTheme = false) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    navigateToRegister = {
                        navController.navigate("register")
                    },
                    navigateToForgotPassword = {
                        navController.navigate("forgot_password")
                    }
                )
            }
        }

        composable("register") {
            FitGymTrackTheme(darkTheme = false) {
                RegisterScreen(
                    navigateToLogin = {
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
                    }
                )
            }
        }

        composable("dashboard") {
            // NUOVO: Crea un'istanza condivisa di StatsViewModel
            val sharedStatsViewModel: StatsViewModel = viewModel()

            Dashboard(
                onLogout = {
                    coroutineScope.launch {
                        sessionManager.clearSession()
                        navController.navigate("login") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                },
                onNavigateToWorkoutPlans = {
                    navController.navigate("workout_plans")
                },
                onNavigateToUserExercises = {
                    navController.navigate("user_exercises")
                },
                onNavigateToWorkouts = {
                    navController.navigate("workouts")
                },
                onNavigateToSubscription = {
                    navController.navigate("subscription")
                },
                onNavigateToStats = {
                    navController.navigate("stats")
                },
                onNavigateToFeedback = {
                    navController.navigate("feedback")
                },
                // FIX: Aggiunto callback mancante per notification test
                onNavigateToNotificationTest = {
                    navController.navigate("notification_test")
                },
                // FIX: Aggiunto callback mancante per step 3 test
                onNavigateToStep3Test = {
                    navController.navigate("step3_test")
                },
                statsViewModel = sharedStatsViewModel
            )
        }

        composable("profile") {
            UserProfileScreen(
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                navigateBack = {
                    navController.popBackStack()
                },
                navigateToResetPassword = { token ->
                    navController.navigate("reset_password/$token")
                }
            )
        }

        composable("notifications") {
            NotificationScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToSubscription = {
                    navController.navigate("subscription")
                },
                onNavigateToWorkouts = {
                    navController.navigate("workouts")
                },
                onNavigateToStats = {
                    navController.navigate("stats")
                }
            )
        }

        // Schermata abbonamento
        composable("subscription") {
            SubscriptionScreen(
                themeManager = themeManager,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Schermata statistiche
        composable("stats") {
            StatsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Schermata feedback
        composable("feedback") {
            FeedbackScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // FIX: Test notifiche base (Step 1)
        composable("notification_test") {
            NotificationTestScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // FIX: Test Step 3 - AGGIUNTO
        composable("step3_test") {
            Step3TestingScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "reset_password/{token}",
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            SimpleResetPasswordScreen(
                token = token,
                navigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("reset_password/$token") { inclusive = true }
                    }
                }
            )
        }

        // Rotta per la lista delle schede
        composable("workout_plans") {
            WorkoutPlansScreen(
                onBack = {
                    navController.navigate("dashboard") {
                        popUpTo("workout_plans") { inclusive = true }
                    }
                },
                onCreateWorkout = {
                    navController.navigate("create_workout")
                },
                onEditWorkout = { schedaId ->
                    navController.navigate("edit_workout/$schedaId")
                },
                onStartWorkout = { schedaId ->
                    currentUser?.let { user ->
                        navController.navigate("active_workout/${schedaId}/${user.id}")
                    }
                }
            )
        }

        // Rotta per la creazione di una scheda
        composable("create_workout") {
            CreateWorkoutScreen(
                onBack = {
                    navController.popBackStack()
                },
                onWorkoutCreated = {
                    navController.navigate("workout_plans") {
                        popUpTo("workout_plans") { inclusive = false }
                    }
                }
            )
        }

        // Rotta per la modifica di una scheda
        composable(
            route = "edit_workout/{schedaId}",
            arguments = listOf(navArgument("schedaId") { type = NavType.IntType })
        ) { backStackEntry ->
            val schedaId = backStackEntry.arguments?.getInt("schedaId") ?: 0
            EditWorkoutScreen(
                schedaId = schedaId,
                onBack = {
                    navController.popBackStack()
                },
                onWorkoutUpdated = {
                    navController.navigate("workout_plans") {
                        popUpTo("workout_plans") { inclusive = false }
                    }
                }
            )
        }

        composable("workouts") {
            WorkoutsScreen(
                onBack = {
                    navController.navigate("dashboard") {
                        popUpTo("workouts") { inclusive = true }
                    }
                },
                onStartWorkout = { schedaId ->
                    currentUser?.let { user ->
                        navController.navigate("active_workout/${schedaId}/${user.id}")
                    }
                },
                onNavigateToHistory = {
                    navController.navigate("workout_history")
                }
            )
        }

        // Storico degli allenamenti
        composable("workout_history") {
            WorkoutHistoryScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Rotta per l'allenamento attivo
        composable(
            route = "active_workout/{schedaId}/{userId}",
            arguments = listOf(
                navArgument("schedaId") { type = NavType.IntType },
                navArgument("userId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val schedaId = backStackEntry.arguments?.getInt("schedaId") ?: 0
            val userId = backStackEntry.arguments?.getInt("userId") ?: 0

            ActiveWorkoutScreen(
                schedaId = schedaId,
                userId = userId,
                onNavigateBack = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = false }
                    }
                },
                onWorkoutCompleted = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = false }
                    }
                }
            )
        }

        // Rotta per gli esercizi personalizzati
        composable("user_exercises") {
            UserExerciseScreen(
                onBack = {
                    navController.navigate("dashboard") {
                        popUpTo("user_exercises") { inclusive = true }
                    }
                }
            )
        }
    }
}