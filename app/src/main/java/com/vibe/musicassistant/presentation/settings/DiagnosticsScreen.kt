package com.vibe.musicassistant.presentation.settings

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibe.musicassistant.R
import com.vibe.musicassistant.presentation.components.GlassCard
import com.vibe.musicassistant.presentation.components.SectionLabel
import com.vibe.musicassistant.presentation.components.SmallPillButton
import com.vibe.musicassistant.presentation.components.VibeBackground
import com.vibe.musicassistant.presentation.components.VibeIconButton
import com.vibe.musicassistant.presentation.theme.VibeSpacing
import com.vibe.musicassistant.presentation.theme.VibeTextSecondary
import com.vibe.musicassistant.service.VibeDiagnostics

/**
 * Shows what VIBE heard, which command it chose, what the music player supports and what
 * happened, so a problem on a real phone can be described. Kept in memory only.
 */
@Composable
fun DiagnosticsScreen(
    summary: () -> String,
    fullText: () -> String,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val entries by VibeDiagnostics.entries.collectAsStateWithLifecycle()
    // Recomputed whenever the log changes, so the session list stays current.
    val summaryText = remember(entries) { summary() }

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
                Text("Diagnostics", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            }

            GlassCard {
                SectionLabel("How to use")
                Spacer(Modifier.height(VibeSpacing.sm))
                Body(
                    "Play a song, then try \"previous song\", \"play <song name>\" and \"Hey VIBE\". " +
                        "Come back here and tap Copy to share the log. It stays in memory on this phone and " +
                        "is wiped when VIBE closes. It contains the words VIBE heard, so read it before sharing.",
                )
                Spacer(Modifier.height(VibeSpacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(VibeSpacing.sm)) {
                    SmallPillButton("Copy") { copy(context, fullText()) }
                    SmallPillButton("Clear") { onClear() }
                }
            }

            GlassCard {
                SectionLabel("This phone")
                Spacer(Modifier.height(VibeSpacing.sm))
                Mono(summaryText)
            }

            GlassCard {
                SectionLabel("Events (${entries.size})")
                Spacer(Modifier.height(VibeSpacing.sm))
                if (entries.isEmpty()) {
                    Body("Nothing yet. Tap the orb and say a command.")
                } else {
                    entries.asReversed().forEach { Mono(VibeDiagnostics.format(it)) }
                }
            }

            Spacer(Modifier.height(VibeSpacing.lg))
        }
    }
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    val clip = ClipData.newPlainText("VIBE diagnostics", text)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Keeps the text out of the clipboard preview pop-up.
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    clipboard.setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = VibeTextSecondary)
}

@Composable
private fun Mono(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
