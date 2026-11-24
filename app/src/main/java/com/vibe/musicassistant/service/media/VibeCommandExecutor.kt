package com.vibe.musicassistant.service.media

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import com.vibe.musicassistant.domain.model.MediaInfo
import com.vibe.musicassistant.domain.model.VoiceCommand
import com.vibe.musicassistant.domain.model.VoiceStatus
import com.vibe.musicassistant.service.VibeDiagnostics
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Executes a [VoiceCommand] on the current player and checks the player's own state
 * afterwards. Returns [VoiceStatus.Success] only when the change is observed.
 */
class VibeCommandExecutor(
    context: Context,
    private val media: VibeMediaRepository,
    private val volume: VibeVolumeController,
) {
    private val appContext = context.applicationContext

    suspend fun execute(command: VoiceCommand, query: String? = null): VoiceStatus {
        VibeDiagnostics.log("cmd", "Run ${command.name}${query?.let { " query=\"$it\"" } ?: ""}")
        val result = run(command, query)
        VibeDiagnostics.log("cmd", "Result: $result")
        return result
    }

    private suspend fun run(command: VoiceCommand, query: String?): VoiceStatus = when (command) {
        VoiceCommand.VOLUME_UP, VoiceCommand.VOLUME_DOWN -> adjustVolume(command == VoiceCommand.VOLUME_UP)
        VoiceCommand.PLAY_SEARCH ->
            if (query.isNullOrBlank()) controlPlayer(VoiceCommand.PLAY) else playSearch(query.take(MAX_QUERY))
        else -> controlPlayer(command)
    }

    /** Play/pause toggle for the quick-control panel. */
    suspend fun togglePlayPause(): VoiceStatus {
        val playing = media.current.value?.isPlaying == true
        return controlPlayer(if (playing) VoiceCommand.PAUSE else VoiceCommand.PLAY)
    }

    private fun adjustVolume(up: Boolean): VoiceStatus {
        val r = volume.adjust(up)
        return when {
            r.atLimit -> VoiceStatus.Success(if (up) "Volume is already at max" else "Volume is already at minimum")
            r.changed -> VoiceStatus.Success("Volume ${r.percent}%")
            else -> VoiceStatus.Error("Volume didn't change. Do Not Disturb or the device may be blocking it.")
        }
    }

    private suspend fun controlPlayer(command: VoiceCommand): VoiceStatus {
        media.refresh()
        val controller = media.activeController()
        val before = media.current.value
        if (controller == null || before == null) {
            return if (!media.hasAccess.value) {
                VoiceStatus.Error("Turn on notification access for VIBE so it can control music.")
            } else {
                VoiceStatus.Error("No music player found. Start a song in your music app first.")
            }
        }
        val app = before.appName
        VibeDiagnostics.log(
            "player",
            "${controller.packageName} state=${VibeDiagnostics.describeState(controller.playbackState?.state)} " +
                "actions=${VibeDiagnostics.describeActions(controller.playbackState?.actions ?: 0L)} track=\"${before.title}\"",
        )

        return when (command) {
            VoiceCommand.NEXT_TRACK -> {
                send(controller, PlaybackState.ACTION_SKIP_TO_NEXT, KeyEvent.KEYCODE_MEDIA_NEXT) { it.skipToNext() }
                val changed = awaitChange { it.trackKey != before.trackKey }
                if (changed != null) VoiceStatus.Success(listOfNotNull("Next track", changed.title).joinToString(" · "))
                else VoiceStatus.Unconfirmed("Sent \"Next track\" to $app, but it didn't confirm")
            }

            VoiceCommand.PREVIOUS_TRACK -> {
                // Most players (Spotify, YouTube Music) restart the current song on the first
                // "previous" if it has played for a few seconds. Press again to really go back.
                val previous: (MediaController.TransportControls) -> Unit = { it.skipToPrevious() }
                send(controller, PlaybackState.ACTION_SKIP_TO_PREVIOUS, KeyEvent.KEYCODE_MEDIA_PREVIOUS, previous)
                var changed = awaitChange(FIRST_PREVIOUS_WAIT_MS) { it.trackKey != before.trackKey }
                VibeDiagnostics.log("player", "Previous #1 -> ${if (changed != null) "track changed" else "same track (restart?)"}")
                if (changed == null) {
                    send(controller, PlaybackState.ACTION_SKIP_TO_PREVIOUS, KeyEvent.KEYCODE_MEDIA_PREVIOUS, previous)
                    changed = awaitChange { it.trackKey != before.trackKey }
                }
                if (changed != null) VoiceStatus.Success(listOfNotNull("Previous track", changed.title).joinToString(" · "))
                else VoiceStatus.Unconfirmed("$app restarted the song but didn't go back. It may be the first song in the queue.")
            }

            VoiceCommand.PLAY -> {
                if (before.isPlaying) return VoiceStatus.Success("Already playing on $app")
                send(controller, PlaybackState.ACTION_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY) { it.play() }
                if (awaitChange { it.isPlaying } != null) VoiceStatus.Success("Playing on $app")
                else VoiceStatus.Unconfirmed("Asked $app to play, but it didn't start")
            }

            VoiceCommand.PAUSE -> {
                if (!before.isPlaying) return VoiceStatus.Success("Already paused")
                send(controller, PlaybackState.ACTION_PAUSE, KeyEvent.KEYCODE_MEDIA_PAUSE) { it.pause() }
                if (awaitChange { !it.isPlaying } != null) VoiceStatus.Success("Paused $app")
                else VoiceStatus.Unconfirmed("Asked $app to pause, but it didn't confirm")
            }

            VoiceCommand.SEEK_FORWARD, VoiceCommand.SEEK_BACKWARD -> seek(controller, before, command)

            VoiceCommand.VOLUME_UP, VoiceCommand.VOLUME_DOWN, VoiceCommand.PLAY_SEARCH -> error("handled above")
        }
    }

    private suspend fun seek(controller: MediaController, before: MediaInfo, command: VoiceCommand): VoiceStatus {
        val state = controller.playbackState
        if (state == null || (state.actions and PlaybackState.ACTION_SEEK_TO) == 0L) {
            return VoiceStatus.Error("${before.appName} doesn't allow seeking from other apps")
        }
        val now = VibeMediaRepository.currentPosition(state)
        val delta = if (command == VoiceCommand.SEEK_FORWARD) SEEK_MS else -SEEK_MS
        var target = (now + delta).coerceAtLeast(0L)
        if (before.durationMs > 0) target = target.coerceAtMost(before.durationMs - 1_000)
        controller.transportControls.seekTo(target)
        val moved = awaitChange { kotlin.math.abs(it.positionMs - target) < 4_000 }
        return if (moved != null) VoiceStatus.Success(command.label)
        else VoiceStatus.Unconfirmed("Asked ${before.appName} to seek, but it didn't confirm")
    }

    /**
     * Plays a specific song. Uses the session's play-from-search when the player offers it;
     * otherwise sends the standard "play from search" request to that player (or to an
     * installed Spotify / YouTube Music). The query only ever goes to a music player.
     */
    private suspend fun playSearch(query: String): VoiceStatus {
        media.refresh()
        val controller = media.activeController()
        val before = media.current.value
        val actions = controller?.playbackState?.actions ?: 0L

        VibeDiagnostics.log(
            "search",
            "player=${controller?.packageName ?: "none"} actions=${VibeDiagnostics.describeActions(actions)}",
        )
        if (controller != null && (actions and PlaybackState.ACTION_PLAY_FROM_SEARCH) != 0L) {
            VibeDiagnostics.log("search", "Using session playFromSearch")
            controller.transportControls.playFromSearch(query, Bundle())
            val started = awaitChange(SEARCH_WAIT_MS) { it.isPlaying && it.trackKey != before?.trackKey }
            return if (started != null) VoiceStatus.Success("Playing ${started.title ?: query}")
            else VoiceStatus.Unconfirmed("Asked ${before?.appName ?: "the player"} to play \"$query\"")
        }

        val target = listOfNotNull(controller?.packageName) + PREFERRED_PLAYERS
        val intent = target.firstNotNullOfOrNull { pkg ->
            Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
                .setPackage(pkg)
                .putExtra(SearchManager.QUERY, query)
                .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .takeIf { it.resolveActivity(appContext.packageManager) != null }
        } ?: return VoiceStatus.Error("Your music app doesn't support playing a song by name. Try Spotify or YouTube Music.")

        val appName = media.appLabel(intent.`package` ?: "")
        VibeDiagnostics.log("search", "Using play-from-search intent -> ${intent.`package`}")
        try {
            appContext.startActivity(intent)
        } catch (e: Exception) {
            return VoiceStatus.Error("Couldn't open $appName to play \"$query\"")
        }
        val started = awaitChange(SEARCH_WAIT_MS) { it.isPlaying && it.trackKey != before?.trackKey }
        return if (started != null) VoiceStatus.Success("Playing ${started.title ?: query}")
        else VoiceStatus.Unconfirmed("Opened $appName for \"$query\"")
    }

    /**
     * Uses the session's transport control when it advertises [action]; otherwise falls back
     * to a media-button key event, which many players still honour.
     */
    private fun send(
        controller: MediaController,
        action: Long,
        keyCode: Int,
        transport: (MediaController.TransportControls) -> Unit,
    ) {
        val actions = controller.playbackState?.actions ?: 0L
        if ((actions and action) != 0L) {
            VibeDiagnostics.log("player", "Sent via transport control")
            transport(controller.transportControls)
        } else {
            VibeDiagnostics.log("player", "Action not advertised; sent media key $keyCode")
            controller.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            controller.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
    }

    private suspend fun awaitChange(
        timeoutMs: Long = VERIFY_TIMEOUT_MS,
        predicate: (MediaInfo) -> Boolean,
    ): MediaInfo? =
        withTimeoutOrNull(timeoutMs) {
            media.current.filterNotNull().first(predicate)
        }

    private companion object {
        const val VERIFY_TIMEOUT_MS = 3_000L
        const val SEEK_MS = 15_000L
        const val FIRST_PREVIOUS_WAIT_MS = 1_500L
        const val SEARCH_WAIT_MS = 8_000L
        const val MAX_QUERY = 120
        val PREFERRED_PLAYERS = listOf(
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.jio.media.jiobeats",
            "com.bsbportal.music",
            "com.gaana",
            "com.amazon.mp3",
        )
    }
}
