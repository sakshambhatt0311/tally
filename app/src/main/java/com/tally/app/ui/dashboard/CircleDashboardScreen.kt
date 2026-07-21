package com.tally.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.app.ui.theme.PlusJakartaSans
import com.tally.app.ui.viewmodel.FeedViewModel
import com.tally.app.ui.viewmodel.GamesViewModel
import com.tally.app.ui.viewmodel.LeaderboardViewModel
import com.tally.app.ui.viewmodel.MembersViewModel

/** The four tabs. Click-only switching (no swipe); each tab's scroll is preserved by the state holder. */
private enum class DashboardTab(val label: String) {
    Feed("Feed"),
    Board("Board"),
    Games("Games"),
    Members("Members"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDashboardScreen(
    circleId: String,
    circleName: String,
    onBackClick: () -> Unit,
    onLogSessionClick: () -> Unit,
    onViewHeadToHead: (playerAId: String, playerBId: String) -> Unit,
    onGameClick: (gameId: String) -> Unit = {},
    initialTabIndex: Int = 0,
) {
    val tabs = DashboardTab.entries
    // Seed from the nav arg (e.g. "Add players" opens Members), then user taps take over.
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(initialTabIndex.coerceIn(tabs.indices)) }
    // Preserves each tab's LazyColumn scroll across instant swaps, with no Pager overhead.
    val stateHolder = rememberSaveableStateHolder()

    // All four tab ViewModels are scoped to THIS Circle route (hiltViewModel here = the CircleDashboard
    // NavBackStackEntry as ViewModelStoreOwner). Hoisting them to the parent — instead of each tab
    // composable creating its own — guarantees a single instance survives every tab switch, so their
    // StateFlows (now SharingStarted.Lazily) keep the latest Room emission cached in memory for the
    // whole time the user is inside the Circle. Returning to a tab replays instantly, never null.
    val feedViewModel: FeedViewModel = hiltViewModel()
    val boardViewModel: LeaderboardViewModel = hiltViewModel()
    val gamesViewModel: GamesViewModel = hiltViewModel()
    val membersViewModel: MembersViewModel = hiltViewModel()

    // Shared with FeedTab so the FAB and the cards agree on edit mode.
    val feedEditMode by feedViewModel.isEditMode.collectAsStateWithLifecycle()
    val feedSelectedIds by feedViewModel.selectedSessionIds.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = circleName,
                        style = MaterialTheme.typography.headlineMedium, // ExtraBold via type scale
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
        floatingActionButton = {
            // Vertically stacked → never clips on narrow screens.
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (tabs[selectedTabIndex]) {
                    DashboardTab.Feed -> if (feedEditMode) {
                        // Edit mode: chunky destructive delete + cancel replace the normal actions.
                        ExtendedFloatingActionButton(
                            text = {
                                Text(
                                    "Delete (${feedSelectedIds.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                            onClick = { if (feedSelectedIds.isNotEmpty()) feedViewModel.deleteSelected() },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        )
                        ExtendedFloatingActionButton(
                            text = { Text("Cancel", style = MaterialTheme.typography.labelLarge) },
                            icon = { Icon(Icons.Rounded.Close, contentDescription = null) },
                            onClick = { feedViewModel.exitEditMode() },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        )
                    } else {
                        // "Edit Sessions" sits directly above "Log Session", in the Head-to-Head color.
                        EditSessionsFab(onClick = { feedViewModel.toggleEditMode() })
                        LogSessionFab(onLogSessionClick)
                    }
                    DashboardTab.Board -> {
                        // Board keeps only Head-to-Head; logging a session lives on the Feed tab.
                        ExtendedFloatingActionButton(
                            text = { Text("Head-to-Head", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans) },
                            icon = { Icon(Icons.Filled.CompareArrows, contentDescription = null) },
                            onClick = { onViewHeadToHead("jordan", "alex") }, // stub pair until selection UI
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        )
                    }
                    DashboardTab.Members -> Unit // Add-player FAB is hosted inside MembersTab now.
                    DashboardTab.Games -> Unit // "Add a Game" is an inline pill button inside GamesTab.
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent, // flat — no color bleed
                contentColor = MaterialTheme.colorScheme.onSurface,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    // Snaps directly to the selected tab (no swipe fraction math).
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ) {
                tabs.forEachIndexed { index, tab ->
                    DashboardTabLabel(
                        label = tab.label,
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Instant swap, no gesture layer. SaveableStateProvider keyed per tab keeps each
                // list's scroll position so re-entering a tab is immediate.
                stateHolder.SaveableStateProvider(tabs[selectedTabIndex].name) {
                    when (selectedTabIndex) {
                        0 -> FeedTab(viewModel = feedViewModel)
                        1 -> BoardTab(viewModel = boardViewModel)
                        2 -> GamesTab(onGameClick = onGameClick, viewModel = gamesViewModel)
                        else -> MembersTab(viewModel = membersViewModel)
                    }
                }
            }
        }
    }
}

/** Ripple-free tab label — `indication = null` kills the Material splash so switching feels crisp. */
@Composable
private fun DashboardTabLabel(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * "Edit Sessions" pill — same flat pill shape as Log Session, in the exact Head-to-Head color
 * (secondaryContainer / onSecondaryContainer). Enters the Feed's multi-select delete mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSessionsFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        text = { Text("Edit Sessions", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans) },
        icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
        onClick = onClick,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
    )
}

/** Primary "Log Session" pill — solid brand green, pill-shaped, flat. Shared by Feed and Board. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogSessionFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        text = { Text("Log Session", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans) },
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        onClick = onClick,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
    )
}
