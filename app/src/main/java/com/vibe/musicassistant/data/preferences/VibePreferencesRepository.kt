package com.vibe.musicassistant.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.vibeDataStore: DataStore<Preferences> by preferencesDataStore(name = "vibe_preferences")

/** Small persisted settings, stored locally with DataStore. */
class VibePreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.vibeDataStore

    val assistantEnabled: Flow<Boolean> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> prefs[KEY_ASSISTANT_ENABLED] ?: false }

    suspend fun setAssistantEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_ASSISTANT_ENABLED] = enabled }
    }

    private companion object {
        val KEY_ASSISTANT_ENABLED = booleanPreferencesKey("assistant_enabled")
    }
}
