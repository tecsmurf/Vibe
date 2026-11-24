package com.vibe.musicassistant.service.media

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.vibe.musicassistant.domain.model.MediaInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Discovers active media sessions (Spotify, YouTube Music, any compliant player) via
 * [MediaSessionManager] and tracks the most relevant one. Needs notification access;
 * without it [hasAccess] stays false and nothing is controlled.
 *
 * All calls are expected on the main thread.
 */
class VibeMediaRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sessionManager = appContext.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(appContext, VibeNotificationListener::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _current = MutableStateFlow<MediaInfo?>(null)
    val current: StateFlow<MediaInfo?> = _current.asStateFlow()

    private val _hasAccess = MutableStateFlow(false)
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private var controller: MediaController? = null
    private var observing = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        onSessionsChanged(list.orEmpty())
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = refresh()
    }

    /** Start listening for sessions appearing and disappearing. Safe to call repeatedly. */
    fun startObserving() {
        refresh()
    }

    fun stopObserving() {
        if (observing) {
            runCatching { sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener) }
            observing = false
        }
        select(null)
    }

    /** Re-reads the active sessions now. */
    fun refresh() {
        val manager = sessionManager ?: return
        val sessions = try {
            manager.getActiveSessions(listenerComponent)
        } catch (e: SecurityException) {
            _hasAccess.value = false
            select(null)
            return
        }
        _hasAccess.value = true
        if (!observing) {
            observing = runCatching {
                manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, mainHandler)
            }.isSuccess
        }
        onSessionsChanged(sessions)
    }

    fun activeController(): MediaController? = controller

    private fun onSessionsChanged(sessions: List<MediaController>) {
        // Prefer whatever is playing; otherwise stay with the player we had; otherwise the
        // top of the list, which Android orders by most recent media activity.
        val best = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull { it.packageName == controller?.packageName }
            ?: sessions.firstOrNull()
        select(best)
    }

    private fun select(next: MediaController?) {
        val previous = controller
        if (previous != null && next != null && previous.sessionToken == next.sessionToken) {
            publish()
            return
        }
        previous?.let { runCatching { it.unregisterCallback(callback) } }
        controller = next
        next?.registerCallback(callback, mainHandler)
        publish()
    }

    private fun publish() {
        _current.value = controller?.let(::toInfo)
    }

    private fun toInfo(c: MediaController): MediaInfo {
        val state = c.playbackState
        val metadata = c.metadata
        return MediaInfo(
            packageName = c.packageName,
            appName = appLabel(c.packageName),
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),
            isPlaying = state?.state == PlaybackState.STATE_PLAYING ||
                state?.state == PlaybackState.STATE_BUFFERING,
            actions = state?.actions ?: 0L,
            positionMs = state?.let(::currentPosition) ?: 0L,
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
        )
    }

    private val labels = mutableMapOf<String, String>()

    fun appLabel(packageName: String): String = labels.getOrPut(packageName) {
        try {
            val pm = appContext.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    companion object {
        /** Extrapolates the playback position from the last state update. */
        fun currentPosition(state: PlaybackState): Long {
            if (state.state != PlaybackState.STATE_PLAYING || state.lastPositionUpdateTime <= 0) {
                return state.position
            }
            val elapsed = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
            return state.position + (elapsed * state.playbackSpeed).toLong()
        }
    }
}
