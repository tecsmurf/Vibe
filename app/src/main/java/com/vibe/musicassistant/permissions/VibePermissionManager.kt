package com.vibe.musicassistant.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.vibe.musicassistant.domain.model.PermissionState
import com.vibe.musicassistant.domain.model.VibePermission
import com.vibe.musicassistant.service.media.VibeNotificationListener

/**
 * Reads the grant state of every access VIBE uses and opens the right system screen
 * to change it. Runtime permissions (microphone, notifications) are requested by the UI.
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
        VibePermission.MICROPHONE -> hasRuntime(Manifest.permission.RECORD_AUDIO)
        VibePermission.USAGE_ACCESS -> hasUsageAccess()
        VibePermission.NOTIFICATIONS ->
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                hasRuntime(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun hasRequired(): Boolean = VibePermission.entries.filter { it.required }.all(::isGranted)

    private fun hasRuntime(permission: String) =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasUsageAccess(): Boolean {
        val appOps = appContext.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** The system settings screen where the user grants [permission]. */
    fun settingsIntent(permission: VibePermission): Intent = when (permission) {
        VibePermission.OVERLAY ->
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri())
        VibePermission.NOTIFICATION_ACCESS ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(appContext, VibeNotificationListener::class.java).flattenToString(),
                )
            } else {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            }
        VibePermission.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        VibePermission.MICROPHONE, VibePermission.NOTIFICATIONS -> appDetailsIntent()
    }

    fun appDetailsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri())

    fun batteryIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    /** Opens [intent], falling back to a more general screen if the device lacks it. */
    fun open(context: Context, intent: Intent, fallback: Intent? = null) {
        val flags = if (context is android.app.Activity) 0 else Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            context.startActivity(intent.addFlags(flags))
        } catch (e: ActivityNotFoundException) {
            val second = fallback ?: appDetailsIntent()
            try {
                context.startActivity(second.addFlags(flags))
            } catch (_: ActivityNotFoundException) {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(flags))
            }
        }
    }

    fun openFor(context: Context, permission: VibePermission) {
        val fallback = when (permission) {
            VibePermission.NOTIFICATION_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            VibePermission.OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            else -> null
        }
        open(context, settingsIntent(permission), fallback)
    }

    private fun packageUri(): Uri = Uri.parse("package:${appContext.packageName}")
}
