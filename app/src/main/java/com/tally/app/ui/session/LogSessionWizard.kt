package com.tally.app.ui.session

import  androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tally.app.data.GameTemplate
import com.tally.app.data.MembershipType
import com.tally.app.data.MockGameData
import com.tally.app.data.RosterMember
import com.tally.app.data.ScoringType
import com.tally.app.data.TrackedGame
import com.tally.app.ui.components.LocalPlayerChip
import com.tally.app.ui.components.PlayerAvatar
import com.tally.app.ui.components.TallyPrimaryButton
import com.tally.app.ui.components.toAvatarColor
import com.tally.app.ui.theme.PlusJakartaSans
import com.tally.app.ui.theme.TallyGreen
import com.tally.app.ui.theme.TallyPillShape
import com.tally.app.ui.theme.TallySurfaceVariant

/**
 * Stateless container for the 3-step logging flow. It renders whatever [state] describes and
 * routes every interaction back out through the callbacks — no business state lives here, so it
 * drops straight onto a ViewModel later (see [LogSessionScreen] for the current state owner).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSessionWizard(
    state: LogSessionState,
    availableGames: List<TrackedGame>,
    roster: List<RosterMember>,
    onGameSelected: (TrackedGame) -> Unit,
    onPlayerToggled: (RosterMember) -> Unit,
    onAddSomeoneNew: () -> Unit,
    onMovePlayer: (fromIndex: Int, toIndex: Int) -> Unit,
    onPointsChanged: (playerName: String, points: String) -> Unit,
    onWinnerToggled: (RosterMember) -> Unit,
    onWinnersSet: (Set<String>) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        // Same canvas as the top bar so there's no tonal/color seam under the title.
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { WizardTopBar(state = state, onBack = onBack, onClose = onClose) },
        bottomBar = { WizardBottomBar(state = state, onNext = onNext, onSave = onSave) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            StepProgressBar(currentStep = state.currentStep, total = TOTAL_STEPS)

            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { width -> direction * width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -direction * width } + fadeOut())
                },
                label = "wizardStep",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { step ->
                when (step) {
                    1 -> PickGameStep(
                        games = availableGames,
                        selectedGame = state.selectedGame,
                        onGameSelected = onGameSelected,
                    )
                    2 -> SelectPlayersStep(
                        roster = roster,
                        selectedPlayerIds = state.selectedPlayers.map { it.id }.toSet(),
                        requirementHint = state.playerRequirementHint,
                        onPlayerToggled = onPlayerToggled,
                        onAddSomeoneNew = onAddSomeoneNew,
                    )
                    else -> EnterResultsStep(
                        players = state.selectedPlayers,
                        results = state.results,
                        scoringUnit = state.selectedGame?.scoringUnit ?: "points",
                        isChess = state.isChess,
                        isBilliards = state.isBilliards,
                        onMovePlayer = onMovePlayer,
                        onPointsChanged = onPointsChanged,
                        onWinnerToggled = onWinnerToggled,
                        onWinnersSet = onWinnersSet,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Chrome — top bar, progress, bottom action
// ---------------------------------------------------------------------------

private fun stepTitle(step: Int): String = when (step) {
    1 -> "Pick a Game"
    2 -> "Who's Playing"
    else -> "Enter Results"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WizardTopBar(state: LogSessionState, onBack: () -> Unit, onClose: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "STEP ${state.currentStep} OF $TOTAL_STEPS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stepTitle(state.currentStep),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface, // was hardcoded near-black → invisible in dark
                )
            }
        },
        navigationIcon = {
            // Step 1 has no prior step, so its leading action closes the whole flow (matches the mockup's ×).
            if (state.isFirstStep) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            } else {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        // Transparent + no scroll tint = flat: the bar reads as one surface with the content, no seam.
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}

@Composable
private fun StepProgressBar(currentStep: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(TallyPillShape)
                    .background(if (index < currentStep) TallyGreen else TallySurfaceVariant),
            )
        }
    }
}

@Composable
private fun WizardBottomBar(state: LogSessionState, onNext: () -> Unit, onSave: () -> Unit) {
    // Transparent so no white block/border prints under the button in dark (was hardcoded TallySurface).
    Surface(color = Color.Transparent) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            if (state.isLastStep) {
                TallyPrimaryButton(text = "Save Session", onClick = onSave, enabled = state.canSave)
            } else {
                TallyPrimaryButton(text = "Next", onClick = onNext, enabled = state.canAdvance)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// STEP 1 — Pick a Game
// ---------------------------------------------------------------------------

@Composable
private fun PickGameStep(
    games: List<TrackedGame>,
    selectedGame: TrackedGame?,
    onGameSelected: (TrackedGame) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Selection tracked by template name locally.
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }

    // Filter the SSOT by name; drop any category left empty so its header never renders.
    val filteredCategories = if (query.isBlank()) {
        MockGameData.allGamesCategorized
    } else {
        MockGameData.allGamesCategorized
            .mapValues { (_, list) -> list.filter { it.name.contains(query.trim(), ignoreCase = true) } }
            .filterValues { it.isNotEmpty() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        GameSearchField(
            query = query,
            onQueryChange = { query = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp), // clear the pinned Next button
        ) {
            filteredCategories.forEach { (category, list) ->
                item(key = "header_$category") { PickGameCategoryHeader(category) }
                items(list, key = { it.name }) { game ->
                    SelectableGameRow(
                        game = game,
                        isSelected = selectedName == game.name,
                        onClick = {
                            selectedName = game.name
                            // Hand back a TrackedGame carrying the picked template's real identity.
                            onGameSelected(TrackedGame(
                                templateId = game.id, 
                                displayName = game.name, 
                                scoringType = game.scoringType, 
                                scoringUnit = game.scoringUnit,
                                isLowerScoreBetter = game.isLowerScoreBetter
                            ))
                        },
                    )
                }
            }
        }
    }
}

/** Flat search field: 0-elevation, rounded, surfaceVariant fill, no visible border (matches Add Game). */
@Composable
private fun GameSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        placeholder = {
            Text(
                text = "Search games",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = PlusJakartaSans,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun PickGameCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface, // normal body color — NOT green
        modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 8.dp),
    )
}

