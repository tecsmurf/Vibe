package com.vibe.musicassistant.presentation.home

import android.app.Application
import android.os.Build
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.musicassistant.VibeApplication
import com.vibe.musicassistant.domain.model.HomeState
import com.vibe.musicassistant.domain.model.OrbVisibility
import com.vibe.musicassistant.domain.model.VoiceCommand
import com.vibe.musicassistant.domain.model.VoiceLanguage
import com.vibe.musicassistant.domain.model.VoiceStatus
import com.vibe.musicassistant.domain.model.VibePermission
import com.vibe.musicassistant.service.VibeDiagnostics
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
            combine(app.preferences.settings, permissions, app.mediaRepository.current, ::Triple),
            VibeRuntime.voiceStatus,
            VibeRuntime.serviceRunning,
            app.wakeModel.state,
        ) { (settings, perms, media), voice, running, wake ->
            HomeState(settings, perms, media, voice, running, wake)
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

    /** Snapshot of the phone and VIBE state for the Diagnostics screen. Holds no personal data. */
    fun diagnosticsSummary(): String {
        permissions.value = app.permissions.snapshot()
        val current = state.value
        val version = runCatching {
            @Suppress("DEPRECATION")
            app.packageManager.getPackageInfo(app.packageName, 0).versionName
        }.getOrNull() ?: "?"
        val perms = permissions.value.joinToString(", ") { "${it.permission.name}=${if (it.granted) "yes" else "NO"}" }
        return buildString {
            appendLine("VIBE $version · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Permissions: $perms")
            appendLine("Speech recognizer available: ${SpeechRecognizer.isRecognitionAvailable(app)}")
            appendLine("Assistant on: ${current?.settings?.assistantEnabled} · orb service running: ${VibeRuntime.serviceRunning.value}")
            appendLine("Hey VIBE switch: ${current?.settings?.wakeWordEnabled} · model: ${current?.wakeModel?.javaClass?.simpleName} (ready=${app.wakeModel.isReady()})")
            appendLine("Language: ${current?.settings?.voiceLanguage}")
            append("Media sessions: ${app.mediaRepository.describeSessions()}")
        }
    }

    fun diagnosticsText(): String = diagnosticsSummary() + "\n\n" + VibeDiagnostics.asText()

    fun clearDiagnostics() = VibeDiagnostics.clear()

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

    /**
     * Turns hands-free "Hey VIBE" on or off. Returns false when the microphone permission
     * is missing. The first time, this downloads the small on-device model (~40 MB).
     */
    fun setWakeWord(enabled: Boolean): Boolean {
        if (enabled && !app.permissions.isGranted(VibePermission.MICROPHONE)) return false
        viewModelScope.launch {
            app.preferences.setWakeWordEnabled(enabled)
            VibeOverlayService.refresh(app)
        }
        if (enabled) retryWakeModel()
        return true
    }

    fun retryWakeModel() {
        app.appScope.launch {
            if (app.wakeModel.download()) VibeOverlayService.refresh(app)
        }
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
