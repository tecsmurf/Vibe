package com.vibe.musicassistant.service.overlay

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibe.musicassistant.R
import com.vibe.musicassistant.domain.model.MediaInfo
import com.vibe.musicassistant.domain.model.VoiceStatus
import com.vibe.musicassistant.presentation.components.OrbState
import com.vibe.musicassistant.presentation.components.VibeOrb
import com.vibe.musicassistant.presentation.theme.VibeElectricBlue
import com.vibe.musicassistant.presentation.theme.VibeError
import com.vibe.musicassistant.presentation.theme.VibeNavy
import com.vibe.musicassistant.presentation.theme.VibeSuccess
import com.vibe.musicassistant.presentation.theme.VibeTextSecondary
import com.vibe.musicassistant.presentation.theme.VibeViolet
import com.vibe.musicassistant.presentation.theme.VibeWarning

const val OVERLAY_ORB_DP = 58
const val OVERLAY_ORB_CANVAS_SCALE = 1.45f

fun VoiceStatus.toOrbState(): OrbState = when (this) {
    VoiceStatus.Idle -> OrbState.IDLE
    is VoiceStatus.Listening -> OrbState.LISTENING
    is VoiceStatus.Processing -> OrbState.PROCESSING
    is VoiceStatus.Success -> OrbState.SUCCESS
    is VoiceStatus.Unconfirmed -> OrbState.IDLE
    is VoiceStatus.Error -> OrbState.ERROR
}

/** The floating orb itself. Touch handling lives in the service (it moves the window). */
@Composable
fun OverlayOrb(status: VoiceStatus) {
    VibeOrb(
        state = status.toOrbState(),
        diameter = OVERLAY_ORB_DP.dp,
        canvasScale = OVERLAY_ORB_CANVAS_SCALE,
    )
}

private val PanelShape = RoundedCornerShape(28.dp)

private fun Modifier.panelSurface(): Modifier = this
    .clip(PanelShape)
    .background(Brush.verticalGradient(listOf(VibeNavy.copy(alpha = 0.97f), Color(0xF2070B1E))))
    .border(1.dp, Brush.horizontalGradient(listOf(VibeElectricBlue.copy(alpha = 0.6f), VibeViolet.copy(alpha = 0.6f))), PanelShape)

/**
 * The bubble near the bottom of the screen: voice feedback, or the quick-control panel
 * after a long press on the orb.
 */
@Composable
fun OverlayBubble(
    status: VoiceStatus,
    panelOpen: Boolean,
    media: MediaInfo?,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onHideOrb: () -> Unit,
    onClosePanel: () -> Unit,
) {
    val mode: Any = if (panelOpen) "panel" else status
    AnimatedVisibility(
        visible = panelOpen || status != VoiceStatus.Idle,
        enter = fadeIn() + slideInVertically { it / 2 } + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + slideOutVertically { it / 2 } + scaleOut(targetScale = 0.9f),
    ) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "bubble",
        ) { current ->
            if (current == "panel") {
                QuickPanel(media, onPrevious, onPlayPause, onNext, onHideOrb, onClosePanel)
            } else if (current is VoiceStatus && current != VoiceStatus.Idle) {
                Feedback(current)
            } else {
                Spacer(Modifier.size(1.dp))
            }
        }
    }
}

@Composable
private fun Feedback(status: VoiceStatus) {
    val (title, body, accent) = when (status) {
        is VoiceStatus.Listening -> Triple("Listening…", status.partialText.ifBlank { "Try \"next song\" or \"agla gaana\"" }, VibeViolet)
        is VoiceStatus.Processing -> Triple("Got it", "\"${status.heard}\"", VibeElectricBlue)
        is VoiceStatus.Success -> Triple("Done", status.message, VibeSuccess)
        is VoiceStatus.Unconfirmed -> Triple("Sent", status.message, VibeWarning)
        is VoiceStatus.Error -> Triple("Hmm", status.message, VibeError)
        VoiceStatus.Idle -> Triple("", "", VibeTextSecondary)
    }
    Row(
        modifier = Modifier
            .widthIn(max = 340.dp)
            .panelSurface()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = accent)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuickPanel(
    media: MediaInfo?,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onHideOrb: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 340.dp)
            .panelSurface()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = media?.title ?: "No music playing",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = media?.let { listOfNotNull(it.artist, it.appName).joinToString(" · ") }
                ?: "Start a song in your music app",
            style = MaterialTheme.typography.bodySmall,
            color = VibeTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PanelButton(R.drawable.ic_close, "Close", onClose, small = true)
            PanelButton(R.drawable.ic_skip_previous, "Previous track", onPrevious)
            PanelButton(
                if (media?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play,
                if (media?.isPlaying == true) "Pause" else "Play",
                onPlayPause,
                primary = true,
            )
            PanelButton(R.drawable.ic_skip_next, "Next track", onNext)
            PanelButton(R.drawable.ic_visibility_off, "Hide orb", onHideOrb, small = true)
        }
    }
}

@Composable
private fun PanelButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    small: Boolean = false,
) {
    val size = when {
        primary -> 60.dp
        small -> 40.dp
        else -> 48.dp
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (primary) Brush.linearGradient(listOf(VibeElectricBlue, VibeViolet))
                else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.05f))),
            )
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(if (primary) 30.dp else if (small) 18.dp else 24.dp),
        )
    }
}
