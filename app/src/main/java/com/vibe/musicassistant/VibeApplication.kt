package com.vibe.musicassistant

import android.app.Application
import com.vibe.musicassistant.data.preferences.VibePreferencesRepository
import com.vibe.musicassistant.permissions.VibePermissionManager
import com.vibe.musicassistant.service.media.VibeCommandExecutor
import com.vibe.musicassistant.service.media.VibeMediaRepository
import com.vibe.musicassistant.service.media.VibeVolumeController

/** Holds the app-wide singletons (a tiny manual DI container). */
class VibeApplication : Application() {
    val preferences by lazy { VibePreferencesRepository(this) }
    val permissions by lazy { VibePermissionManager(this) }
    val mediaRepository by lazy { VibeMediaRepository(this) }
    val commandExecutor by lazy { VibeCommandExecutor(mediaRepository, VibeVolumeController(this)) }
}
