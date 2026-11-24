package com.vibe.musicassistant.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStateTest {
    private fun permissions(granted: (VibePermission) -> Boolean) =
        VibePermission.entries.map { PermissionState(it, granted(it)) }

    private val on = VibeSettings(assistantEnabled = true)

    private fun media(playing: Boolean) = MediaInfo(
        packageName = "com.spotify.music", appName = "Spotify", title = "Song", artist = "Artist",
        isPlaying = playing, actions = 0, positionMs = 0, durationMs = 1000,
    )

    @Test fun notActivatedWhenAssistantIsOff() {
        assertEquals(AssistantStatus.NOT_ACTIVATED, HomeState(permissions = permissions { true }).status)
    }

    @Test fun permissionRequiredWhenARequiredAccessIsMissing() {
        val state = HomeState(on, permissions { it != VibePermission.OVERLAY })
        assertEquals(AssistantStatus.PERMISSION_REQUIRED, state.status)
        assertEquals(listOf(VibePermission.OVERLAY), state.missingRequired)
    }

    @Test fun optionalAccessesDoNotBlockActivation() {
        val state = HomeState(on, permissions { it.required })
        assertEquals(AssistantStatus.WAITING_FOR_MUSIC, state.status)
        assertEquals(3, state.grantedCount)
    }

    @Test fun reflectsPlayerAndVoiceState() {
        val all = permissions { true }
        assertEquals(AssistantStatus.MUSIC_DETECTED, HomeState(on, all, media(false)).status)
        assertEquals(AssistantStatus.READY_TO_LISTEN, HomeState(on, all, media(true)).status)
        assertEquals(AssistantStatus.LISTENING, HomeState(on, all, media(true), VoiceStatus.Listening()).status)
        assertEquals(AssistantStatus.COMMAND_EXECUTED, HomeState(on, all, media(true), VoiceStatus.Success("ok")).status)
    }
}
