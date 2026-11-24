package com.vibe.musicassistant.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.vibe.musicassistant.presentation.theme.VibeGlassBottom
import com.vibe.musicassistant.presentation.theme.VibeGlassStroke
import com.vibe.musicassistant.presentation.theme.VibeGlassStrokeFaint
import com.vibe.musicassistant.presentation.theme.VibeGlassTop
import com.vibe.musicassistant.presentation.theme.VibeShapes

/** Frosted, softly outlined surface used for every card in VIBE. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(VibeShapes.card)
            .background(Brush.verticalGradient(listOf(VibeGlassTop, VibeGlassBottom)))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(VibeGlassStroke, VibeGlassStrokeFaint)),
                shape = VibeShapes.card,
            )
            .padding(contentPadding),
        content = content,
    )
}
