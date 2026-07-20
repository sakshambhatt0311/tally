package com.tally.app.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.app.ui.viewmodel.FeedViewModel
import com.tally.app.ui.viewmodel.GamesViewModel
import com.tally.app.ui.viewmodel.LeaderboardViewModel
import com.tally.app.ui.viewmodel.MembersViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tally.app.data.MembershipType
import com.tally.app.data.MockGameData
import com.tally.app.data.RosterMember
import com.tally.app.data.SessionSummary
import com.tally.app.data.TrackedGame
import com.tally.app.data.scoringSummary
import com.tally.app.ui.components.LeaderboardRow
import com.tally.app.ui.components.PlayerAvatar
import com.tally.app.ui.components.toAvatarColor
import com.tally.app.ui.session.AddPlayerBottomSheet
import com.tally.app.ui.session.AddPlayerSheetState
import com.tally.app.ui.theme.PlusJakartaSans
import com.tally.app.ui.theme.TallyPillShape
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// FEED — the shared timeline every circle member sees once a session syncs.
// ---------------------------------------------------------------------------

@Composable
fun FeedTab(
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedSessionIds.collectAsStateWithLifecycle()

    // System back leaves edit mode instead of the dashboard.
    BackHandler(enabled = isEditMode) { viewModel.exitEditMode() }

    val list = feed
    // Crossfade the load->data transition so the list eases in instead of snapping over the frame.
    Crossfade(targetState = loadPhase(list), animationSpec = tween(300), label = "feedPhase", modifier = modifier.fillMaxSize()) { phase ->
        when (phase) {
            LoadPhase.Loading -> LoadingBox(Modifier.fillMaxSize())
            LoadPhase.Empty -> TabEmptyState(
                title = "No activity yet",
                subtitle = "Play a game to get started!",
                modifier = Modifier.fillMaxSize(),
            )
            LoadPhase.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Extra bottom pad so the last card clears the stacked FABs while editing.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(list.orEmpty(), key = { it.id }, contentType = { "feedCard" }) { item ->
                    FeedCard(
                        item = item,
                        isEditMode = isEditMode,
                        selected = item.id in selectedIds,
                        onToggleSelect = { viewModel.toggleSelection(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedCard(
    item: SessionSummary,
    isEditMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Resolve the catalog template for the real icon/brand color; the name is stored on the session.
    val template = MockGameData.templateById(item.templateId)
    Card(
        modifier = modifier
            .fillMaxWidth()
            // Only tappable while editing — selects/deselects this session.
            .then(if (isEditMode) Modifier.clickable { onToggleSelect() } else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        // Chunky selected state: thick brand-primary border (flat, 0 elevation).
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Identical icon size + 16dp gap as the Games tab, vertically centered.
                Icon(
                    imageVector = template?.icon ?: Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = template?.brandColor ?: MaterialTheme.colorScheme.onSurface, // colorful brand tint
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = item.gameName,
                    style = MaterialTheme.typography.titleLarge, // larger, Bold via type scale
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = item.participantNames.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Quote block: thin left rule + upright recap (no italics), distinguished by indentation.
            Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Text(
                    // Dynamic, game-specific flavor line from FeedMessageGenerator — the fun bit.
                    text = "“${item.recap}”",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = relativeTime(item.playedAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val feedTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
private val feedDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
private val feedDateYearFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

/** Compact relative timestamp for a Feed card. */
private fun relativeTime(playedAt: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = playedAt }
    val date = Date(playedAt)
    val timeString = feedTimeFormat.format(date).lowercase(Locale.getDefault())
    return when {
        isSameDay(now, then) -> "Today · $timeString"
        isYesterday(now, then) -> "Yesterday · $timeString"
        else -> {
            val isSameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
            val dateString = if (isSameYear) feedDateFormat.format(date) else feedDateYearFormat.format(date)
            "$dateString · $timeString"
        }
    }
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun isYesterday(now: Calendar, other: Calendar): Boolean {
    val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return isSameDay(yesterday, other)
}

// ---------------------------------------------------------------------------
// BOARD — derived leaderboard, filterable by game.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardTab(
    modifier: Modifier = Modifier,
    viewModel: LeaderboardViewModel = hiltViewModel(),
) {
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val games by viewModel.games.collectAsStateWithLifecycle()
    val selected by viewModel.selectedFilter.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        // Filter chips built from the circle's real tracked games; "All Games" clears the filter.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "all") {
                BoardFilterChip("All Games", selected == null) { viewModel.selectFilter(null) }
            }
            items(games.orEmpty(), key = { it.templateId }) { game ->
                BoardFilterChip(game.displayName, selected == game.templateId) {
                    viewModel.selectFilter(game.templateId)
                }
            }
        }

        val list = leaderboard
        Crossfade(
            targetState = loadPhase(list),
            animationSpec = tween(300),
            label = "boardPhase",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { phase ->
            when (phase) {
                LoadPhase.Loading -> LoadingBox(Modifier.fillMaxSize())
                LoadPhase.Empty -> TabEmptyState(
                    title = "The board is clear",
                    subtitle = "Log a session to claim the top spot!",
                    modifier = Modifier.fillMaxSize(),
                )
                LoadPhase.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Rank is the list position — the VM already sorted by wins, then win%, then games.
                    itemsIndexed(
                        list.orEmpty(),
                        key = { _, entry -> entry.player.name },
                        contentType = { _, _ -> "leaderboardRow" },
                    ) { index, entry ->
                        val rank = index + 1
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            shape = RoundedCornerShape(24.dp),
                            // Low-alpha podium tint glows over the current theme surface — legible in both modes.
                            colors = CardDefaults.cardColors(containerColor = podiumContainerColor(rank)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            LeaderboardRow(
                                rank = rank,
                                name = entry.player.name,
                                statSummary = "Win rate ${entry.winPercentage}% · ${entry.gamesPlayed} games",
                                avatarInitial = entry.player.initial,
                                avatarColor = entry.player.colorKey.toAvatarColor(),
                                hasStreak = false, // streaks land in Step 4 (deep analytics)
                                streakCount = 0,
                                modifier = Modifier.padding(16.dp),
                                photoUrl = entry.player.photoUrl,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Flat filter pill: selected = secondary container, unselected = transparent + thin outline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoardFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = TallyPillShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = Color.Transparent,
        ),
        elevation = FilterChipDefaults.filterChipElevation(elevation = 0.dp),
    )
}

/** Subtle Gold/Silver/Bronze wash for the top 3, standard surface below. Low alpha keeps text legible. */
@Composable
private fun podiumContainerColor(rank: Int): Color = when (rank) {
    1 -> Color(0xFFFFD700).copy(alpha = 0.12f)
    2 -> Color(0xFFC0C0C0).copy(alpha = 0.12f)
    3 -> Color(0xFFCD7F32).copy(alpha = 0.14f)
    else -> MaterialTheme.colorScheme.surface
}

// ---------------------------------------------------------------------------
// GAMES — every game this circle actually tracks.
// ---------------------------------------------------------------------------

@Composable
fun GamesTab(
    onAddGameClick: () -> Unit = {},
    onGameClick: (gameId: String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: GamesViewModel = hiltViewModel(),
) {
    val games by viewModel.games.collectAsStateWithLifecycle()
    Box(modifier = modifier.fillMaxSize()) {
        val list = games
        Crossfade(targetState = loadPhase(list), animationSpec = tween(300), label = "gamesPhase", modifier = Modifier.fillMaxSize()) { phase ->
            when (phase) {
                LoadPhase.Loading -> LoadingBox(Modifier.fillMaxSize())
                LoadPhase.Empty -> TabEmptyState(
                    title = "No games yet",
                    subtitle = "Tap Add a Game to start tracking sessions.",
                    modifier = Modifier.fillMaxSize(),
                )
                LoadPhase.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(list.orEmpty(), key = { it.templateId }, contentType = { "gameCard" }) { game ->
                        GameCard(game = game, onClick = { onGameClick(game.templateId) })
                    }
                }
            }
        }
        // Floating, bottom-right — identical structure to the Board tab's "+ Log Session" FAB.
        ExtendedFloatingActionButton(
            onClick = onAddGameClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add a Game", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) },
        )
    }
}

/** Feed-style isolated card: flat white surface, 24dp corners, monochrome glyph + chevron. */
@Composable
private fun GameCard(game: TrackedGame, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Resolve the catalog template for the real icon/brand color; displayName is stored on the game.
    val template = MockGameData.templateById(game.templateId)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = template?.icon ?: Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = template?.brandColor ?: MaterialTheme.colorScheme.onSurface, // colorful brand tint
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    text = game.displayName,
                    style = MaterialTheme.typography.titleLarge, // exact match to FeedCard game name
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${game.sessionCount} sessions · ${game.scoringSummary()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// MEMBERS — roster, divided by account type.
// ---------------------------------------------------------------------------

@Composable
fun MembersTab(
    modifier: Modifier = Modifier,
    viewModel: MembersViewModel = hiltViewModel(),
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val isDeviceOnly by viewModel.isDeviceOnly.collectAsStateWithLifecycle()
    val inviteCode by viewModel.inviteCode.collectAsStateWithLifecycle()
    val isCurrentUserOwner by viewModel.isCurrentUserOwner.collectAsStateWithLifecycle()

    var showSheet by rememberSaveable { mutableStateOf(false) }
    var sheetState by remember { mutableStateOf(AddPlayerSheetState()) }

    var isRemoveMode by rememberSaveable { mutableStateOf(false) }
    var selectedMembers by remember { mutableStateOf(emptySet<String>()) }
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    fun exitRemoveMode() {
        isRemoveMode = false
        selectedMembers = emptySet()
    }

    // System back exits selection instead of leaving the dashboard.
    BackHandler(enabled = isRemoveMode) { exitRemoveMode() }

    Box(modifier = modifier.fillMaxSize()) {
        val roster = members
        Crossfade(targetState = loadPhase(roster), animationSpec = tween(300), label = "membersPhase", modifier = Modifier.fillMaxSize()) { phase ->
            when (phase) {
                LoadPhase.Loading -> LoadingBox(Modifier.fillMaxSize())
                LoadPhase.Empty -> TabEmptyState(
                    title = "No members yet",
                    subtitle = "Tap Add Member to invite someone to this circle.",
                    modifier = Modifier.fillMaxSize(),
                )
                LoadPhase.Content -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(roster.orEmpty(), key = { it.name }, contentType = { "memberCard" }) { member ->
                        val key = member.id.ifBlank { member.name }
                        val isSelectable = isRemoveMode && member.membershipType != MembershipType.OWNER
                        MemberCard(
                            member = member,
                            isRemoveMode = isSelectable,
                            selected = key in selectedMembers,
                            onClick = {
                                selectedMembers = if (key in selectedMembers) {
                                    selectedMembers - key
                                } else {
                                    selectedMembers + key
                                }
                            },
                        )
                    }
                }
            }
        }

        if (isRemoveMode) {
            // Selection action bar replaces the FABs.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { exitRemoveMode() },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = TallyPillShape,
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                }
                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = selectedMembers.isNotEmpty(),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = TallyPillShape,
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Remove (${selectedMembers.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans)
                }
            }
        } else if (members != null) {
            // Stacked FABs: Remove (owner only) above Add. Hidden while the roster is still loading.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isCurrentUserOwner) {
                    ExtendedFloatingActionButton(
                        onClick = { isRemoveMode = true },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        icon = { Icon(Icons.Rounded.PersonRemove, contentDescription = null, modifier = Modifier.scale(scaleX = -1f, scaleY = 1f)) },
                        text = { Text("Remove Member", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans) },
                    )
                }
                ExtendedFloatingActionButton(
                    onClick = { showSheet = true },
                    shape = CircleShape, // pill, exactly matching the Log Session FAB
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
                    text = { Text("Add Member", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans) },
                )
            }
        }
    }

    if (showConfirmDialog) {
        val count = selectedMembers.size
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Remove Members",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                )
            },
            text = {
                Text(
                    text = "Remove ${if (count == 1) "this member" else "$count members"} from this circle? This can't be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PlusJakartaSans,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeMembers(selectedMembers)
                    showConfirmDialog = false
                    exitRemoveMode()
                }) {
                    Text("Remove", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, fontFamily = PlusJakartaSans, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    if (showSheet) {
        AddPlayerBottomSheet(
            state = sheetState.copy(shareCode = inviteCode),
            onTabSelected = { sheetState = sheetState.copy(selectedTab = it) },
            onNameChange = { sheetState = sheetState.copy(nameInput = it) },
            onAddPlayerClicked = {
                val name = sheetState.nameInput.trim()
                if (name.isNotEmpty()) {
                    if (isDeviceOnly) {
                        viewModel.addMember(name) // persists to Room; roster Flow re-emits
                    } else {
                        viewModel.onAddGuestClicked(name, isOnline = true)
                    }
                    sheetState = sheetState.copy(
                        nameInput = "",
                        recentlyAdded = sheetState.recentlyAdded + name,
                    )
                }
            },
            onRemoveRecent = { sheetState = sheetState.copy(recentlyAdded = sheetState.recentlyAdded - it) },
            onCopyCode = { /* clipboard write lands with the platform layer */ },
            onDismiss = { showSheet = false },
            isOnline = !isDeviceOnly,
        )
    }
}

/** Feed-style member card: flat surface, 24dp corners, avatar + name + role chip (or select ring). */
@Composable
private fun MemberCard(
    member: RosterMember,
    modifier: Modifier = Modifier,
    isRemoveMode: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val containerColor = if (isRemoveMode && selected) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Surface(
        onClick = onClick,
        enabled = isRemoveMode, // only tappable while selecting
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlayerAvatar(initial = member.initial, backgroundColor = member.colorKey.toAvatarColor(), photoUrl = member.photoUrl)
            Text(
                text = member.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                color = MaterialTheme.colorScheme.onSurface, // fixes the dark-mode invisible-text bug
                modifier = Modifier.weight(1f),
            )
            if (isRemoveMode) {
                MemberSelectionIndicator(selected = selected)
            } else {
                when (member.membershipType) {
                    MembershipType.OWNER -> RoleChip(
                        text = "Owner",
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        content = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MembershipType.LOCAL -> RoleChip(
                        text = "Local player",
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    MembershipType.LINKED -> Unit // a real linked account needs no extra tag
                }
            }
        }
    }
}

@Composable
private fun MemberSelectionIndicator(selected: Boolean, modifier: Modifier = Modifier) {
    if (selected) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = "Selected",
            tint = MaterialTheme.colorScheme.error,
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

@Composable
private fun RoleChip(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Shared tab states — loading spinner + styled empty prompt.
// ---------------------------------------------------------------------------

/** The three render phases of a data-backed tab; used as a stable Crossfade key. */
private enum class LoadPhase { Loading, Empty, Content }

/** null list = still loading (never the empty state); empty list = genuinely empty; else content. */
private fun loadPhase(list: List<*>?): LoadPhase = when {
    list == null -> LoadPhase.Loading
    list.isEmpty() -> LoadPhase.Empty
    else -> LoadPhase.Content
}

/** Subtle centered spinner shown while a tab's first DB emission is still loading. */
@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** Clean, styled empty state — shown when a tab loaded successfully but has no rows. */
@Composable
private fun TabEmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = PlusJakartaSans,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
