package com.abrar.motolog.shared.domain.voice

/**
 * Interface abstracting acoustic voice announcement delivery for turn-by-turn navigation.
 * Decouples speech synthesis and audio focus management from ViewModels and domain logic.
 */
interface VoiceAnnouncer {
    /**
     * Speak a navigation instruction or status update.
     *
     * @param text The acoustic message to synthesize.
     * @param isUrgent If true, flushes queued speech and speaks immediately.
     */
    fun speak(text: String, isUrgent: Boolean = false)

    /**
     * Toggle or set the active session mute status.
     */
    fun setMuted(muted: Boolean)

    /**
     * Whether voice guidance is currently muted.
     */
    fun isMuted(): Boolean

    /**
     * Update global voice announcement enabled preference.
     */
    fun setGloballyEnabled(enabled: Boolean)

    /**
     * Immediately stop active speech synthesis and release audio focus.
     */
    fun stop()

    /**
     * Release underlying audio and speech synthesis resources.
     */
    fun shutdown()
}
