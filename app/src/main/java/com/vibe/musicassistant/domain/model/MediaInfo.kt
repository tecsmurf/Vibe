package com.vibe.musicassistant.domain.model

/** Snapshot of the media session VIBE is currently controlling. */
data class MediaInfo(
    val packageName: String,
    val appName: String,
    val title: String?,
    val artist: String?,
    val isPlaying: Boolean,
    val actions: Long,
    val positionMs: Long,
    val durationMs: Long,
) {
    /** Identifies the track, so a skip can be confirmed by a change here. */
    val trackKey: String get() = "$title|$artist|$durationMs"
}