/** Sleek selectable row: colorful icon, name, and a check when picked. */
@Composable
private fun SelectableGameRow(
    game: GameTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = game.icon,
                contentDescription = null,
                tint = game.brandColor, // colorful
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = game.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (isSelected) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            } else {
                Spacer(Modifier.size(24.dp)) // reserve the trailing slot so rows never shift
            }
        }
    }
}

// ---------------------------------------------------------------------------
// STEP 2 — Who's Playing
// ---------------------------------------------------------------------------

@Composable
private fun SelectPlayersStep(
    roster: List<RosterMember>,
    selectedPlayerIds: Set<String>,
    requirementHint: String?,
    onPlayerToggled: (RosterMember) -> Unit,
    onAddSomeoneNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp), // clear the pinned Next button
    ) {
        if (requirementHint != null) {
            item {
                // Player-count rule (Chess / racket sports). Next stays disabled via state.canAdvance.
                Text(
                    text = requirementHint,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                )
            }
        }
        items(roster, key = { it.id }) { member ->
            PlayerSelectRow(
                member = member,
                selected = selectedPlayerIds.contains(member.id),
                onToggle = { onPlayerToggled(member) },
            )
        }
        item {
            TextButton(
                onClick = onAddSomeoneNew,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Add someone new",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Sleek selectable roster row mirroring Step 1's SelectableGameRow: avatar, name, trailing check. */
@Composable
private fun PlayerSelectRow(
    member: RosterMember,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onToggle,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerAvatar(initial = member.initial, backgroundColor = member.colorKey.toAvatarColor())
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (member.membershipType == MembershipType.LOCAL) {
                    Spacer(Modifier.height(4.dp))
                    LocalPlayerChip()
                }
            }
            Spacer(Modifier.weight(1f))
            SelectionIndicator(selected = selected)
        }
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean, modifier: Modifier = Modifier) {
    if (selected) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = "Selected",
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(24.dp),
        )
    } else {
        Box(
            modifier = modifier
                .size(22.dp)
                .border(width = 1.5.dp, color = MaterialTheme.colorScheme.outline, shape = CircleShape),
        )
    }
}

// ---------------------------------------------------------------------------
// STEP 3 — Enter Results (adapts to the game's scoring type)
// ---------------------------------------------------------------------------

