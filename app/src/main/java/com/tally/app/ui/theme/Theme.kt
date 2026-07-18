package com.tally.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TallyLightColorScheme = lightColorScheme(
    primary = TallyGreen,                     // Continue Solo, FAB, Create Circle, Join
    onPrimary = TallySurface,                  // white label text on solid-green buttons
    primaryContainer = TallyGreenContainer,    // Sign in to Sync, Add a Game, Local player chip
    onPrimaryContainer = TallyOnGreenContainer,

    secondary = TallyGreen,
    onSecondary = TallySurface,
    secondaryContainer = TallyGreenContainer,   // uniform "Synced" badge — pastel green
    onSecondaryContainer = TallyOnGreenContainer,

    background = TallyBackground,
    onBackground = TallyOnSurface,

    surface = TallySurface,
    onSurface = TallyOnSurface,
    surfaceVariant = TallySurfaceVariant,
    onSurfaceVariant = TallyOnSurfaceVariant,

    surfaceContainerLowest = TallySurfaceContainerLowest,
    surfaceContainerLow = TallySurfaceContainerLow,
    surfaceContainer = TallySurfaceContainer,
    surfaceContainerHigh = TallySurfaceContainerHigh,
    surfaceContainerHighest = TallySurfaceContainerHighest,

    outline = TallyOutline,
    outlineVariant = TallyOutlineVariant,

    error = GamePoker,
    onError = TallySurface,
)

/**
 * Dark scheme, flat by design. All surfaceContainer* tones collapse onto the same [DarkSurface]
 * so cards never gain tonal elevation — depth comes purely from [DarkBackground] vs [DarkSurface].
 */
private val TallyDarkColorScheme = darkColorScheme(
    primary = BrandMint,                       // Continue Solo, FAB — vibrant mint
    onPrimary = OnBrandMint,
    primaryContainer = BrandDarkGreen,         // Sign in to Sync, chips — muted forest
    onPrimaryContainer = OnBrandDarkGreen,

    secondary = BrandMint,
    onSecondary = OnBrandMint,
    secondaryContainer = BrandDarkGreen,
    onSecondaryContainer = OnBrandDarkGreen,

    background = DarkBackground,
    onBackground = DarkOnSurface,

    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,

    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurface,
    surfaceContainerHighest = DarkSurfaceVariant,

    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,

    error = GamePoker,
    onError = DarkOnSurface,
)

@Composable
fun TallyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Snap instantly — the smooth light↔dark transition is a hardware-accelerated root-level
    // Crossfade in MainActivity, not per-token color animation (which thrashed recomposition).
    MaterialTheme(
        colorScheme = if (darkTheme) TallyDarkColorScheme else TallyLightColorScheme,
        typography = TallyTypography,
        shapes = TallyShapes,
        content = content,
    )
}
