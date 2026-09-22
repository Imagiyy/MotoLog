package com.abrar.motolog.service

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice Announcer for motorcycle riders using Bluetooth helmet communicators (e.g. Sena, Cardo).
 * Leverages native Android Text-to-Speech (TTS) with zero external dependencies.
 *
 * Provides glance-free acoustic telemetry for speed warnings, auto-pause/resume events,
 * and ride status updates.
 */
@Singleton
class HelmetVoiceAnnouncer @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val isInitialized = AtomicBoolean(false)
    private var isEnabled = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    engine.setAudioAttributes(audioAttributes)
                    isInitialized.set(true)
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        this.isEnabled = enabled
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_ADD) {
        if (!isEnabled || !isInitialized.get()) return
        tts?.speak(text, queueMode, null, "motolog_utterance_${System.currentTimeMillis()}")
    }

    fun announceRideStarted() {
        speak("Ride tracking started. Ride safe.")
    }

    fun announcePaused() {
        speak("Ride paused.")
    }

    fun announceResumed() {
        speak("Resuming ride.")
    }

    fun announceSpeedWarning() {
        speak("Speed limit exceeded.", TextToSpeech.QUEUE_FLUSH)
    }

    fun announceRideCompleted(distanceKm: Double, avgSpeedKmh: Double, isMetric: Boolean) {
        val distUnit = if (isMetric) "kilometers" else "miles"
        val distVal = if (isMetric) distanceKm else distanceKm * 0.621371
        val spdUnit = if (isMetric) "kilometers per hour" else "miles per hour"
        val spdVal = if (isMetric) avgSpeedKmh else avgSpeedKmh * 0.621371

        val message = String.format(
            Locale.US,
            "Ride completed. Distance %.1f %s. Average speed %.0f %s.",
            distVal,
            distUnit,
            spdVal,
            spdUnit
        )
        speak(message)
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized.set(false)
        } catch (_: Exception) {
        }
    }
}
