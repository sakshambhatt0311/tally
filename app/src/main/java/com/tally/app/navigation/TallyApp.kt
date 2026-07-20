package com.tally.app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.app.ui.viewmodel.AuthUiState
import com.tally.app.ui.viewmodel.AuthViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tally.app.ui.dashboard.CircleDashboardScreen
import com.tally.app.ui.screens.AddGameScreen
import kotlinx.coroutines.launch
import com.tally.app.ui.screens.CircleReadyScreen
import com.tally.app.ui.screens.GameDetailScreen
import com.tally.app.ui.screens.HeadToHeadScreen
import com.tally.app.ui.screens.JoinCircleScreen
import com.tally.app.ui.screens.LocalCircleCreatedScreen
import com.tally.app.ui.screens.ManageCirclesScreen
import com.tally.app.ui.screens.MyCirclesScreen
import com.tally.app.ui.screens.NewCircleScreen
import com.tally.app.ui.screens.SettingsScreen
import com.tally.app.ui.screens.WelcomeScreen
import com.tally.app.ui.session.LogSessionScreen
import com.tally.app.ui.viewmodel.CirclesViewModel
import kotlin.math.absoluteValue

@Composable
fun TallyApp(
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val startDestination = if (authState is AuthUiState.GoogleLinked) {
        TallyRoute.MyCircles
    } else {
        TallyRoute.Welcome
    }
    // Themed root surface behind the whole graph — covers the window during nav transition
    // animations so no white/black flash shows through when the theme is dark.
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        // DO NOT alter transition animations implicitly. All navigation transitions must be explicitly defined here to prevent OS overrides.
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() },
        sizeTransform = { null },
    ) {

        composable<TallyRoute.Welcome> {
            WelcomeScreen(
                // Both paths reach the same app — signing in only adds sync on top.
                onContinueSolo = {
                    navController.navigate(TallyRoute.MyCircles) {
                        popUpTo(TallyRoute.Welcome) { inclusive = true }
                    }
                }
            )
        }

        composable<TallyRoute.MyCircles> { entry ->
            val circlesViewModel: CirclesViewModel = hiltViewModel()
            val circles by circlesViewModel.circles.collectAsStateWithLifecycle()
            MyCirclesScreen(
                circles = circles,
                // RESUMED guard: fire exactly once per intent. Blocks a duplicate destination from a
                // rapid double-tap, but with instant transitions the screen resumes immediately so the
                // very first tap after reopening still registers.
                onCircleClick = { circleId, circleName ->
                    if (entry.lifecycleIsResumed()) {
                        navController.navigate(TallyRoute.CircleDashboard(circleId, circleName))
                    }
                },
                onCreateCircleClick = {
                    if (entry.lifecycleIsResumed()) navController.navigate(TallyRoute.CreateCircle)
                },
                onJoinCircleClick = {
                    if (entry.lifecycleIsResumed()) navController.navigate(TallyRoute.JoinCircle)
                },
                onSettingsClick = {
                    if (entry.lifecycleIsResumed()) navController.navigate(TallyRoute.Settings)
                },
                isAuthenticated = authState is AuthUiState.GoogleLinked,
            )
        }

        composable<TallyRoute.CreateCircle> {
            val circlesViewModel: CirclesViewModel = hiltViewModel()
            val coroutineScope = rememberCoroutineScope()
            NewCircleScreen(
                onBackClick = { navController.popBackStack() },
                onCreateCircle = { circleName, isOnline ->
                    coroutineScope.launch {
                        val circleId = circlesViewModel.createCircle(circleName, isOnline)
                        if (circleId != null) {
                            if (isOnline) {
                                // Online success returns to MyCircles
                                navController.navigate(TallyRoute.MyCircles) {
                                    popUpTo(TallyRoute.CreateCircle) { inclusive = true }
                                }
                            } else {
                                // Local continues to add players
                                navController.navigate(TallyRoute.LocalCircleCreated(circleId, circleName)) {
                                    popUpTo(TallyRoute.CreateCircle) { inclusive = true }
                                }
                            }
                        }
                    }
                },
            )
        }

        composable<TallyRoute.JoinCircle> {
            JoinCircleScreen(
                onBackClick = { navController.popBackStack() },
                onJoined = { circleId, circleName ->
                    navController.navigate(TallyRoute.CircleDashboard(circleId, circleName)) {
                        popUpTo(TallyRoute.MyCircles)
                    }
                },
            )
        }

        composable<TallyRoute.CircleCreated> { backStackEntry ->
            val route = backStackEntry.toRoute<TallyRoute.CircleCreated>()
            CircleReadyScreen(
                circleName = route.circleName,
                shareCode = shareCodeFor(route.circleId),
                onDone = {
                    navController.navigate(TallyRoute.CircleDashboard(route.circleId, route.circleName)) {
                        popUpTo(TallyRoute.MyCircles)
                    }
                },
                // "Add players" opens the circle straight on the Members tab (index 3).
                onAddPlayersInstead = {
                    navController.navigate(TallyRoute.CircleDashboard(route.circleId, route.circleName, initialTabIndex = 3)) {
                        popUpTo(TallyRoute.MyCircles)
                    }
                },
            )
        }

        composable<TallyRoute.LocalCircleCreated> { backStackEntry ->
            val route = backStackEntry.toRoute<TallyRoute.LocalCircleCreated>()
            LocalCircleCreatedScreen(
                circleName = route.circleName,
                // "Add players" opens the circle straight on the Members tab (index 3).
                onAddPlayers = {
                    navController.navigate(TallyRoute.CircleDashboard(route.circleId, route.circleName, initialTabIndex = 3)) {
                        popUpTo(TallyRoute.MyCircles)
                    }
                },
                onDone = {
                    navController.navigate(TallyRoute.CircleDashboard(route.circleId, route.circleName)) {
                        popUpTo(TallyRoute.MyCircles)
                    }
                },
            )
        }

        composable<TallyRoute.CircleDashboard> { backStackEntry ->
            val route = backStackEntry.toRoute<TallyRoute.CircleDashboard>()
            CircleDashboardScreen(
                circleId = route.circleId,
                circleName = route.circleName,
                initialTabIndex = route.initialTabIndex,
                onBackClick = { navController.popBackStack() },
                onLogSessionClick = { navController.navigate(TallyRoute.LogSession(route.circleId)) },
                onViewHeadToHead = { playerAId, playerBId ->
                    navController.navigate(TallyRoute.HeadToHead(route.circleId, playerAId, playerBId))
                },
                onGameClick = { gameId ->
                    navController.navigate(TallyRoute.GameDetail(route.circleId, route.circleName, gameId))
                },
            )
        }

        composable<TallyRoute.GameDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<TallyRoute.GameDetail>()
            GameDetailScreen(
                circleName = route.circleName,
                gameId = route.gameId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<TallyRoute.AddGame> { backStackEntry ->
            val route = backStackEntry.toRoute<TallyRoute.AddGame>()
            AddGameScreen(
                circleId = route.circleId,
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<TallyRoute.LogSession> { backStackEntry ->
            val route = backStackEntry.toRoute<TallyRoute.LogSession>()
            LogSessionScreen(
                circleId = route.circleId,
                // Both X (close) and a successful save return to the dashboard the same way.
                onCloseClick = { navController.popBackStack() },
                onSessionSaved = { navController.popBackStack() },
            )
        }

        composable<TallyRoute.HeadToHead> { backStackEntry ->
            val route = backStackEntry.toRoute<TallyRoute.HeadToHead>()
            HeadToHeadScreen(
                circleId = route.circleId,
                playerAId = route.playerAId,
                playerBId = route.playerBId,
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<TallyRoute.Settings> { entry ->
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onProfileClick = {
                    if (entry.lifecycleIsResumed()) navController.navigate(TallyRoute.Profile)
                },
                isDarkMode = isDarkMode,
                onThemeToggle = onThemeToggle,
                isNotificationsEnabled = notificationsEnabled,
                onNotificationsToggle = onNotificationsToggle,
                onManageCirclesClick = {
                    if (entry.lifecycleIsResumed()) navController.navigate(TallyRoute.ManageCircles)
                },
            )
        }

        composable<TallyRoute.Profile> {
            com.tally.app.ui.screens.ProfileScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        composable<TallyRoute.ManageCircles> {
            ManageCirclesScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
    }
    }
}

/**
 * True only when this destination is fully RESUMED — i.e. actually the visible foreground screen.
 * Guards against duplicate navigation from a double-tap fired before the screen leaves the foreground.
 */
private fun NavBackStackEntry.lifecycleIsResumed(): Boolean =
    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

/** Placeholder deterministic share code from the circle id — real codes come from Firestore later. */
private fun shareCodeFor(circleId: String): String {
    val h = circleId.hashCode().absoluteValue
    val code = buildString {
        var n = h
        repeat(4) { append('A' + n % 26); n /= 26 }
    }
    return "$code-${h % 90 + 10}"
}
