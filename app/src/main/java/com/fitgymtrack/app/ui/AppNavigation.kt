package com.fitgymtrack.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitgymtrack.app.models.User
import com.fitgymtrack.app.ui.screens.*
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.utils.ThemeManager
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
    val showTopBar = currentRoute != null &&
            currentRoute != "login" &&
            currentRoute != "register" &&
            currentRoute != "profile" &&
            currentRoute != "forgot_password" &&
            !currentRoute.toString().startsWith("reset_password") &&
            !currentRoute.toString().startsWith("create_workout") &&
            !currentRoute.toString().startsWith("edit_workout") &&
            !currentRoute.toString().startsWith("user_exercises") &&
            !currentRoute.toString().startsWith("active_workout") // Aggiungiamo anche la nuova rotta

    // Ottieni il tema corrente
    val themeMode = if (themeManager != null) {
        themeManager.themeFlow.collectAsState(initial = ThemeManager.ThemeMode.SYSTEM).value
    } else {
        ThemeManager.ThemeMode.SYSTEM
    }

    val isDarkTheme = when (themeMode) {
        ThemeManager.ThemeMode.LIGHT -> false
        ThemeManager.ThemeMode.DARK -> true
        ThemeManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        else -> isSystemInDarkTheme()
    }

    // Stato per lo scrolling
    var isScrolled by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
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

        composable("register") {
            RegisterScreen(
                navigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
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
                }
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
            // Implementare la schermata delle notifiche
            Box(modifier = Modifier) {
                // Placeholder per la schermata di notifiche
            }
        }

        composable("subscription") {
            SubscriptionScreen(
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
                    // MODIFICATO: Assicurarsi che torni alla dashboard
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
                    // Navigazione verso la nuova schermata di allenamento attivo
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
                    // Torna alla lista delle schede
                    navController.popBackStack()
                },
                onWorkoutCreated = {
                    // MODIFICATO: Naviga alla lista delle schede dopo la creazione
                    // rimuovendo create_workout dal backstack
                    navController.navigate("workout_plans") {
                        // Rimuove tutte le destinazioni fino a workout_plans (esclusa)
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
            // Implementazione della schermata di modifica scheda
            EditWorkoutScreen(
                schedaId = schedaId,
                onBack = {
                    // Torna alla lista delle schede
                    navController.popBackStack()
                },
                onWorkoutUpdated = {
                    // MODIFICATO: Naviga alla lista delle schede dopo l'aggiornamento
                    // rimuovendo edit_workout dal backstack
                    navController.navigate("workout_plans") {
                        // Rimuove tutte le destinazioni fino a workout_plans (esclusa)
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
                    // Navigazione verso la schermata di allenamento attivo
                    currentUser?.let { user ->
                        navController.navigate("active_workout/${schedaId}/${user.id}")
                    }
                },
                onNavigateToHistory = {
                    navController.navigate("workout_history")
                }
            )
        }

// Aggiungiamo anche la rotta per lo storico degli allenamenti
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
                    // Torna alla lista delle schede
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = false }
                    }
                },
                onWorkoutCompleted = {
                    // Naviga alla dashboard dopo il completamento dell'allenamento
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = false }
                    }
                }
            )
        }

        // Nuova rotta per gli esercizi personalizzati
        composable("user_exercises") {
            UserExerciseScreen(
                onBack = {
                    // Torna alla dashboard
                    navController.navigate("dashboard") {
                        popUpTo("user_exercises") { inclusive = true }
                    }
                }
            )
        }
    }
}