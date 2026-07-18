package com.tally.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.tally.app.data.preferences.ThemeConfig
import com.tally.app.navigation.TallyApp
import com.tally.app.ui.theme.TallyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate — swaps the splash theme in and installs the keep/exit hooks.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the splash frame until DataStore has yielded the persisted theme (no theme flicker).
        splashScreen.setKeepOnScreenCondition {
            mainViewModel.uiState.value is MainActivityUiState.Loading
        }
        // Gentle dismissal: a soft cross-fade with a barely-there scale-down on the icon — the splash
        // recedes and the Compose tree fades in beneath. No zoom, easy on the eyes.
        splashScreen.setOnExitAnimationListener { splashProvider ->
            val iconView = splashProvider.iconView

            // Icon fades out while easing back a touch (1.0 -> 0.95) for a soft "receding" feel.
            val iconFade = ObjectAnimator.ofPropertyValuesHolder(
                iconView,
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.95f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.95f),
            ).apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = 280L
            }

            // Whole splash canvas fades out to reveal the underlying UI.
            val screenFade = ObjectAnimator.ofFloat(splashProvider.view, View.ALPHA, 1f, 0f).apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = 280L
            }

            AnimatorSet().apply {
                playTogether(iconFade, screenFade)
                // Clear the splash window exactly when the fade finishes.
                doOnEnd { splashProvider.remove() }
                start()
            }
        }

        setContent {
            // Activity-scoped (Hilt) so the single instance is shared by both Crossfade layers.
            val settingsViewModel: SettingsViewModel = hiltViewModel()

            // Persisted theme choice from DataStore. null = first read pending → follow the system
            // for now (default is FOLLOW_SYSTEM anyway, so no light↔dark flash on a cold start).
            val themeConfig by settingsViewModel.themeConfig.collectAsStateWithLifecycle()
            val isDark = when (themeConfig) {
                ThemeConfig.DARK -> true
                ThemeConfig.LIGHT -> false
                ThemeConfig.FOLLOW_SYSTEM, null -> isSystemInDarkTheme()
            }

            val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()

            // Bars stay transparent; the animated Compose canvas shows through, so they fade too.
            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark },
                )
                onDispose {}
            }

            // One NavHost, shared across both crossfade layers, so nav state survives the fade.
            val navController = rememberNavController()

            // Hardware-accelerated, root-level crossfade of the whole rendered tree — crisp 200ms
            // snap, no node-by-node color recomposition.
            Crossfade(
                targetState = isDark,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "themeCrossfade",
            ) { dark ->
                TallyTheme(darkTheme = dark) {
                    TallyApp(
                        isDarkMode = isDark,
                        onThemeToggle = settingsViewModel::setDarkMode,
                        notificationsEnabled = notificationsEnabled,
                        onNotificationsToggle = settingsViewModel::setNotificationsEnabled,
                        navController = navController,
                    )
                }
            }
        }
    }
}
