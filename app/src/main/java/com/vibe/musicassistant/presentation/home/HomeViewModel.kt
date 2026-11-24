package com.vibe.musicassistant.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.musicassistant.VibeApplication
import com.vibe.musicassistant.domain.model.HomeState
import com.vibe.musicassistant.domain.model.OrbVisibility
import com.vibe.musicassistant.domain.model.VoiceCommand
import com.vibe.musicassistant.domain.model.VoiceLanguage
import com.vibe.musicassistant.domain.model.VoiceStatus
import com.vibe.musicassistant.service.VibeRuntime
import com.vibe.musicassistant.service.overlay.VibeOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Shared by every screen: settings, permission state, current player and actions. */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as VibeApplication
    private val permissions = MutableStateFlow(app.permissions.snapshot())

    /** Null until settings have loaded, so the splash screen can wait for it. */
    val state: StateFlow<HomeState?> =
        combine(
            app.preferences.settings,
            permissions,
            app.mediaRepository.current,
            VibeRuntime.voiceStatus,
            VibeRuntime.serviceRunning,
        ) { settings, perms, media, voice, running ->
            HomeState(settings, perms, media, voice, running)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Called whenever the app comes to the foreground. */
    fun onResume() {
        permissions.value = app.permissions.snapshot()
        app.mediaRepository.refresh()
        // Bring the orb back if VIBE is on but its service isn't running (e.g. after a reboot).
        viewModelScope.launch {
            val settings = app.preferences.settings.first()
            if (settings.assistantEnabled && app.permissions.hasRequired() && !VibeRuntime.serviceRunning.value) {
                VibeOverlayService.start(app)
            }
        }
    }

    /** Returns false (and does nothing) when a required permission is still missing. */
    fun activate(): Boolean {
        permissions.value = app.permissions.snapshot()
        if (!app.permissions.hasRequired()) return false
        viewModelScope.launch {
            app.preferences.setAssistantEnabled(true)
            VibeOverlayService.start(app)
        }
        return true
    }

    fun deactivate() {
        viewModelScope.launch { app.preferences.setAssistantEnabled(false) }
        VibeOverlayService.stop(app)
    }

    fun completeOnboarding(activateNow: Boolean) {
        viewModelScope.launch { app.preferences.setOnboardingDone() }
        if (activateNow) activate()
    }

    fun setOrbVisibility(value: OrbVisibility) {
        viewModelScope.launch { app.preferences.setOrbVisibility(value) }
    }

    fun setVoiceLanguage(value: VoiceLanguage) {
        viewModelScope.launch { app.preferences.setVoiceLanguage(value) }
    }

    fun resetOrbPosition() {
        viewModelScope.launch { app.preferences.setOrbPosition(-1, -1) }
        VibeOverlayService.resetPosition(app)
    }

    fun mediaCommand(command: VoiceCommand) {
        viewModelScope.launch { report(app.commandExecutor.execute(command)) }
    }

    fun togglePlayPause() {
        viewModelScope.launch { report(app.commandExecutor.togglePlayPause()) }
    }

    private fun report(result: VoiceStatus) {
        if (result !is VoiceStatus.Success) VibeRuntime.voiceStatus.value = result
    }

    fun clearFeedback() {
        VibeRuntime.voiceStatus.value = VoiceStatus.Idle
    }
}
