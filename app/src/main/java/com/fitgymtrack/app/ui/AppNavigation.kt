package com.fitgymtrack.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import com.fitgymtrack.app.ui.components.ImprovedTopBar
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.utils.ThemeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
            !currentRoute.toString().startsWith("reset_password")

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

        composable(
            route = "reset_password/{token}",
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            ResetPasswordScreen(
                token = token,
                navigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("reset_password/$token") { inclusive = true }
                    }
                }
            )
        }

        composable("notifications") {
            // Implementare la schermata delle notifiche
            Box(modifier = Modifier) {
                // Placeholder per la schermata di notifiche
            }
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
    }
}