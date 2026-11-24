package com.vibe.musicassistant.service

import android.media.session.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-memory event log shown on the Diagnostics screen, so problems on a real phone can be
 * reported. Never written to disk or logcat, never sent anywhere, cleared when the app's
 * process ends or the user taps Clear.
 */
object VibeDiagnostics {
    data class Entry(val time: Long, val tag: String, val message: String)

    private const val MAX_ENTRIES = 150
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    fun log(tag: String, message: String) {
        _entries.update { (it + Entry(System.currentTimeMillis(), tag, message.take(400))).takeLast(MAX_ENTRIES) }
    }

    fun clear() {
        _entries.value = emptyList()
    }

    fun format(entry: Entry): String =
        "${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(entry.time))} [${entry.tag}] ${entry.message}"

    fun asText(): String = _entries.value.joinToString("\n", transform = ::format)

    private val ACTION_NAMES = listOf(
        PlaybackState.ACTION_PLAY to "PLAY",
        PlaybackState.ACTION_PAUSE to "PAUSE",
        PlaybackState.ACTION_PLAY_PAUSE to "PLAY_PAUSE",
        PlaybackState.ACTION_SKIP_TO_NEXT to "NEXT",
        PlaybackState.ACTION_SKIP_TO_PREVIOUS to "PREVIOUS",
        PlaybackState.ACTION_SEEK_TO to "SEEK",
        PlaybackState.ACTION_PLAY_FROM_SEARCH to "PLAY_FROM_SEARCH",
        PlaybackState.ACTION_PLAY_FROM_MEDIA_ID to "PLAY_FROM_MEDIA_ID",
        PlaybackState.ACTION_STOP to "STOP",
    )

    fun describeActions(actions: Long): String =
        ACTION_NAMES.filter { (flag, _) -> (actions and flag) != 0L }.joinToString(",") { it.second }
            .ifEmpty { "none" }

    fun describeState(state: Int?): String = when (state) {
        PlaybackState.STATE_PLAYING -> "playing"
        PlaybackState.STATE_PAUSED -> "paused"
        PlaybackState.STATE_STOPPED -> "stopped"
        PlaybackState.STATE_BUFFERING -> "buffering"
        PlaybackState.STATE_CONNECTING -> "connecting"
        PlaybackState.STATE_SKIPPING_TO_NEXT -> "skipping-next"
        PlaybackState.STATE_SKIPPING_TO_PREVIOUS -> "skipping-previous"
        PlaybackState.STATE_ERROR -> "error"
        PlaybackState.STATE_NONE, null -> "none"
        else -> "state-$state"
    }
}
