package com.vibe.musicassistant.service

import com.vibe.musicassistant.domain.model.VoiceStatus
import kotlinx.coroutines.flow.MutableStateFlow

/** Process-wide live state shared between the overlay service and the app UI. */
object VibeRuntime {
    val serviceRunning = MutableStateFlow(false)
    val voiceStatus = MutableStateFlow<VoiceStatus>(VoiceStatus.Idle)
}
