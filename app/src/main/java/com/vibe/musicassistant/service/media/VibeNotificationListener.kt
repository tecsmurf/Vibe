package com.vibe.musicassistant.service.media

import android.service.notification.NotificationListenerService
import com.vibe.musicassistant.VibeApplication

/**
 * Exists so the user can grant notification access, which is what Android requires
 * before an app may see and control other apps' media sessions. VIBE ignores the
 * notifications themselves.
 */
class VibeNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        (application as VibeApplication).mediaRepository.refresh()
    }
}
