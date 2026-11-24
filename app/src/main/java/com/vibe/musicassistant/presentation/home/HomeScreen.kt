package com.vibe.musicassistant.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.musicassistant.R
import com.vibe.musicassistant.domain.model.AssistantStatus
import com.vibe.musicassistant.domain.model.HomeState
import com.vibe.musicassistant.domain.model.PermissionState
import com.vibe.musicassistant.domain.model.VibePermission
import com.vibe.musicassistant.presentation.components.GlassCard
import com.vibe.musicassistant.presentation.components.OrbState
import com.vibe.musicassistant.presentation.components.SectionLabel
import com.vibe.musicassistant.presentation.components.StatusChip
import com.vibe.musicassistant.presentation.components.VibeBackground
import com.vibe.musicassistant.presentation.components.VibeButton
import com.vibe.musicassistant.presentation.components.VibeIconButton
import com.vibe.musicassistant.presentation.components.VibeOrb
import com.vibe.musicassistant.presentation.components.VibeWordmark
import com.vibe.musicassistant.presentation.components.orbState
import com.vibe.musicassistant.presentation.theme.VibeCyan
import com.vibe.musicassistant.presentation.theme.VibeElectricBlue
import com.vibe.musicassistant.presentation.theme.VibeShapes
import com.vibe.musicassistant.presentation.theme.VibeSpacing
import com.vibe.musicassistant.presentation.theme.VibeSuccess
import com.vibe.musicassistant.presentation.theme.VibeTextSecondary
import com.vibe.musicassistant.presentation.theme.VibeTheme
import com.vibe.musicassistant.presentation.theme.VibeViolet
import com.vibe.musicassistant.presentation.theme.VibeWarning
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    state: HomeState,
    onToggleAssistant: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    VibeBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VibeSpacing.screenGutter),
        ) {
            TopBar(onOpenSettings)
            Hero(state)
            Spacer(Modifier.height(VibeSpacing.xl))
            VibeButton(
                text = if (state.assistantEnabled) "Stop VIBE" else "Activate VIBE",
                outlined = state.assistantEnabled,
                onClick = { onToggleAssistant(!state.assistantEnabled) },
            )
            ActivationHint(state)
            Spacer(Modifier.height(VibeSpacing.xl))
            PlayerCard(state.connectedPlayer)
            Spacer(Modifier.height(VibeSpacing.lg))
            PermissionsCard(state)
            Spacer(Modifier.height(VibeSpacing.lg))
            PrivacyCard(onOpenSettings)
            Spacer(Modifier.height(VibeSpacing.xxl))
        }
    }
}

@Composable
private fun TopBar(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = VibeSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VibeOrb(state = OrbState.IDLE, diameter = 22.dp)
        Spacer(Modifier.width(VibeSpacing.xs))
        VibeWordmark(style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 4.sp))
        Spacer(Modifier.weight(1f))
        VibeIconButton(
            icon = R.drawable.ic_settings,
            contentDescription = "Settings",
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun Hero(state: HomeState) {
    // Tapping the orb plays a short preview of the listen -> process -> success cycle.
    var preview by remember { mutableStateOf<AssistantStatus?>(null) }
    var previewJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val shownStatus = preview ?: state.status

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(top = VibeSpacing.lg)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "Preview the orb",
                ) {
                    previewJob?.cancel()
                    previewJob = scope.launch {
                        preview = AssistantStatus.LISTENING
                        delay(2_200)
                        preview = AssistantStatus.PROCESSING
                        delay(1_200)
                        preview = AssistantStatus.COMMAND_EXECUTED
                        delay(1_200)
                        preview = null
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            VibeOrb(state = shownStatus.orbState(), diameter = 132.dp)
        }
        Text(
            text = "Your music. Your voice. Zero touch.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(VibeSpacing.sm))
        Text(
            text = "Tap the orb to preview it",
            style = MaterialTheme.typography.bodySmall,
            color = VibeTextSecondary,
        )
        Spacer(Modifier.height(VibeSpacing.lg))
        AnimatedContent(
            targetState = shownStatus,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "status",
        ) { status -> StatusChip(status) }
    }
}

@Composable
private fun ActivationHint(state: HomeState) {
    val text = when (state.status) {
        AssistantStatus.NOT_ACTIVATED ->
            "Activate once, then open Spotify or YouTube Music. The orb stays with you."
        AssistantStatus.PERMISSION_REQUIRED ->
            "VIBE is on, but needs ${state.missingRequired.size} more " +
                (if (state.missingRequired.size == 1) "permission" else "permissions") +
                " before the orb can appear."
        else -> "VIBE is on. Start music in any player and tap the orb to talk."
    }
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = VibeSpacing.md),
        style = MaterialTheme.typography.bodySmall,
        color = VibeTextSecondary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PlayerCard(connectedPlayer: String?) {
    GlassCard {
        SectionLabel("Now controlling")
        Spacer(Modifier.height(VibeSpacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(VibeElectricBlue.copy(alpha = 0.35f), VibeViolet.copy(alpha = 0.35f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(VibeSpacing.lg))
            Column {
                Text(
                    text = connectedPlayer ?: "No player connected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (connectedPlayer == null) {
                        "Play something in Spotify, YouTube Music or another player."
                    } else {
                        "Ready for your voice commands."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun PermissionsCard(state: HomeState) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Permissions")
            Spacer(Modifier.weight(1f))
            Text(
                text = "${state.grantedCount} of ${state.permissions.size} ready",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.missingRequired.isEmpty()) VibeSuccess else VibeWarning,
            )
        }
        Spacer(Modifier.height(VibeSpacing.sm))
        state.permissions.forEach { PermissionRow(it) }
    }
}

@Composable
private fun PermissionRow(item: PermissionState) {
    val accent = when {
        item.granted -> VibeSuccess
        item.permission.required -> VibeWarning
        else -> VibeTextSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VibeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            if (item.granted) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
        }
        Spacer(Modifier.width(VibeSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.permission.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.permission.reason,
                style = MaterialTheme.typography.bodySmall,
                color = VibeTextSecondary,
            )
        }
        Spacer(Modifier.width(VibeSpacing.sm))
        Text(
            text = when {
                item.granted -> "On"
                item.permission.required -> "Needed"
                else -> "Optional"
            },
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
    }
}

@Composable
private fun PrivacyCard(onOpenSettings: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .clip(VibeShapes.card)
            .clickable(role = Role.Button, onClick = onOpenSettings),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_shield),
                contentDescription = null,
                tint = VibeCyan,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(VibeSpacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Private by design",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Listens only after you tap the orb. No recordings saved. No account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeTextSecondary,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = VibeTextSecondary,
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    VibeTheme {
        HomeScreen(
            state = HomeState(
                assistantEnabled = true,
                permissions = VibePermission.entries.map { PermissionState(it, it.ordinal % 2 == 0) },
            ),
            onToggleAssistant = {},
            onOpenSettings = {},
        )
    }
}
