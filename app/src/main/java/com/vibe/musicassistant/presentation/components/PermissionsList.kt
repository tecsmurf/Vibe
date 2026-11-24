package com.vibe.musicassistant.presentation.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.vibe.musicassistant.R
import com.vibe.musicassistant.domain.model.PermissionState
import com.vibe.musicassistant.domain.model.VibePermission
import com.vibe.musicassistant.presentation.theme.VibeElectricBlue
import com.vibe.musicassistant.presentation.theme.VibeShapes
import com.vibe.musicassistant.presentation.theme.VibeSpacing
import com.vibe.musicassistant.presentation.theme.VibeSuccess
import com.vibe.musicassistant.presentation.theme.VibeTextSecondary
import com.vibe.musicassistant.presentation.theme.VibeViolet
import com.vibe.musicassistant.presentation.theme.VibeWarning

/** One card per access: why VIBE needs it, its status, and a button to grant it. */
@Composable
fun PermissionsList(
    permissions: List<PermissionState>,
    onGrant: (VibePermission) -> Unit,
    onOpenAppInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        permissions.forEach { item ->
            PermissionCard(item, onGrant, onOpenAppInfo)
            Spacer(Modifier.height(VibeSpacing.md))
        }
    }
}

@Composable
private fun PermissionCard(
    item: PermissionState,
    onGrant: (VibePermission) -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    val accent = when {
        item.granted -> VibeSuccess
        item.permission.required -> VibeWarning
        else -> VibeTextSecondary
    }
    GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                if (item.granted) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = accent, modifier = Modifier.size(18.dp))
                } else {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                }
            }
            Spacer(Modifier.width(VibeSpacing.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.permission.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (item.permission.required && !item.granted) {
                        Spacer(Modifier.width(VibeSpacing.sm))
                        Text("REQUIRED", style = MaterialTheme.typography.labelSmall, color = VibeWarning)
                    }
                }
                Text(
                    item.permission.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = VibeTextSecondary,
                )
            }
            Spacer(Modifier.width(VibeSpacing.sm))
            if (item.granted) {
                Text("ON", style = MaterialTheme.typography.labelSmall, color = VibeSuccess)
            } else {
                SmallPillButton("Allow") { onGrant(item.permission) }
            }
        }
        if (!item.granted && item.permission == VibePermission.NOTIFICATION_ACCESS &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            Spacer(Modifier.height(VibeSpacing.md))
            Text(
                "Switch greyed out? Android limits this for apps installed from a file. Open App info, " +
                    "tap ⋮ (top right) → \"Allow restricted settings\", then come back and tap Allow.",
                style = MaterialTheme.typography.bodySmall,
                color = VibeTextSecondary,
            )
            Spacer(Modifier.height(VibeSpacing.sm))
            Text(
                "Open App info",
                modifier = Modifier
                    .clip(VibeShapes.pill)
                    .clickable(role = Role.Button, onClick = onOpenAppInfo)
                    .padding(vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = VibeElectricBlue,
            )
        }
    }
}

@Composable
fun SmallPillButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(VibeShapes.pill)
            .background(Brush.horizontalGradient(listOf(VibeElectricBlue, VibeViolet)))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

/** Two-or-more option pill selector. */
@Composable
fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(VibeShapes.pill)
            .background(Color.White.copy(alpha = 0.05f))
            .padding(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(VibeShapes.pill)
                    .then(
                        if (isSelected) Modifier.background(Brush.horizontalGradient(listOf(VibeElectricBlue, VibeViolet)))
                        else Modifier,
                    )
                    .clickable(role = Role.RadioButton) { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else VibeTextSecondary,
                )
            }
        }
    }
}
