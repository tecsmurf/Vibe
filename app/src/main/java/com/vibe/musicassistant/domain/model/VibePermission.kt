package com.vibe.musicassistant.domain.model

/**
 * The special accesses VIBE relies on, with the plain-language reason shown to the user.
 * [required] marks the ones the assistant cannot work without.
 */
enum class VibePermission(
    val title: String,
    val reason: String,
    val required: Boolean,
) {
    OVERLAY(
        title = "Display over other apps",
        reason = "Keeps the VIBE orb floating above your music app.",
        required = true,
    ),
    NOTIFICATION_ACCESS(
        title = "Notification access",
        reason = "Lets VIBE find the player that's playing and control it.",
        required = true,
    ),
    MICROPHONE(
        title = "Microphone",
        reason = "Hears your command, only after you tap the orb.",
        required = true,
    ),
    USAGE_ACCESS(
        title = "Usage access",
        reason = "Optional. Shows the orb when you open a music app.",
        required = false,
    ),
}

data class PermissionState(
    val permission: VibePermission,
    val granted: Boolean,
)
