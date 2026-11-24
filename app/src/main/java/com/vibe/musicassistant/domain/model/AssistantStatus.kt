package com.vibe.musicassistant.domain.model

/** Every state the VIBE assistant can report on the home screen and the orb. */
enum class AssistantStatus(val label: String) {
    NOT_ACTIVATED("Not activated"),
    PERMISSION_REQUIRED("Permission required"),
    WAITING_FOR_MUSIC("Waiting for music"),
    MUSIC_DETECTED("Music detected"),
    READY_TO_LISTEN("Ready to listen"),
    LISTENING("Listening"),
    PROCESSING("Processing"),
    COMMAND_EXECUTED("Command executed"),
}
