package com.vibe.musicassistant.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vibe.musicassistant.domain.model.OrbVisibility
import com.vibe.musicassistant.domain.model.VibeSettings
import com.vibe.musicassistant.domain.model.VoiceLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.vibeDataStore: DataStore<Preferences> by preferencesDataStore(name = "vibe_preferences")

/** All persisted VIBE settings, stored locally with DataStore. */
class VibePreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.vibeDataStore

    val settings: Flow<VibeSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { p ->
            VibeSettings(
                assistantEnabled = p[ENABLED] ?: false,
                onboardingDone = p[ONBOARDING_DONE] ?: false,
                orbVisibility = p[ORB_VISIBILITY]?.let { runCatching { OrbVisibility.valueOf(it) }.getOrNull() }
                    ?: OrbVisibility.ALWAYS,
                voiceLanguage = p[VOICE_LANGUAGE]?.let { runCatching { VoiceLanguage.valueOf(it) }.getOrNull() }
                    ?: VoiceLanguage.ENGLISH_HINGLISH,
                orbX = p[ORB_X] ?: -1,
                orbY = p[ORB_Y] ?: -1,
            )
        }
        .distinctUntilChanged()

    suspend fun setAssistantEnabled(enabled: Boolean) = dataStore.edit { it[ENABLED] = enabled }
    suspend fun setOnboardingDone() = dataStore.edit { it[ONBOARDING_DONE] = true }
    suspend fun setOrbVisibility(value: OrbVisibility) = dataStore.edit { it[ORB_VISIBILITY] = value.name }
    suspend fun setVoiceLanguage(value: VoiceLanguage) = dataStore.edit { it[VOICE_LANGUAGE] = value.name }

    suspend fun setOrbPosition(x: Int, y: Int) = dataStore.edit {
        it[ORB_X] = x
        it[ORB_Y] = y
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("assistant_enabled")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val ORB_VISIBILITY = stringPreferencesKey("orb_visibility")
        val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
        val ORB_X = intPreferencesKey("orb_x")
        val ORB_Y = intPreferencesKey("orb_y")
    }
}
