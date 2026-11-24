package com.vibe.musicassistant.presentation

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibe.musicassistant.VibeApplication
import com.vibe.musicassistant.domain.model.VibePermission
import com.vibe.musicassistant.domain.model.VoiceCommand
import com.vibe.musicassistant.presentation.components.VibeBackground
import com.vibe.musicassistant.presentation.home.HomeScreen
import com.vibe.musicassistant.presentation.home.HomeViewModel
import com.vibe.musicassistant.presentation.onboarding.OnboardingScreen
import com.vibe.musicassistant.presentation.onboarding.PermissionsScreen
import com.vibe.musicassistant.presentation.settings.SettingsScreen

private enum class Screen { HOME, SETTINGS, PERMISSIONS }

/** Root composable: routing, permission requests and wiring to the view model. */
@Composable
fun VibeApp(viewModel: HomeViewModel) {
    val loaded by viewModel.state.collectAsStateWithLifecycle()
    val state = loaded ?: run {
        VibeBackground {}
        return
    }
    val context = LocalContext.current
    val app = context.applicationContext as VibeApplication
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var micAsked by rememberSaveable { mutableStateOf(false) }
    var notificationsAsked by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    val runtimeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.onResume() }

    fun requestRuntime(permission: String, askedBefore: Boolean) {
        val activity = context as? Activity
        val blocked = askedBefore && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        if (blocked) app.permissions.open(context, app.permissions.appDetailsIntent())
        else runtimeLauncher.launch(permission)
    }

    val onGrant: (VibePermission) -> Unit = { permission ->
        when (permission) {
            VibePermission.MICROPHONE -> {
                requestRuntime(Manifest.permission.RECORD_AUDIO, micAsked)
                micAsked = true
            }
            VibePermission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestRuntime(Manifest.permission.POST_NOTIFICATIONS, notificationsAsked)
                notificationsAsked = true
            }
            else -> app.permissions.openFor(context, permission)
        }
    }
    val onOpenAppInfo: () -> Unit = { app.permissions.open(context, app.permissions.appDetailsIntent()) }
    val activate: () -> Unit = { if (!viewModel.activate()) screen = Screen.PERMISSIONS }

    if (!state.settings.onboardingDone) {
        OnboardingScreen(
            state = state,
            onGrant = onGrant,
            onOpenAppInfo = onOpenAppInfo,
            onFinish = { ready -> viewModel.completeOnboarding(activateNow = ready) },
        )
        return
    }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            val forward = targetState != Screen.HOME
            (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
        },
        label = "screen",
    ) { current ->
        when (current) {
            Screen.HOME -> HomeScreen(
                state = state,
                onActivate = activate,
                onDeactivate = viewModel::deactivate,
                onOpenSettings = { screen = Screen.SETTINGS },
                onOpenPermissions = { screen = Screen.PERMISSIONS },
                onPrevious = { viewModel.mediaCommand(VoiceCommand.PREVIOUS_TRACK) },
                onPlayPause = viewModel::togglePlayPause,
                onNext = { viewModel.mediaCommand(VoiceCommand.NEXT_TRACK) },
                onWakeWord = { on -> if (!viewModel.setWakeWord(on)) screen = Screen.PERMISSIONS },
                onRetryWakeModel = viewModel::retryWakeModel,
            )
            Screen.SETTINGS -> SettingsScreen(
                state = state,
                onToggleAssistant = { on -> if (on) activate() else viewModel.deactivate() },
                onOrbVisibility = viewModel::setOrbVisibility,
                onVoiceLanguage = viewModel::setVoiceLanguage,
                onResetOrb = viewModel::resetOrbPosition,
                onOpenPermissions = { screen = Screen.PERMISSIONS },
                onOpenBattery = { app.permissions.open(context, app.permissions.batteryIntent()) },
                onBack = { screen = Screen.HOME },
            )
            Screen.PERMISSIONS -> PermissionsScreen(
                state = state,
                onGrant = onGrant,
                onOpenAppInfo = onOpenAppInfo,
                onActivate = {
                    if (viewModel.activate()) screen = Screen.HOME
                },
                onBack = { screen = Screen.HOME },
            )
        }
    }
}
