package com.vibe.musicassistant.domain.model

enum class OrbVisibility(val label: String) {
    ALWAYS("Always"),
    WITH_MUSIC("With music"),
}

enum class VoiceLanguage(val tag: String, val label: String) {
    ENGLISH_HINGLISH("en-IN", "English + Hinglish"),
    HINDI("hi-IN", "हिंदी"),
}

data class VibeSettings(
    val assistantEnabled: Boolean = false,
    val onboardingDone: Boolean = false,
    val orbVisibility: OrbVisibility = OrbVisibility.ALWAYS,
    val voiceLanguage: VoiceLanguage = VoiceLanguage.ENGLISH_HINGLISH,
    val wakeWordEnabled: Boolean = false,
    val orbX: Int = -1,
    val orbY: Int = -1,
)

/** On-device "Hey VIBE" model download state. */
sealed interface WakeModelState {
    data object NotDownloaded : WakeModelState
    data class Downloading(val progress: Float) : WakeModelState
    data object Ready : WakeModelState
    data class Failed(val message: String) : WakeModelState
}
