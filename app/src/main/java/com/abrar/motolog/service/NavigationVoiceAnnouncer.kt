package com.abrar.motolog.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.abrar.motolog.shared.domain.voice.VoiceAnnouncer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Text-To-Speech (TTS) voice announcer for motorcycle turn-by-turn navigation guidance.
 *
 * Designed for glance-free acoustic routing over Bluetooth helmet communicators (e.g. Cardo, Sena).
 * Features:
 * - Managed Android [AudioManager] audio focus requesting [AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK]
 *   so helmet music or intercom volume ducks smoothly during navigation cues.
 * - Automatic focus abandonment via [UtteranceProgressListener] as soon as speech finishes.
 * - Runtime mute/unmute toggle support for glove-friendly handlebar control.
 * - Pure Android TTS with zero external SDK dependencies.
 */
@Singleton
class NavigationVoiceAnnouncer @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener, VoiceAnnouncer {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var tts: TextToSpeech? = null
    private val isInitialized = AtomicBoolean(false)

    @Volatile
    private var isMuted = false

    @Volatile
    private var isGloballyEnabled = true

    private var audioFocusRequest: AudioFocusRequest? = null
    private val focusLock = Any()

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

                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            // Audio focus already granted
                        }

                        override fun onDone(utteranceId: String?) {
                            abandonAudioFocus()
                        }

                        @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
                        override fun onError(utteranceId: String?) {
                            abandonAudioFocus()
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            abandonAudioFocus()
                        }
                    })

                    isInitialized.set(true)
                }
            }
        }
    }

    /**
     * Set global preference from Settings DataStore.
     */
    override fun setGloballyEnabled(enabled: Boolean) {
        this.isGloballyEnabled = enabled
        if (!enabled) {
            stop()
        }
    }

    /**
     * Toggle or set active session mute status.
     */
    override fun setMuted(muted: Boolean) {
        this.isMuted = muted
        if (muted) {
            stop()
        }
    }

    override fun isMuted(): Boolean = isMuted

    /**
     * Speak a navigation announcement.
     *
     * @param text The voice prompt text to synthesize.
     * @param isUrgent If true, flushes any queued speech and speaks immediately.
     */
    override fun speak(text: String, isUrgent: Boolean) {
        if (isMuted || !isGloballyEnabled || !isInitialized.get() || text.isBlank()) {
            return
        }

        requestAudioFocus()

        val queueMode = if (isUrgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = "motolog_nav_utterance_${System.currentTimeMillis()}"

        tts?.speak(text, queueMode, null, utteranceId)
    }

    /**
     * Immediately stop speech synthesis and abandon audio focus.
     */
    override fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        } finally {
            abandonAudioFocus()
        }
    }

    private fun requestAudioFocus() {
        synchronized(focusLock) {
            val am = audioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest == null) {
                    val playbackAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()

                    audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(false)
                        .setOnAudioFocusChangeListener { /* Ducking handled by platform */ }
                        .build()
                }
                audioFocusRequest?.let { am.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        }
    }

    private fun abandonAudioFocus() {
        synchronized(focusLock) {
            val am = audioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        }
    }

    /**
     * Release TTS and audio resources when destroying service/app.
     */
    override fun shutdown() {
        stop()
        try {
            tts?.shutdown()
            tts = null
            isInitialized.set(false)
        } catch (_: Exception) {
        }
    }
}
