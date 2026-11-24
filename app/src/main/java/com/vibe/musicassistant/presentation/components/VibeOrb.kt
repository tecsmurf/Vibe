package com.vibe.musicassistant.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vibe.musicassistant.presentation.theme.VibeElectricBlue
import com.vibe.musicassistant.presentation.theme.VibeError
import com.vibe.musicassistant.presentation.theme.VibeSuccess
import com.vibe.musicassistant.presentation.theme.VibeViolet
import kotlin.math.PI
import kotlin.math.sin

/** Visual states of the orb. The floating overlay (Phase 4) reuses this component. */
enum class OrbState { IDLE, LISTENING, PROCESSING, SUCCESS, ERROR }

private val BarRestHeights = floatArrayOf(0.35f, 0.6f, 0.9f, 0.55f, 0.3f)

/**
 * The VIBE orb: a blue-to-violet gradient sphere with a soft outer glow and a
 * five-bar waveform. [diameter] is the core sphere; the canvas reserves extra
 * room around it for the glow.
 */
@Composable
fun VibeOrb(
    modifier: Modifier = Modifier,
    state: OrbState = OrbState.IDLE,
    diameter: Dp = 60.dp,
) {
    val transition = rememberInfiniteTransition(label = "orb")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "wavePhase",
    )
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "breath",
    )

    val amplitude by animateFloatAsState(
        targetValue = when (state) {
            OrbState.IDLE -> 0.12f
            OrbState.LISTENING -> 1f
            OrbState.PROCESSING -> 0.35f
            OrbState.SUCCESS, OrbState.ERROR -> 0f
        },
        label = "amplitude",
    )
    val glowStrength by animateFloatAsState(
        targetValue = when (state) {
            OrbState.IDLE -> 0.35f
            OrbState.LISTENING -> 0.75f
            OrbState.PROCESSING -> 0.55f
            OrbState.SUCCESS, OrbState.ERROR -> 0.6f
        },
        label = "glow",
    )
    val glowColor by animateColorAsState(
        targetValue = when (state) {
            OrbState.SUCCESS -> VibeSuccess
            OrbState.ERROR -> VibeError
            else -> VibeViolet
        },
        label = "glowColor",
    )

    Canvas(modifier = modifier.size(diameter * 1.8f)) {
        val pulseScale = if (state == OrbState.PROCESSING) 1f + 0.06f * breath else 1f
        val coreRadius = diameter.toPx() / 2f * pulseScale
        val glowRadius = coreRadius * (1.45f + 0.25f * breath)

        // Outer glow.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = glowStrength), Color.Transparent),
                center = center,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = center,
        )

        // Core sphere.
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(VibeElectricBlue, VibeViolet),
                start = Offset(center.x - coreRadius, center.y - coreRadius),
                end = Offset(center.x + coreRadius, center.y + coreRadius),
            ),
            radius = coreRadius,
            center = center,
        )

        // Soft highlight for depth.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(center.x - coreRadius * 0.35f, center.y - coreRadius * 0.4f),
                radius = coreRadius * 0.8f,
            ),
            radius = coreRadius,
            center = center,
        )

        // Waveform.
        val barCount = BarRestHeights.size
        val barWidth = coreRadius * 0.13f
        val gap = coreRadius * 0.11f
        val totalWidth = barCount * barWidth + (barCount - 1) * gap
        val maxHeight = coreRadius * 1.05f
        for (i in 0 until barCount) {
            val wobble = (sin(phase + i * 0.9f) + 1f) / 2f
            val level = (BarRestHeights[i] * (1f - amplitude) + wobble * amplitude)
                .coerceIn(0.12f, 1f)
            val height = maxHeight * level
            val x = center.x - totalWidth / 2f + i * (barWidth + gap)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.92f),
                topLeft = Offset(x, center.y - height / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Preview(backgroundColor = 0xFF050816, showBackground = true)
@Composable
private fun VibeOrbPreview() {
    VibeOrb(state = OrbState.LISTENING, diameter = 64.dp)
}
