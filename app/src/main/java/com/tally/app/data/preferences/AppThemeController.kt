package com.tally.app.data.preferences

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes the app's [ThemeConfig] down to the OS framework so the window manager knows the night mode
 * *before* the next cold boot's `onCreate` runs — which is what draws the Splash Screen in the right
 * theme. [UiModeManager.setApplicationNightMode] (API 31+) persists this per-app at the framework
 * level, so a saved Light preference stops flashing the system's Dark splash on subsequent launches.
 */
@Singleton
class AppThemeController @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Sync the choice to the framework. No-op below API 31 (no OS-level per-app night-mode cache). */
    fun syncToFramework(config: ThemeConfig) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        uiModeManager.setApplicationNightMode(
            when (config) {
                ThemeConfig.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeConfig.DARK -> UiModeManager.MODE_NIGHT_YES
                ThemeConfig.FOLLOW_SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            },
        )
    }
}
