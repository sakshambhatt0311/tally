package com.tally.app.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.app.data.MembershipType
import com.tally.app.data.PlayerColorKey
import com.tally.app.data.RosterMember
import com.tally.app.ui.viewmodel.LogSessionViewModel

/**
 * Stateful host for the log-a-session flow. Owns the wizard's mutable [LogSessionState] and turns the
 * stateless [LogSessionWizard]/[AddPlayerBottomSheet] callbacks into pure transitions on it.
 *
 * The roster comes from Room via [LogSessionViewModel] (scoped to the nav circleId — [circleId] itself
 * is consumed by the VM's SavedStateHandle). On Save the wizard state is handed to the VM, which
 * persists the session and fires a one-shot signal we turn into [onSessionSaved].
 */
@Composable
fun LogSessionScreen(
    circleId: String,
    onCloseClick: () -> Unit,
    onSessionSaved: () -> Unit,
    viewModel: LogSessionViewModel = hiltViewModel(),
) {
    val roster by viewModel.roster.collectAsStateWithLifecycle()

    // Fire the nav-pop exactly once, when the DB insert has committed.
    LaunchedEffect(Unit) {
        viewModel.sessionSaved.collect { onSessionSaved() }
    }

    // Gate the wizard until the roster's first DB emission — avoids a flash of an empty player step.
    val members = roster
    if (members == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var state by remember { mutableStateOf(LogSessionState()) }
    // Local working roster seeds from the real members; ad-hoc guests added in the sheet append here.
    var localRoster by remember { mutableStateOf(members) }

    var showAddPlayer by remember { mutableStateOf(false) }
    // Static share code for now; the real one arrives with the circle from Room/Firestore.
    var sheetState by remember { mutableStateOf(AddPlayerSheetState(shareCode = "GLNC-42")) }

    // Intercept system back/swipe on steps 2 & 3 → walk the wizard back instead of popping the
    // whole flow off the nav stack. Step 1 stays unhandled so back closes as usual.
    BackHandler(enabled = state.currentStep > 1) { state = state.goBack() }

    LogSessionWizard(
        state = state,
        availableGames = emptyList(), // Step 1 picks from the full catalog; the game is tracked on save.
        roster = localRoster,
        onGameSelected = { state = state.selectGame(it) },
        onPlayerToggled = { state = state.togglePlayer(it) },
        onAddSomeoneNew = { showAddPlayer = true },
        onMovePlayer = { from, to -> state = state.movePlayer(from, to) },
        onPointsChanged = { name, points -> state = state.setPoints(name, points) },
        onWinnerToggled = { state = state.toggleWinner(it) },
        onWinnersSet = { state = state.setWinners(it) },
        onBack = { state = state.goBack() },
        onNext = { state = state.advance() },
        onSave = {
            state.selectedGame?.let { game ->
                viewModel.saveSession(game, state.selectedPlayers, state.results)
            }
        },
        onClose = onCloseClick,
    )

    if (showAddPlayer) {
        AddPlayerBottomSheet(
            state = sheetState,
            onTabSelected = { sheetState = sheetState.copy(selectedTab = it) },
            onNameChange = { sheetState = sheetState.copy(nameInput = it) },
            onAddPlayerClicked = {
                val name = sheetState.nameInput.trim()
                if (name.isNotEmpty()) {
                    val newMember = RosterMember(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name,
                        initial = name.take(1).uppercase(),
                        colorKey = nextAvatarColor(localRoster.size),
                        membershipType = MembershipType.LOCAL,
                    )
                    localRoster = localRoster + newMember
                    state = state.togglePlayer(newMember) // auto-select the just-added guest
                    sheetState = sheetState.copy(
                        nameInput = "",
                        recentlyAdded = sheetState.recentlyAdded + name,
                    )
                }
            },
            onRemoveRecent = { name ->
                sheetState = sheetState.copy(recentlyAdded = sheetState.recentlyAdded - name)
                localRoster = localRoster.filterNot { it.name == name }
                state = state.copy(selectedPlayers = state.selectedPlayers.filterNot { it.name == name })
            },
            onCopyCode = { /* clipboard write lands with the platform/ViewModel layer */ },
            onDismiss = { showAddPlayer = false },
        )
    }
}

/** Cycles the fixed avatar palette so each newly added local player still gets a stable color. */
private fun nextAvatarColor(index: Int): PlayerColorKey {
    val palette = PlayerColorKey.entries
    return palette[index % palette.size]
}
