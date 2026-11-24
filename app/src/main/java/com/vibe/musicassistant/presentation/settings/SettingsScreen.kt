package com.vibe.musicassistant.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.vibe.musicassistant.R
import com.vibe.musicassistant.domain.model.HomeState
import com.vibe.musicassistant.domain.model.OrbVisibility
import com.vibe.musicassistant.domain.model.VoiceLanguage
import com.vibe.musicassistant.presentation.components.ChoiceRow
import com.vibe.musicassistant.presentation.components.GlassCard
import com.vibe.musicassistant.presentation.components.SectionLabel
import com.vibe.musicassistant.presentation.components.VibeBackground
import com.vibe.musicassistant.presentation.components.VibeIconButton
import com.vibe.musicassistant.presentation.theme.VibeElectricBlue
import com.vibe.musicassistant.presentation.theme.VibeNavySurface
import com.vibe.musicassistant.presentation.theme.VibeShapes
import com.vibe.musicassistant.presentation.theme.VibeSpacing
import com.vibe.musicassistant.presentation.theme.VibeTextSecondary
import com.vibe.musicassistant.presentation.theme.VibeTheme

@Composable
fun SettingsScreen(
    state: HomeState,
    onToggleAssistant: (Boolean) -> Unit,
    onOrbVisibility: (OrbVisibility) -> Unit,
    onVoiceLanguage: (VoiceLanguage) -> Unit,
    onResetOrb: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
    }
    val settings = state.settings

    VibeBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VibeSpacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(VibeSpacing.lg),
        ) {
            Row(
                modifier = Modifier.padding(top = VibeSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VibeIconButton(R.drawable.ic_arrow_back, "Back", onBack)
                Spacer(Modifier.width(VibeSpacing.md))
                Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            }

            GlassCard {
                SectionLabel("Assistant")
                Spacer(Modifier.height(VibeSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SettingTitle("VIBE assistant")
                        SettingBody(
                            if (settings.assistantEnabled) "On. The orb floats over your apps."
                            else "Off. Nothing runs in the background.",
                        )
                    }
                    Switch(
                        checked = settings.assistantEnabled,
                        onCheckedChange = onToggleAssistant,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VibeElectricBlue,
                            uncheckedTrackColor = VibeNavySurface,
                        ),
                    )
                }
            }

            GlassCard {
                SectionLabel("Floating orb")
                Spacer(Modifier.height(VibeSpacing.sm))
                SettingTitle("Show the orb")
                SettingBody("\"With music\" shows it only while music plays or a music app is open. Needs Usage access to spot music apps.")
                Spacer(Modifier.height(VibeSpacing.md))
                ChoiceRow(
                    options = OrbVisibility.entries.toList(),
                    selected = settings.orbVisibility,
                    label = { it.label },
                    onSelect = onOrbVisibility,
                )
                Spacer(Modifier.height(VibeSpacing.md))
                LinkRow("Reset orb position", "Moves the orb back to the right edge.", onResetOrb)
            }

            GlassCard {
                SectionLabel("Voice")
                Spacer(Modifier.height(VibeSpacing.sm))
                SettingTitle("Speech language")
                SettingBody("English + Hinglish understands \"next song\" and \"agla gaana\". Choose हिंदी if you speak mostly Hindi.")
                Spacer(Modifier.height(VibeSpacing.md))
                ChoiceRow(
                    options = VoiceLanguage.entries.toList(),
                    selected = settings.voiceLanguage,
                    label = { it.label },
                    onSelect = onVoiceLanguage,
                )
                Spacer(Modifier.height(VibeSpacing.md))
                SettingBody("Try: next song · previous song · pause · play · volume up/down · skip ahead · rewind · agla gaana · pichla gaana · gaana band karo · music chalao · volume badhao · volume kam karo")
            }

            GlassCard {
                SectionLabel("System")
                Spacer(Modifier.height(VibeSpacing.sm))
                LinkRow("Permissions", "${state.grantedCount} of ${state.permissions.size} allowed", onOpenPermissions)
                LinkRow(
                    "Battery",
                    "If the orb disappears after a while, set VIBE to \"Unrestricted\" or \"Don't optimize\".",
                    onOpenBattery,
                )
                LinkRow(
                    "Diagnostics",
                    "See what VIBE heard and what your music app allowed. Use it when a command doesn't work.",
                    onOpenDiagnostics,
                )
            }

            GlassCard {
                SectionLabel("Privacy")
                Spacer(Modifier.height(VibeSpacing.sm))
                PrivacyPoint("The microphone turns on only after you tap the orb, and stops after your command.")
                PrivacyPoint("VIBE never records or saves audio.")
                PrivacyPoint("No account, no servers, no tracking. Settings stay on this phone.")
                PrivacyPoint("Notification access is used only to find and control the music player. VIBE ignores your notifications.")
                PrivacyPoint("Speech is turned into text by your phone's speech service (usually Google), which may use the internet.")
            }

            GlassCard {
                SectionLabel("About")
                Spacer(Modifier.height(VibeSpacing.sm))
                SettingTitle("VIBE $version")
                SettingBody("Your music. Your voice. Zero touch.")
            }

            Spacer(Modifier.height(VibeSpacing.lg))
        }
    }
}

@Composable
private fun LinkRow(title: String, body: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(VibeShapes.row)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = VibeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            SettingTitle(title)
            SettingBody(body)
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = VibeTextSecondary)
    }
}

@Composable
private fun SettingTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun SettingBody(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = VibeTextSecondary)
}

@Composable
private fun PrivacyPoint(text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = VibeSpacing.xs)) {
        Text("•", style = MaterialTheme.typography.bodyMedium, color = VibeElectricBlue)
        Spacer(Modifier.width(VibeSpacing.sm))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SettingsPreview() {
    VibeTheme {
        SettingsScreen(HomeState(), {}, {}, {}, {}, {}, {}, {}, {})
    }
}