@Composable
private fun EnterResultsStep(
    players: List<RosterMember>,
    results: SessionResults,
    scoringUnit: String,
    isChess: Boolean,
    isBilliards: Boolean,
    onMovePlayer: (Int, Int) -> Unit,
    onPointsChanged: (String, String) -> Unit,
    onWinnerToggled: (RosterMember) -> Unit,
    onWinnersSet: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (results) {
        is SessionResults.Placement -> {
            // Resolve the finishing order against the live roster (selectedPlayers) so names always
            // load and no selected player is dropped, even if the seeded order went stale.
            val ordered = remember(results.orderedPlayers, players) {
                val byName = players.associateBy { it.name }
                val resolved = results.orderedPlayers.mapNotNull { byName[it.name] }
                resolved + players.filter { p -> resolved.none { it.name == p.name } }
            }
            PlacementResults(ordered, onMovePlayer, modifier)
        }
        is SessionResults.Points -> PointsResults(players, results.pointsByPlayer, scoringUnit, onPointsChanged, modifier)
        // Chess gets its own 1v1 win/draw picker. Billiards is single-select (tap replaces the winner).
        // Every other win/loss game keeps the multi-select "Who won?" grid (Pool, FM, CoD/Halo, …).
        is SessionResults.WinLoss -> when {
            isChess && players.size == 2 ->
                ChessResults(players[0], players[1], results.winners, onWinnersSet, modifier)
            isBilliards ->
                WinLossResults(players, results.winners, onPlayerClick = { onWinnersSet(setOf(it.id)) }, modifier = modifier)
            else ->
                WinLossResults(players, results.winners, onPlayerClick = onWinnerToggled, modifier = modifier)
        }
        SessionResults.None -> Box(modifier.fillMaxSize())
    }
}

/**
 * Chess: exactly three exclusive, chunky choices — Player A, Player B, or Draw. Each writes the
 * winners set directly (A -> {A}, B -> {B}, Draw -> {A, B}); the draw uses the multi-winner tie
 * logic so both are Placement 1.
 */
@Composable
private fun ChessResults(
    playerA: RosterMember,
    playerB: RosterMember,
    winners: Set<String>,
    onWinnersSet: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Who won?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        ChessOptionCard(
            player = playerA,
            label = playerA.name,
            selected = winners == setOf(playerA.id),
            onClick = { onWinnersSet(setOf(playerA.id)) },
        )
        ChessOptionCard(
            player = playerB,
            label = playerB.name,
            selected = winners == setOf(playerB.id),
            onClick = { onWinnersSet(setOf(playerB.id)) },
        )
        ChessOptionCard(
            player = null,
            label = "Draw / Stalemate",
            selected = winners == setOf(playerA.id, playerB.id),
            onClick = { onWinnersSet(setOf(playerA.id, playerB.id)) },
        )
    }
}

/** Chunky full-width choice card for the Chess step — avatar (or handshake for a draw) + label + check. */
@Composable
private fun ChessOptionCard(
    player: RosterMember?,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (player != null) {
                PlayerAvatar(initial = player.initial, backgroundColor = player.colorKey.toAvatarColor(), size = 40.dp)
            } else {
                // Draw: neutral handshake tile instead of a player avatar.
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Handshake,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            PlayerNameLabel(label)
            if (selected) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            } else {
                Spacer(Modifier.size(24.dp))
            }
        }
    }
}

/**
 * Placement: a long-press drag-to-reorder ranked list. A local working copy keeps the drag smooth
 * while each committed swap is mirrored out through [onMovePlayer] so the wizard state stays in sync.
 */
