package com.vibe.musicassistant.domain.model

/**
 * The accesses VIBE relies on, with the plain-language reason shown to the user.
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
        reason = "Lets VIBE find the player that's playing and control it. VIBE doesn't read your messages.",
        required = true,
    ),
    MICROPHONE(
        title = "Microphone",
        reason = "Hears your command, only after you tap the orb.",
        required = true,
    ),
    USAGE_ACCESS(
        title = "Usage access",
        reason = "Optional. Lets the orb appear only when you open a music app.",
        required = false,
    ),
    NOTIFICATIONS(
        title = "Notifications",
        reason = "Optional. Shows VIBE's status notification with a Stop button.",
        required = false,
    ),
}

data class PermissionState(
    val permission: VibePermission,
    val granted: Boolean,
)
