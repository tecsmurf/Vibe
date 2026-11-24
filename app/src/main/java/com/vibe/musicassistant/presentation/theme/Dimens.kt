package com.vibe.musicassistant.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Spacing scale used across VIBE screens (4dp grid). */
object VibeSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val screenGutter = 20.dp
}

object VibeShapes {
    val card = RoundedCornerShape(24.dp)
    val row = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(percent = 50)
}
