package com.fitgymtrack.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fitgymtrack.app.models.User
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

class SessionManager(private val context: Context) {
    companion object {
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val USER_DATA = stringPreferencesKey("user_data")
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
        }
    }

    suspend fun saveUserData(user: User) {
        val userJson = Gson().toJson(user)
        context.dataStore.edit { preferences ->
            preferences[USER_DATA] = userJson
        }
    }

    fun getAuthToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTH_TOKEN]
        }
    }

    fun getUserData(): Flow<User?> {
        return context.dataStore.data.map { preferences ->
            val userJson = preferences[USER_DATA]
            if (userJson != null) {
                try {
                    Gson().fromJson(userJson, User::class.java)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN)
            preferences.remove(USER_DATA)
        }
    }
}