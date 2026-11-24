package com.vibe.musicassistant.domain.model

/** Media intents VIBE understands, independent of how they were spoken. */
enum class VoiceCommand(val label: String) {
    NEXT_TRACK("Next track"),
    PREVIOUS_TRACK("Previous track"),
    PLAY("Play"),
    PAUSE("Pause"),
    VOLUME_UP("Volume up"),
    VOLUME_DOWN("Volume down"),
    SEEK_FORWARD("Skip ahead 15s"),
    SEEK_BACKWARD("Rewind 15s"),
    PLAY_SEARCH("Play a song"),
}
