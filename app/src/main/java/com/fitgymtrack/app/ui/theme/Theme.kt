package com.fitgymtrack.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.fitgymtrack.app.utils.ThemeManager
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView



private val DarkColorScheme = darkColorScheme(
    primary = Indigo600,
    secondary = PurpleSecondary,
    tertiary = GreenPrimary,
    background = Gray900,
    surface = Gray800,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Gray100,
    onSurface = Gray200
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    secondary = PurpleSecondary,
    tertiary = GreenPrimary,
    background = Color.White,
    surface = Gray50,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Gray900,
    onSurface = Gray800
)

@Composable
fun FitGymTrackTheme(
    themeManager: ThemeManager? = null,
    darkTheme: Boolean = when {
        themeManager != null -> {
            val themePreference by themeManager.themeFlow.collectAsState(initial = ThemeManager.ThemeMode.SYSTEM)
            when (themePreference) {
                ThemeManager.ThemeMode.LIGHT -> false
                ThemeManager.ThemeMode.DARK -> true
                ThemeManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
        }
        else -> isSystemInDarkTheme()
    },
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Rendi la status bar trasparente
            window.statusBarColor = android.graphics.Color.TRANSPARENT

            // Abilita il layout edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Gestisci l'aspetto delle icone della status bar
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}