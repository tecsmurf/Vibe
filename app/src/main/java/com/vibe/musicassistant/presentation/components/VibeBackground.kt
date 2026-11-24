package com.vibe.musicassistant.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.vibe.musicassistant.presentation.theme.VibeCyan
import com.vibe.musicassistant.presentation.theme.VibeElectricBlue
import com.vibe.musicassistant.presentation.theme.VibeNavyDeep
import com.vibe.musicassistant.presentation.theme.VibeViolet

/** Deep-navy screen background with slow-drifting blue and violet light. */
@Composable
fun VibeBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VibeNavyDeep)
            .drawBehind {
                val w = size.width
                val h = size.height
                glow(VibeElectricBlue, 0.20f, Offset(w * (0.1f + 0.25f * drift), h * 0.12f), w * 0.95f)
                glow(VibeViolet, 0.16f, Offset(w * (0.95f - 0.3f * drift), h * (0.5f + 0.1f * drift)), w * 0.85f)
                glow(VibeCyan, 0.06f, Offset(w * (0.3f + 0.2f * drift), h * 0.95f), w * 0.7f)
            },
        content = content,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.glow(
    color: Color,
    alpha: Float,
    center: Offset,
    radius: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
