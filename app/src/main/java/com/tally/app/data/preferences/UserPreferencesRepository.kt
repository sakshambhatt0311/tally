package com.tally.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads/writes user settings backed by Preferences DataStore. Survives process death — the theme
 * choice is read from disk on next launch instead of resetting to the system default.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    private object Keys {
        val THEME_CONFIG = stringPreferencesKey("theme_config")
    }

    /** The persisted theme choice, defaulting to [ThemeConfig.FOLLOW_SYSTEM] before any is saved. */
    val themeConfig: Flow<ThemeConfig> = dataStore.data
        // A read error (e.g. corrupt file) shouldn't crash the app — fall back to empty prefs.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> ThemeConfig.fromKey(prefs[Keys.THEME_CONFIG]) }

    /** Persist the user's theme choice; the [themeConfig] Flow re-emits automatically. */
    suspend fun updateThemeConfig(config: ThemeConfig) {
        dataStore.edit { prefs -> prefs[Keys.THEME_CONFIG] = config.name }
    }
}
