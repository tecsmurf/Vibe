package com.vibe.musicassistant.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vibe.musicassistant.domain.model.AssistantStatus
import com.vibe.musicassistant.presentation.theme.VibeCyan
import com.vibe.musicassistant.presentation.theme.VibeElectricBlue
import com.vibe.musicassistant.presentation.theme.VibeShapes
import com.vibe.musicassistant.presentation.theme.VibeSuccess
import com.vibe.musicassistant.presentation.theme.VibeTextSecondary
import com.vibe.musicassistant.presentation.theme.VibeViolet
import com.vibe.musicassistant.presentation.theme.VibeWarning

fun AssistantStatus.color(): Color = when (this) {
    AssistantStatus.NOT_ACTIVATED -> VibeTextSecondary
    AssistantStatus.PERMISSION_REQUIRED -> VibeWarning
    AssistantStatus.WAITING_FOR_MUSIC -> VibeElectricBlue
    AssistantStatus.MUSIC_DETECTED, AssistantStatus.READY_TO_LISTEN -> VibeCyan
    AssistantStatus.LISTENING -> VibeViolet
    AssistantStatus.PROCESSING -> VibeElectricBlue
    AssistantStatus.COMMAND_EXECUTED -> VibeSuccess
}

fun AssistantStatus.orbState(): OrbState = when (this) {
    AssistantStatus.LISTENING -> OrbState.LISTENING
    AssistantStatus.PROCESSING -> OrbState.PROCESSING
    AssistantStatus.COMMAND_EXECUTED -> OrbState.SUCCESS
    else -> OrbState.IDLE
}

/** Pill showing the assistant status with a pulsing colored dot. */
@Composable
fun StatusChip(status: AssistantStatus, modifier: Modifier = Modifier) {
    val color by animateColorAsState(status.color(), label = "statusColor")
    val pulse by rememberInfiniteTransition(label = "statusPulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "dotAlpha",
    )
    val animated = status != AssistantStatus.NOT_ACTIVATED

    Row(
        modifier = modifier
            .clip(VibeShapes.pill)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), VibeShapes.pill)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .alpha(if (animated) pulse else 1f)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = status.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
