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
    val orbX: Int = -1,
    val orbY: Int = -1,
)
