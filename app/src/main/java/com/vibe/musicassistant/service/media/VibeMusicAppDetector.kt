package com.vibe.musicassistant.service.media

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager
import com.vibe.musicassistant.utils.MusicApps
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/**
 * Decides whether the user is "in a music moment": music is playing, or a music app
 * is in the foreground. Uses Usage Access (if granted) for the foreground app and the
 * media repository for playback. Polls lightly and skips work while the screen is off.
 */
class VibeMusicAppDetector(
    context: Context,
    private val media: VibeMediaRepository,
) {
    private val appContext = context.applicationContext
    private val usageStats = appContext.getSystemService(UsageStatsManager::class.java)
    private val power = appContext.getSystemService(PowerManager::class.java)
    private var lastForeground: String? = null

    fun musicContext(hasUsageAccess: () -> Boolean): Flow<Boolean> = flow {
        while (true) {
            if (power?.isInteractive != false) emit(check(hasUsageAccess()))
            delay(POLL_MS)
        }
    }.distinctUntilChanged()

    private fun check(usageAccess: Boolean): Boolean {
        val playing = media.current.value
        if (playing?.isPlaying == true) return true
        if (!usageAccess && !media.hasAccess.value) return true // Can't tell, so don't hide VIBE.
        val foreground = if (usageAccess) foregroundPackage() else null
        return foreground != null &&
            (foreground in MusicApps.KNOWN || foreground == playing?.packageName)
    }

    private fun foregroundPackage(): String? {
        val manager = usageStats ?: return lastForeground
        val end = System.currentTimeMillis()
        val events = runCatching { manager.queryEvents(end - 30_000, end) }.getOrNull() ?: return lastForeground
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION")
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.packageName != appContext.packageName) lastForeground = event.packageName
            }
        }
        return lastForeground
    }

    private companion object {
        const val POLL_MS = 2_000L
    }
}
