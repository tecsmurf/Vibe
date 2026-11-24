package com.vibe.musicassistant.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vibe.musicassistant.domain.model.PermissionState
import com.vibe.musicassistant.domain.model.VibePermission

/**
 * Reads the current grant state of every access VIBE uses. Read-only: nothing here
 * requests a permission. Call [snapshot] again whenever the app resumes, because the
 * user changes these in system settings.
 */
class VibePermissionManager(context: Context) {
    private val appContext = context.applicationContext

    fun snapshot(): List<PermissionState> =
        VibePermission.entries.map { PermissionState(it, isGranted(it)) }

    fun isGranted(permission: VibePermission): Boolean = when (permission) {
        VibePermission.OVERLAY -> Settings.canDrawOverlays(appContext)
        VibePermission.NOTIFICATION_ACCESS ->
            NotificationManagerCompat.getEnabledListenerPackages(appContext)
                .contains(appContext.packageName)
        VibePermission.MICROPHONE ->
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        VibePermission.USAGE_ACCESS -> hasUsageAccess()
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = appContext.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                appContext.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                appContext.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
