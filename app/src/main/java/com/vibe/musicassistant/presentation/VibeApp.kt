package com.vibe.musicassistant.presentation

import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibe.musicassistant.presentation.home.HomeScreen
import com.vibe.musicassistant.presentation.home.HomeViewModel
import com.vibe.musicassistant.presentation.settings.SettingsScreen

private enum class Screen { HOME, SETTINGS }

/** Root composable: owns the screen state and the shared view model. */
@Composable
fun VibeApp(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    // Users grant permissions in system settings, so re-check whenever we come back.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            val forward = targetState == Screen.SETTINGS
            (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
        },
        label = "screen",
    ) { current ->
        when (current) {
            Screen.HOME -> HomeScreen(
                state = state,
                onToggleAssistant = viewModel::setAssistantEnabled,
                onOpenSettings = { screen = Screen.SETTINGS },
            )
            Screen.SETTINGS -> SettingsScreen(
                state = state,
                onToggleAssistant = viewModel::setAssistantEnabled,
                onBack = { screen = Screen.HOME },
            )
        }
    }
}