@Composable
private fun PlacementResults(
    ordered: List<RosterMember>,
    onMovePlayer: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val rowSpacing = 10.dp
    val spacingPx = with(density) { rowSpacing.toPx() }

    // Local order drives rendering so a reorder mid-drag never fights the incoming state. Reseed
    // only when the *membership* changes (re-entering the step) — never during a live reorder.
    val items = remember { mutableStateListOf<RosterMember>().apply { addAll(ordered) } }
    LaunchedEffect(ordered) {
        if (items.map { it.name }.toSet() != ordered.map { it.name }.toSet()) {
            items.clear()
            items.addAll(ordered)
        }
    }

    var draggedName by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Drag to set finishing order",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = PlusJakartaSans,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(rowSpacing)) {
            items.forEachIndexed { index, player ->
                // key() keeps each row's node (and its live drag gesture) attached to the player as
                // the list reorders, so a single long-press drag stays continuous across swaps.
                key(player.name) {
                    val dragging = draggedName == player.name
                    PlacementRow(
                        rank = placementLabel(index),
                        player = player,
                        modifier = Modifier
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer { translationY = if (dragging) dragOffsetY else 0f }
                            .onSizeChanged { rowHeightPx = it.height.toFloat() }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedName = player.name
                                        dragOffsetY = 0f
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragEnd = { draggedName = null; dragOffsetY = 0f },
                                    onDragCancel = { draggedName = null; dragOffsetY = 0f },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        dragOffsetY += drag.y
                                        val cur = items.indexOfFirst { it.name == player.name }
                                        val step = rowHeightPx + spacingPx
                                        if (cur < 0 || step <= 0f) return@detectDragGesturesAfterLongPress
                                        when {
                                            dragOffsetY > step / 2f && cur < items.lastIndex -> {
                                                items.add(cur + 1, items.removeAt(cur))
                                                onMovePlayer(cur, cur + 1)
                                                dragOffsetY -= step // row jumped down one slot; keep it under the finger
                                            }
                                            dragOffsetY < -step / 2f && cur > 0 -> {
                                                items.add(cur - 1, items.removeAt(cur))
                                                onMovePlayer(cur, cur - 1)
                                                dragOffsetY += step
                                            }
                                        }
                                    },
                                )
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlacementRow(
    rank: String,
    player: RosterMember,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp), // chunky global component sizing
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = rank,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = PlusJakartaSans,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 40.dp),
            )
            PlayerAvatar(initial = player.initial, backgroundColor = player.colorKey.toAvatarColor(), size = 32.dp)
            Text(
                text = player.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.DragIndicator,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Muted section caption shared by the Step 3 result views. */
@Composable
private fun ResultsCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontFamily = PlusJakartaSans,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Premium flat result card: 0-elevation surface, rounded, hairline outline. */
@Composable
private fun ResultRowCard(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Player name pulled straight from the live roster row so it can never render blank. */
@Composable
private fun RowScope.PlayerNameLabel(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f),
    )
}

/** Points: a number field beside each player, each row in a premium card. */
@Composable
private fun PointsResults(
    players: List<RosterMember>,
    pointsByPlayer: Map<String, String>,
    scoringUnit: String,
    onPointsChanged: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Dynamic unit — "points" for most games, "goals" for FIFA, etc.
        item { ResultsCaption("Enter each player's $scoringUnit") }
        items(players, key = { it.name }) { player ->
            ResultRowCard {
                PlayerAvatar(initial = player.initial, backgroundColor = player.colorKey.toAvatarColor(), size = 32.dp)
                PlayerNameLabel(player.name)
                OutlinedTextField(
                    // State keeps points as a String; bridge to Int here. 0 renders blank so the
                    // placeholder reads through — no stray leading zero.
                    value = (pointsByPlayer[player.id].orEmpty().toIntOrNull() ?: 0).let { points ->
                        if (points == 0) "" else points.toString()
                    },
                    onValueChange = { input ->
                        val points = input.filter(Char::isDigit).toIntOrNull() ?: 0
                        onPointsChanged(player.id, points.toString())
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaSans,
                        textAlign = TextAlign.Center,
                    ),
                    placeholder = {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = PlusJakartaSans,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.width(88.dp),
                )
            }
        }
    }
}

/**
 * Win/loss list. [onPlayerClick] decides the interaction: multi-select (toggle) for team-win games,
 * or single-select (replace) for strict 1v1 games like Billiards. Selection reads from [winners].
 */
@Composable
private fun WinLossResults(
    players: List<RosterMember>,
    winners: Set<String>,
    onPlayerClick: (RosterMember) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            // Full-size section header, matching the app's other titles — not the tiny caption.
            Text(
                text = "Who won?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
        }
        items(players, key = { it.id }) { player ->
            val selected = player.id in winners
            Surface(
                onClick = { onPlayerClick(player) }, // toggle or replace, per the caller
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 72.dp), // chunky, large tap target
                shape = RoundedCornerShape(16.dp),
                // Flat 0-elevation; selection reads purely from the container color shift.
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PlayerAvatar(initial = player.initial, backgroundColor = player.colorKey.toAvatarColor(), size = 40.dp)
                    PlayerNameLabel(player.name)
                    if (selected) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Winner", tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Spacer(Modifier.size(24.dp)) // reserve the trailing slot so rows never shift
                    }
                }
            }
        }
    }
}

/** 0-based index → "1st", "2nd", "3rd", "4th", … */
private fun placementLabel(index: Int): String {
    val n = index + 1
    val suffix = when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}
