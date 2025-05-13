package com.fitgymtrack.app

import android.app.Application
import com.fitgymtrack.app.utils.ThemeManager

class FitGymTrackApplication : Application() {

    lateinit var themeManager: ThemeManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Inizializza il gestore del tema
        themeManager = ThemeManager(applicationContext)

        // Altre inizializzazioni dell'app
    }
}