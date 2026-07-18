package com.tally.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.preferences.AppThemeController
import com.tally.app.data.preferences.ThemeConfig
import com.tally.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Settings screen. Owns the persisted theme choice (read/written via DataStore so it
 * survives app restarts) plus the in-memory notifications toggle. Activity-scoped in MainActivity,
 * so the single instance is shared across both theme-crossfade layers.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appThemeController: AppThemeController,
) : ViewModel() {

    // null = first DataStore read pending; MainActivity falls back to the system theme until it loads.
    val themeConfig: StateFlow<ThemeConfig?> = userPreferencesRepository.themeConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    /**
     * The Settings screen exposes a binary dark switch; map it onto the persisted [ThemeConfig] and
     * write it to disk. (First launch stays FOLLOW_SYSTEM until the user flips this.)
     */
    fun setDarkMode(enabled: Boolean) {
        val config = if (enabled) ThemeConfig.DARK else ThemeConfig.LIGHT
        // Sync to the OS framework now so the next cold boot's Splash Screen uses this theme.
        appThemeController.syncToFramework(config)
        viewModelScope.launch {
            userPreferencesRepository.updateThemeConfig(config)
        }
    }
}
    