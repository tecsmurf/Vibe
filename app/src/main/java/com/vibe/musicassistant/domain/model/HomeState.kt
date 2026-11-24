package com.vibe.musicassistant.domain.model

/** Everything the home screen shows, plus the derived assistant status. */
data class HomeState(
    val settings: VibeSettings = VibeSettings(),
    val permissions: List<PermissionState> = emptyList(),
    val media: MediaInfo? = null,
    val voice: VoiceStatus = VoiceStatus.Idle,
    val serviceRunning: Boolean = false,
    val wakeModel: WakeModelState = WakeModelState.NotDownloaded,
) {
    val assistantEnabled: Boolean get() = settings.assistantEnabled

    val missingRequired: List<VibePermission>
        get() = permissions.filter { it.permission.required && !it.granted }.map { it.permission }

    val grantedCount: Int get() = permissions.count { it.granted }

    val status: AssistantStatus
        get() = when {
            !assistantEnabled -> AssistantStatus.NOT_ACTIVATED
            missingRequired.isNotEmpty() -> AssistantStatus.PERMISSION_REQUIRED
            voice is VoiceStatus.Listening -> AssistantStatus.LISTENING
            voice is VoiceStatus.Processing -> AssistantStatus.PROCESSING
            voice is VoiceStatus.Success -> AssistantStatus.COMMAND_EXECUTED
            media == null -> AssistantStatus.WAITING_FOR_MUSIC
            media.isPlaying -> AssistantStatus.READY_TO_LISTEN
            else -> AssistantStatus.MUSIC_DETECTED
        }
}
