package com.vibe.musicassistant.presentation.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vibe.musicassistant.R
import com.vibe.musicassistant.domain.model.AssistantStatus
import com.vibe.musicassistant.presentation.components.OrbState
import com.vibe.musicassistant.presentation.components.VibeOrb
import com.vibe.musicassistant.presentation.theme.VibeCyan
import com.vibe.musicassistant.presentation.theme.VibeElectricBlue
import com.vibe.musicassistant.presentation.theme.VibeNavy
import com.vibe.musicassistant.presentation.theme.VibeNavyDeep
import com.vibe.musicassistant.presentation.theme.VibeNavySurface
import com.vibe.musicassistant.presentation.theme.VibeTheme
import com.vibe.musicassistant.presentation.theme.VibeViolet

/**
 * Phase 1 launch screen: brand, tagline, animated orb preview and current status.
 * The full home screen (activate button, permission and player status) is Phase 2.
 */
@Composable
fun LaunchScreen(
    status: AssistantStatus = AssistantStatus.NOT_ACTIVATED,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VibeNavy, VibeNavyDeep))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayLarge.copy(
                        brush = Brush.horizontalGradient(listOf(VibeElectricBlue, VibeViolet, VibeCyan)),
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            VibeOrb(state = OrbState.IDLE, diameter = 140.dp)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StatusPill(status)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Voice control for the music apps you already use.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(status: AssistantStatus) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(VibeNavySurface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant),
        )
        Text(
            text = status.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun LaunchScreenPreview() {
    VibeTheme { LaunchScreen() }
}
