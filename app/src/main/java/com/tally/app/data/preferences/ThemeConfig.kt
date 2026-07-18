package com.tally.app.data.preferences

/**
 * The user's persisted theme choice. [FOLLOW_SYSTEM] is the first-launch default — the app tracks
 * the OS setting until the user explicitly picks [LIGHT] or [DARK] in Settings.
 */
enum class ThemeConfig {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** Parse a stored key back to a config, tolerating null/unknown values (default follow-system). */
        fun fromKey(key: String?): ThemeConfig =
            key?.let { runCatching { valueOf(it) }.getOrNull() } ?: FOLLOW_SYSTEM
    }
}
