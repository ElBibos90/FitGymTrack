package com.fitgymtrack.app.utils

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SoundManager(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())

    enum class WorkoutSound {
        TIMER_COMPLETE,      // Fine timer isometrico
        SERIES_COMPLETE,     // Serie completata
        REST_COMPLETE,       // Fine recupero
        WORKOUT_COMPLETE,    // Allenamento completato
        COUNTDOWN_BEEP       // Beep countdown ultimi secondi
    }

    suspend fun playWorkoutSound(sound: WorkoutSound, withVibration: Boolean = true) =
        withContext(Dispatchers.IO) {
            try {
                when (sound) {
                    WorkoutSound.TIMER_COMPLETE -> playTimerComplete(withVibration)
                    WorkoutSound.SERIES_COMPLETE -> playSeriesComplete(withVibration)
                    WorkoutSound.REST_COMPLETE -> playRestComplete(withVibration)
                    WorkoutSound.WORKOUT_COMPLETE -> playWorkoutComplete(withVibration)
                    WorkoutSound.COUNTDOWN_BEEP -> playCountdownBeep(withVibration)
                }
            } catch (e: Exception) {
                Log.e("SoundManager", "❌ Errore riproduzione suono: ${e.message}")
            }
        }

    private fun playTimerComplete(withVibration: Boolean) {
        try {
            // 🔊 Metodo 1: Prova con ToneGenerator ottimizzato
            playOptimizedTone(
                tone = ToneGenerator.TONE_PROP_BEEP2,
                duration = 800,
                volume = 80,
                description = "Timer completato"
            )

            if (withVibration) vibrate(longArrayOf(0, 400))
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Timer completato fallito: ${e.message}")
            // Fallback: prova con MediaPlayer
            playFallbackSound(withVibration)
        }
    }

    private fun playCountdownBeep(withVibration: Boolean) {
        try {
            // Beep più semplice e veloce per countdown
            playOptimizedTone(
                tone = ToneGenerator.TONE_PROP_BEEP,
                duration = 150,
                volume = 70,
                description = "Countdown beep"
            )

            if (withVibration) vibrate(longArrayOf(0, 80))
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Countdown beep fallito: ${e.message}")
        }
    }

    private fun playSeriesComplete(withVibration: Boolean) {
        try {
            playOptimizedTone(
                tone = ToneGenerator.TONE_PROP_ACK,
                duration = 250,
                volume = 75,
                description = "Serie completata"
            )

            if (withVibration) vibrate(longArrayOf(0, 150))
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Serie completata fallito: ${e.message}")
        }
    }

    private fun playRestComplete(withVibration: Boolean) {
        try {
            // Due beep con timing ottimizzato
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 85)

            // Primo beep
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)

            // Usa Handler invece di Thread.sleep per migliore precisione
            handler.postDelayed({
                try {
                    // Secondo beep
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)

                    // Rilascia dopo il secondo beep
                    handler.postDelayed({
                        toneGen.release()
                    }, 450)

                } catch (e: Exception) {
                    Log.e("SoundManager", "❌ Secondo beep recupero fallito: ${e.message}")
                    toneGen.release()
                }
            }, 350)

            Log.d("SoundManager", "✅ Recupero completato - 2 beep ottimizzati")

            if (withVibration) vibrate(longArrayOf(0, 200, 100, 300))
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Recupero completato fallito: ${e.message}")
        }
    }

    private fun playWorkoutComplete(withVibration: Boolean) {
        try {
            // Per allenamento completato, usa MediaPlayer con suono di sistema
            playSystemNotificationSound()

            if (withVibration) {
                vibrate(longArrayOf(0, 300, 100, 300, 100, 500))
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Allenamento completato fallito: ${e.message}")
        }
    }

    /**
     * Metodo ottimizzato per ToneGenerator con timing preciso
     */
    private fun playOptimizedTone(tone: Int, duration: Int, volume: Int, description: String) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, volume)
            toneGen.startTone(tone, duration)

            // Usa Handler per rilasciare al momento giusto
            handler.postDelayed({
                toneGen.release()
            }, duration.toLong() + 50) // Piccolo buffer

            Log.d("SoundManager", "✅ $description - tone ottimizzato")
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ $description - tone fallito: ${e.message}")
            throw e
        }
    }

    /**
     * Fallback con MediaPlayer per dispositivi problematici
     */
    private fun playFallbackSound(withVibration: Boolean) {
        try {
            playSystemNotificationSound()
            Log.d("SoundManager", "✅ Fallback con MediaPlayer riuscito")
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Anche il fallback è fallito: ${e.message}")
        }
    }

    /**
     * Suono di sistema con MediaPlayer (più compatibile)
     */
    private fun playSystemNotificationSound() {
        try {
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                prepare()
                start()
                setOnCompletionListener { release() }
            }
            Log.d("SoundManager", "✅ MediaPlayer suono di sistema")
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ MediaPlayer fallito: ${e.message}")
        }
    }

    private fun vibrate(pattern: LongArray) {
        try {
            val vibrator = getSystemService(context, Vibrator::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Vibrazione fallita: ${e.message}")
        }
    }
}