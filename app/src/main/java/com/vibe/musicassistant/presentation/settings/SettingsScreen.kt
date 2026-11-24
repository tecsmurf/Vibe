package com.vibe.musicassistant.presentation.settings

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.vibe.musicassistant.R
import com.vibe.musicassistant.domain.model.HomeState
import com.vibe.musicassistant.presentation.components.GlassCard
import com.vibe.musicassistant.presentation.components.SectionLabel
import com.vibe.musicassistant.presentation.components.VibeBackground
import com.vibe.musicassistant.presentation.components.VibeIconButton
import com.vibe.musicassistant.presentation.theme.VibeElectricBlue
import com.vibe.musicassistant.presentation.theme.VibeNavySurface
import com.vibe.musicassistant.presentation.theme.VibeSpacing
import com.vibe.musicassistant.presentation.theme.VibeTextSecondary
import com.vibe.musicassistant.presentation.theme.VibeTheme

@Composable
fun SettingsScreen(
    state: HomeState,
    onToggleAssistant: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
    }

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
                VibeIconButton(
                    icon = R.drawable.ic_arrow_back,
                    contentDescription = "Back",
                    onClick = onBack,
                )
                Spacer(Modifier.width(VibeSpacing.md))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            GlassCard {
                SectionLabel("Assistant")
                Spacer(Modifier.height(VibeSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SettingTitle("VIBE assistant")
                        SettingBody(if (state.assistantEnabled) "On. The orb is allowed to appear." else "Off. Nothing runs in the background.")
                    }
                    Switch(
                        checked = state.assistantEnabled,
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
                SectionLabel("Voice")
                Spacer(Modifier.height(VibeSpacing.sm))
                SettingTitle("Languages")
                SettingBody("English, Hindi and Hinglish. Try \"Next song\" or \"Agla gaana\".")
                Spacer(Modifier.height(VibeSpacing.md))
                SettingTitle("How to talk to VIBE")
                SettingBody("Tap the orb, then speak. There is no always-on listening.")
            }

            GlassCard {
                SectionLabel("Privacy")
                Spacer(Modifier.height(VibeSpacing.sm))
                PrivacyPoint("The microphone turns on only after you tap the orb, and stops after your command.")
                PrivacyPoint("Audio is never recorded or saved by VIBE.")
                PrivacyPoint("No account, no servers, no tracking. Settings stay on this phone.")
                PrivacyPoint("VIBE reads only what's playing so it can control it. It doesn't read your messages.")
                PrivacyPoint("Speech is turned into text by your phone's speech service, which may use the network.")
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
    VibeTheme { SettingsScreen(state = HomeState(), onToggleAssistant = {}, onBack = {}) }
}
