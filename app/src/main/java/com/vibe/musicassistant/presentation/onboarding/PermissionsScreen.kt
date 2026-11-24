package com.vibe.musicassistant.presentation.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vibe.musicassistant.R
import com.vibe.musicassistant.domain.model.HomeState
import com.vibe.musicassistant.domain.model.VibePermission
import com.vibe.musicassistant.presentation.components.PermissionsList
import com.vibe.musicassistant.presentation.components.VibeBackground
import com.vibe.musicassistant.presentation.components.VibeButton
import com.vibe.musicassistant.presentation.components.VibeIconButton
import com.vibe.musicassistant.presentation.theme.VibeSpacing
import com.vibe.musicassistant.presentation.theme.VibeTextSecondary

/** Permissions dashboard reachable from Home and Settings. */
@Composable
fun PermissionsScreen(
    state: HomeState,
    onGrant: (VibePermission) -> Unit,
    onOpenAppInfo: () -> Unit,
    onActivate: () -> Unit,
    onBack: () -> Unit,
) {
    VibeBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VibeSpacing.screenGutter),
        ) {
            Row(
                modifier = Modifier.padding(top = VibeSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VibeIconButton(R.drawable.ic_arrow_back, "Back", onBack)
                Spacer(Modifier.width(VibeSpacing.md))
                Text("Permissions", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.height(VibeSpacing.sm))
            Text(
                "${state.grantedCount} of ${state.permissions.size} allowed. VIBE still opens without the optional ones.",
                style = MaterialTheme.typography.bodyMedium,
                color = VibeTextSecondary,
            )
            Spacer(Modifier.height(VibeSpacing.lg))
            PermissionsList(state.permissions, onGrant, onOpenAppInfo)
            if (state.missingRequired.isEmpty() && !state.assistantEnabled) {
                Spacer(Modifier.height(VibeSpacing.sm))
                VibeButton("Activate VIBE", onActivate)
            }
            Spacer(Modifier.height(VibeSpacing.xxl))
        }
    }
}
