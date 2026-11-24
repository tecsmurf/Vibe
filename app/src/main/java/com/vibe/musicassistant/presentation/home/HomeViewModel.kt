package com.vibe.musicassistant.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.musicassistant.data.preferences.VibePreferencesRepository
import com.vibe.musicassistant.domain.model.HomeState
import com.vibe.musicassistant.permissions.VibePermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = VibePreferencesRepository(application)
    private val permissionManager = VibePermissionManager(application)
    private val permissions = MutableStateFlow(permissionManager.snapshot())

    val state: StateFlow<HomeState> =
        combine(preferences.assistantEnabled, permissions) { enabled, perms ->
            HomeState(assistantEnabled = enabled, permissions = perms)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeState(permissions = permissions.value),
        )

    /** Permissions change in system settings, so re-read them every time the app resumes. */
    fun refreshPermissions() {
        permissions.value = permissionManager.snapshot()
    }

    fun setAssistantEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setAssistantEnabled(enabled) }
    }
}
