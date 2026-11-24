package com.vibe.musicassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vibe.musicassistant.presentation.VibeApp
import com.vibe.musicassistant.presentation.home.HomeViewModel
import com.vibe.musicassistant.presentation.theme.VibeTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash until settings load, so onboarding never flashes for returning users.
        splash.setKeepOnScreenCondition { viewModel.state.value == null }
        enableEdgeToEdge()
        setContent {
            VibeTheme {
                VibeApp(viewModel)
            }
        }
    }
}
