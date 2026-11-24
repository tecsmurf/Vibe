package com.vibe.musicassistant

import android.app.Application
import com.vibe.musicassistant.data.preferences.VibePreferencesRepository
import com.vibe.musicassistant.permissions.VibePermissionManager
import com.vibe.musicassistant.service.media.VibeCommandExecutor
import com.vibe.musicassistant.service.media.VibeMediaRepository
import com.vibe.musicassistant.service.media.VibeVolumeController
import com.vibe.musicassistant.service.voice.WakeModelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Holds the app-wide singletons (a tiny manual DI container). */
class VibeApplication : Application() {
    /** Work that must outlive a screen, such as the wake-model download. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val wakeModel by lazy { WakeModelManager(this) }
    val preferences by lazy { VibePreferencesRepository(this) }
    val permissions by lazy { VibePermissionManager(this) }
    val mediaRepository by lazy { VibeMediaRepository(this) }
    val commandExecutor by lazy { VibeCommandExecutor(this, mediaRepository, VibeVolumeController(this)) }
}
