package com.vibe.musicassistant.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vibe.musicassistant.domain.model.HomeState
import com.vibe.musicassistant.domain.model.VibePermission
import com.vibe.musicassistant.presentation.components.GlassCard
import com.vibe.musicassistant.presentation.components.OrbState
import com.vibe.musicassistant.presentation.components.PermissionsList
import com.vibe.musicassistant.presentation.components.VibeBackground
import com.vibe.musicassistant.presentation.components.VibeButton
import com.vibe.musicassistant.presentation.components.VibeOrb
import com.vibe.musicassistant.presentation.components.VibeWordmark
import com.vibe.musicassistant.presentation.theme.VibeElectricBlue
import com.vibe.musicassistant.presentation.theme.VibeSpacing
import com.vibe.musicassistant.presentation.theme.VibeTextSecondary
import com.vibe.musicassistant.presentation.theme.VibeViolet

private const val PAGES = 3

@Composable
fun OnboardingScreen(
    state: HomeState,
    onGrant: (VibePermission) -> Unit,
    onOpenAppInfo: () -> Unit,
    onFinish: (activateNow: Boolean) -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }

    VibeBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = VibeSpacing.screenGutter, vertical = VibeSpacing.lg),
        ) {
            AnimatedContent(
                targetState = page,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
                },
                label = "onboarding",
            ) { current ->
                when (current) {
                    0 -> WelcomePage()
                    1 -> HowItWorksPage()
                    else -> PermissionsPage(state, onGrant, onOpenAppInfo)
                }
            }

            Dots(page)
            Spacer(Modifier.height(VibeSpacing.lg))
            val ready = state.missingRequired.isEmpty()
            VibeButton(
                text = when {
                    page == 0 -> "Get started"
                    page < PAGES - 1 -> "Continue"
                    ready -> "Activate VIBE"
                    else -> "Finish later"
                },
                outlined = page == PAGES - 1 && !ready,
                onClick = {
                    if (page < PAGES - 1) page++ else onFinish(ready)
                },
            )
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VibeOrb(state = OrbState.LISTENING, diameter = 150.dp)
        Spacer(Modifier.height(VibeSpacing.xl))
        VibeWordmark()
        Spacer(Modifier.height(VibeSpacing.sm))
        Text(
            "Your music. Your voice. Zero touch.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(VibeSpacing.lg))
        Text(
            "A floating orb that controls Spotify, YouTube Music and other players with your voice. " +
                "Perfect for the shower, the kitchen or the gym.",
            style = MaterialTheme.typography.bodyLarge,
            color = VibeTextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HowItWorksPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "How VIBE works",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(VibeSpacing.xl))
        Step(1, "Activate once", "Turn VIBE on, then leave the app. The orb stays on top.")
        Step(2, "Play your music", "Use Spotify, YouTube Music or any player you like.")
        Step(3, "Tap the orb and speak", "\"Next song\", \"Agla gaana\", \"Pause music\", \"Volume badhao\".")
        Step(4, "Long-press for controls", "Previous, play/pause and next, without opening anything.")
        Spacer(Modifier.height(VibeSpacing.lg))
        GlassCard {
            Text(
                "VIBE listens only after you tap the orb. Nothing is recorded or uploaded by VIBE.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Step(number: Int, title: String, body: String) {
    Row(Modifier.padding(vertical = VibeSpacing.md)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(VibeElectricBlue, VibeViolet))),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        Spacer(Modifier.width(VibeSpacing.lg))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = VibeTextSecondary)
        }
    }
}

@Composable
private fun PermissionsPage(
    state: HomeState,
    onGrant: (VibePermission) -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(VibeSpacing.lg))
        Text(
            "A few permissions",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(VibeSpacing.sm))
        Text(
            "Each one opens the right Android setting. Come back here after you switch it on.",
            style = MaterialTheme.typography.bodyMedium,
            color = VibeTextSecondary,
        )
        Spacer(Modifier.height(VibeSpacing.lg))
        PermissionsList(state.permissions, onGrant, onOpenAppInfo)
    }
}

@Composable
private fun Dots(page: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(PAGES) { i ->
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = if (i == page) 22.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (i == page) VibeElectricBlue else Color.White.copy(alpha = 0.2f)),
            )
        }
    }
}
