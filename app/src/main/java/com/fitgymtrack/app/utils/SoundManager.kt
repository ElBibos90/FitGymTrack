package com.fitgymtrack.app.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SoundManager(private val context: Context) {

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
            // 🔊 UN SINGOLO BEEP LUNGO E FORTE per timer completato
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)

            // Un solo beep lungo e potente
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 1200) // 1200ms di durata
            if (withVibration) vibrate(longArrayOf(0, 400)) // Una vibrazione lunga
            // ✅ ASPETTA che il suono finisca prima di rilasciare
            Thread.sleep(1500) // Aspetta un po' di più della durata del suono (1200ms + buffer)

            toneGen.release()
            Log.d("SoundManager", "✅ Timer completato - 1 beep forte completo")
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Timer completato fallito: ${e.message}")
        }


    }

    private fun playCountdownBeep(withVibration: Boolean) {
        try {
            // Beep singolo forte per countdown
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            toneGen.release()
            Log.d("SoundManager", "✅ Countdown beep")
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Countdown beep fallito: ${e.message}")
        }

        if (withVibration) vibrate(longArrayOf(0, 100)) // Vibrazione breve
    }

    private fun playSeriesComplete(withVibration: Boolean) {
        try {
            // Beep di conferma con STREAM_MUSIC
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 300)
            toneGen.release()
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Serie completata fallito: ${e.message}")
        }

        if (withVibration) vibrate(longArrayOf(0, 150))
    }

    private fun playRestComplete(withVibration: Boolean) {
        try {
            // 🔊 Suono distintivo per fine recupero - più forte e più lungo
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 90)

            // Due beep lunghi per indicare "recupero finito, riprendi!"
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            Thread.sleep(450)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 600) // Secondo più lungo
            Thread.sleep(650) // Aspetta che finisca

            toneGen.release()
            Log.d("SoundManager", "✅ Recupero completato - 2 beep")
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Recupero completato fallito: ${e.message}")
        }

        if (withVibration) vibrate(longArrayOf(0, 200, 100, 400)) // Due vibrazioni
    }

    private fun playWorkoutComplete(withVibration: Boolean) {
        try {
            // Sequenza di successo lunga con STREAM_MUSIC
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

            // Melodia di successo: beep crescenti
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            Thread.sleep(200)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
            Thread.sleep(200)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800) // Finale lungo

            toneGen.release()
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Allenamento completato fallito: ${e.message}")
        }

        if (withVibration) vibrate(longArrayOf(0, 300, 100, 300, 100, 600)) // Vibrazione di successo
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