package com.example.engine

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class FeedbackController(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    fun triggerScanSuccess(hapticEnabled: Boolean, soundEnabled: Boolean, isWarning: Boolean = false) {
        if (hapticEnabled) {
            try {
                val vib = vibrator
                if (vib?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (isWarning) {
                            vib.vibrate(
                                VibrationEffect.createWaveform(longArrayOf(0, 45, 50, 75), -1)
                            )
                        } else {
                            vib.vibrate(
                                VibrationEffect.createOneShot(28L, VibrationEffect.DEFAULT_AMPLITUDE)
                            )
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(30L)
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (soundEnabled) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
                val tone = if (isWarning) {
                    ToneGenerator.TONE_PROP_NACK
                } else {
                    ToneGenerator.TONE_PROP_BEEP
                }
                toneGen.startTone(tone, 95)
            } catch (_: Exception) {
            }
        }
    }
}
