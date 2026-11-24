package com.vibe.musicassistant.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStateTest {
    private fun permissions(granted: (VibePermission) -> Boolean) =
        VibePermission.entries.map { PermissionState(it, granted(it)) }

    @Test
    fun notActivatedWhenAssistantIsOff() {
        val state = HomeState(assistantEnabled = false, permissions = permissions { true })
        assertEquals(AssistantStatus.NOT_ACTIVATED, state.status)
    }

    @Test
    fun permissionRequiredWhenARequiredAccessIsMissing() {
        val state = HomeState(
            assistantEnabled = true,
            permissions = permissions { it != VibePermission.OVERLAY },
        )
        assertEquals(AssistantStatus.PERMISSION_REQUIRED, state.status)
        assertEquals(listOf(VibePermission.OVERLAY), state.missingRequired)
    }

    @Test
    fun optionalUsageAccessDoesNotBlockActivation() {
        val state = HomeState(
            assistantEnabled = true,
            permissions = permissions { it != VibePermission.USAGE_ACCESS },
        )
        assertEquals(AssistantStatus.WAITING_FOR_MUSIC, state.status)
        assertEquals(3, state.grantedCount)
    }

    @Test
    fun musicDetectedWhenAPlayerIsConnected() {
        val state = HomeState(
            assistantEnabled = true,
            permissions = permissions { true },
            connectedPlayer = "Spotify",
        )
        assertEquals(AssistantStatus.MUSIC_DETECTED, state.status)
    }
}
