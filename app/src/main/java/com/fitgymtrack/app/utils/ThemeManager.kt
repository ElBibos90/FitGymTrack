package com.fitgymtrack.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemeManager(private val context: Context) {

    enum class ThemeMode(val value: Int) {
        LIGHT(0),
        DARK(1),
        SYSTEM(2);

        companion object {
            fun fromValue(value: Int) = ThemeMode.entries.firstOrNull { it.value == value } ?: SYSTEM
        }
    }

    companion object {
        private val THEME_MODE_KEY = intPreferencesKey("theme_mode")
    }

    val themeFlow: Flow<ThemeMode> = context.themeDataStore.data
        .map { preferences ->
            val themeValue = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.value
            ThemeMode.fromValue(themeValue)
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.value
        }
    }

    suspend fun toggleTheme() {
        context.themeDataStore.edit { preferences ->
            val currentTheme = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.value
            val newTheme = when (ThemeMode.fromValue(currentTheme)) {
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.LIGHT
                // Se è SYSTEM, passiamo a LIGHT o DARK in base all'attuale tema di sistema
                ThemeMode.SYSTEM -> if (android.content.res.Configuration.UI_MODE_NIGHT_YES ==
                    context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
                    ThemeMode.LIGHT
                } else {
                    ThemeMode.DARK
                }
            }
            preferences[THEME_MODE_KEY] = newTheme.value
        }
    }
}