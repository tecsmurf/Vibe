package com.vibe.musicassistant.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vibe.musicassistant.domain.model.WakeModelState
import com.vibe.musicassistant.presentation.theme.VibeElectricBlue
import com.vibe.musicassistant.presentation.theme.VibeError
import com.vibe.musicassistant.presentation.theme.VibeNavySurface
import com.vibe.musicassistant.presentation.theme.VibeSpacing
import com.vibe.musicassistant.presentation.theme.VibeSuccess
import com.vibe.musicassistant.presentation.theme.VibeTextSecondary

/** Opt-in hands-free "Hey VIBE" switch with download progress and a plain privacy note. */
@Composable
fun WakeWordCard(
    enabled: Boolean,
    assistantOn: Boolean,
    model: WakeModelState,
    onToggle: (Boolean) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier) {
        SectionLabel("Hands-free")
        Spacer(Modifier.height(VibeSpacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Say \"Hey VIBE\"",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Wake VIBE without touching the phone, then say your command. " +
                        "The wake phrase is detected on this phone only. Nothing is recorded or sent. Uses extra battery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeTextSecondary,
                )
            }
            Spacer(Modifier.width(VibeSpacing.md))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = VibeElectricBlue,
                    uncheckedTrackColor = VibeNavySurface,
                ),
            )
        }
        if (enabled) {
            Spacer(Modifier.height(VibeSpacing.md))
            when (model) {
                is WakeModelState.Downloading -> {
                    Text(
                        "Downloading the voice model (one time, about 40 MB)… ${(model.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeTextSecondary,
                    )
                    Spacer(Modifier.height(VibeSpacing.sm))
                    LinearProgressIndicator(
                        progress = { model.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = VibeElectricBlue,
                        trackColor = VibeNavySurface,
                    )
                }
                is WakeModelState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        model.message,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeError,
                    )
                    Spacer(Modifier.width(VibeSpacing.sm))
                    SmallPillButton("Retry", onClick = onRetry)
                }
                WakeModelState.NotDownloaded -> SmallPillButton("Download voice model", onClick = onRetry)
                WakeModelState.Ready -> Text(
                    if (assistantOn) "Listening for \"Hey VIBE\" or \"Hello VIBE\". English wake phrase; commands can be Hindi or Hinglish."
                    else "Ready. Activate VIBE to start listening for \"Hey VIBE\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeSuccess,
                )
            }
        }
    }
}
