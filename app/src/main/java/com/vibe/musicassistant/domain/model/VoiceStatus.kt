package com.vibe.musicassistant.domain.model

/** Live state of a tap-to-talk interaction, shared by the orb and the home screen. */
sealed interface VoiceStatus {
    data object Idle : VoiceStatus
    data class Listening(val partialText: String = "") : VoiceStatus
    data class Processing(val heard: String) : VoiceStatus
    data class Success(val message: String) : VoiceStatus
    /** The player got the command but didn't confirm it, so we don't claim success. */
    data class Unconfirmed(val message: String) : VoiceStatus
    data class Error(val message: String) : VoiceStatus
}
