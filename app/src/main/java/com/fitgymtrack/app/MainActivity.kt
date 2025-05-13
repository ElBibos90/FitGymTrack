package com.fitgymtrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.fitgymtrack.app.models.User
import com.fitgymtrack.app.ui.AppNavigation
import com.fitgymtrack.app.ui.components.ImprovedTopBar
import com.fitgymtrack.app.ui.theme.FitGymTrackTheme
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.utils.ThemeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themeManager = ThemeManager(applicationContext)

        setContent {
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

            // Stato per rilevare lo scroll
            var isScrolled by remember { mutableStateOf(false) }

            // Ottieni il tema corrente
            val themeMode by themeManager.themeFlow.collectAsState(initial = ThemeManager.ThemeMode.SYSTEM)
            val isDarkTheme = when (themeMode) {
                ThemeManager.ThemeMode.LIGHT -> false
                ThemeManager.ThemeMode.DARK -> true
                ThemeManager.ThemeMode.SYSTEM -> {
                    (context.resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                            android.content.res.Configuration.UI_MODE_NIGHT_YES)
                }
            }

            FitGymTrackTheme(themeManager = themeManager) {
                Scaffold(
                    topBar = {
                        ImprovedTopBar(
                            user = currentUser,
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = {
                                coroutineScope.launch {
                                    themeManager.toggleTheme()
                                }
                            },
                            onNavigateToProfile = {
                                // Implementare la navigazione al profilo
                            },
                            onNavigateToNotifications = {
                                // Implementare la navigazione alle notifiche
                            },
                            isScrolled = isScrolled
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}