package com.vibe.musicassistant.service.media

import android.content.Context
import android.media.AudioManager
import android.os.Build

/**
 * Changes only the media (music) stream, never ring, alarm or notification volume,
 * and reports the level actually reached.
 */
class VibeVolumeController(context: Context) {
    private val audio = context.applicationContext.getSystemService(AudioManager::class.java)

    data class Result(val changed: Boolean, val percent: Int, val atLimit: Boolean)

    fun adjust(up: Boolean, steps: Int = 2): Result {
        val stream = AudioManager.STREAM_MUSIC
        val max = audio.getStreamMaxVolume(stream).coerceAtLeast(1)
        val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(stream) else 0
        val before = audio.getStreamVolume(stream)
        if ((up && before >= max) || (!up && before <= min)) {
            return Result(changed = false, percent = before * 100 / max, atLimit = true)
        }
        val direction = if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        repeat(steps) { i ->
            val flags = if (i == steps - 1) AudioManager.FLAG_SHOW_UI else 0
            audio.adjustStreamVolume(stream, direction, flags)
        }
        val after = audio.getStreamVolume(stream)
        return Result(changed = after != before, percent = after * 100 / max, atLimit = false)
    }
}
