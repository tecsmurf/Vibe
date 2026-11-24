package com.vibe.musicassistant.domain.model

/** Everything the home screen shows, plus the derived assistant status. */
data class HomeState(
    val assistantEnabled: Boolean = false,
    val permissions: List<PermissionState> = emptyList(),
    val connectedPlayer: String? = null,
) {
    val missingRequired: List<VibePermission>
        get() = permissions.filter { it.permission.required && !it.granted }.map { it.permission }

    val grantedCount: Int get() = permissions.count { it.granted }

    val status: AssistantStatus
        get() = when {
            !assistantEnabled -> AssistantStatus.NOT_ACTIVATED
            missingRequired.isNotEmpty() -> AssistantStatus.PERMISSION_REQUIRED
            connectedPlayer == null -> AssistantStatus.WAITING_FOR_MUSIC
            else -> AssistantStatus.MUSIC_DETECTED
        }
}
