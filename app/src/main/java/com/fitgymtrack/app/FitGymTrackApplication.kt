package com.fitgymtrack.app

import android.app.Application
import com.fitgymtrack.app.api.ApiClient
import com.fitgymtrack.app.utils.SessionManager
import com.fitgymtrack.app.utils.ThemeManager

class FitGymTrackApplication : Application() {

    lateinit var themeManager: ThemeManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Inizializza il gestore del tema
        themeManager = ThemeManager(applicationContext)

        // Inizializza il SessionManager
        val sessionManager = SessionManager(applicationContext)

        // Inizializza ApiClient con SessionManager
        ApiClient.initialize(sessionManager)

        // Altre inizializzazioni dell'app
    }
}