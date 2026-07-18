package com.tally.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.app.data.H2HGameRecord
import com.tally.app.data.H2HMeeting
import com.tally.app.data.H2HOutcome
import com.tally.app.data.HeadToHeadRecord
import com.tally.app.data.RosterMember
import com.tally.app.ui.components.PlayerAvatar
import com.tally.app.ui.components.toAvatarColor
import com.tally.app.ui.viewmodel.HeadToHeadViewModel

private const val SIDE_PADDING = 24

/**
 * Rivalry comparison between exactly two members. Both players and their record are derived live from
 * the sessions they both played (see [HeadToHeadViewModel]). The route's playerA/B ids are stubs and
 * unused — selection is seeded to the first two members and driven by the in-screen pickers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeadToHeadScreen(
    circleId: String,
    playerAId: String,
    playerBId: String,
    onBackClick: () -> Unit,
    viewModel: HeadToHeadViewModel = hiltViewModel(),
) {
    val roster by viewModel.roster.collectAsStateWithLifecycle()
    val record by viewModel.record.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Head-to-Head",
                        style = MaterialTheme.typography.headlineMedium,
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
    ) { innerPadding ->
        val members = roster
        val rivalry = record
        when {
            // Loading — never flash the empty text while the first DB emission is pending.
            members == null -> LoadingBox(Modifier.padding(innerPadding).fillMaxSize())
            members.size < 2 -> CenteredMessage(
                title = "Not enough players",
                subtitle = "Add at least two players to this circle to compare them.",
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
            )
            rivalry == null -> LoadingBox(Modifier.padding(innerPadding).fillMaxSize())
            else -> HeadToHeadContent(
                record = rivalry,
                roster = members,
                onSelectA = viewModel::selectA,
                onSelectB = viewModel::selectB,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun HeadToHeadContent(
    record: HeadToHeadRecord,
    roster: List<RosterMember>,
    onSelectA: (String) -> Unit,
    onSelectB: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectingSlot by remember { mutableStateOf<Int?>(null) }
    val playerA = record.playerA
    val playerB = record.playerB

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SIDE_PADDING.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Spacer(Modifier.height(32.dp))

        RivalryHeader(
            playerA = playerA,
            playerB = playerB,
            onPickA = { selectingSlot = 0 },
            onPickB = { selectingSlot = 1 },
        )

        if (record.totalMeetings == 0) {
            // Both players resolve, but they've never played a session together.
            CenteredMessage(
                title = "No matches found between these players.",
                subtitle = "Log a session with both of them to start their rivalry.",
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                WinRatioBar(
                    colorA = playerA.colorKey.toAvatarColor(),
                    colorB = playerB.colorKey.toAvatarColor(),
                    winsA = record.aWins,
                    ties = record.ties,
                    winsB = record.bWins,
                )
                RivalrySummaryCard(
                    nameA = playerA.name,
                    winsA = record.aWins,
                    ties = record.ties,
                    winsB = record.bWins,
                    nameB = playerB.name,
                )
            }

            LastMeetingsRow(meetings = record.meetings, playerA = playerA, playerB = playerB)
            ByGameBreakdown(record.byGame)
        }
    }

    if (selectingSlot != null) {
        // Exclude the other slot's player so a mirror match can never be picked.
        val available = when (selectingSlot) {
            0 -> roster.filter { it.name != playerB.name }
            else -> roster.filter { it.name != playerA.name }
        }
        PlayerPickerSheet(
            roster = available,
            onDismiss = { selectingSlot = null },
            onSelect = { member ->
                if (selectingSlot == 0) onSelectA(member.name) else onSelectB(member.name)
                selectingSlot = null
            },
        )
    }
}

/** Stacked proportion bar: player A | ties | player B, widths weighted by their counts. */
@Composable
private fun WinRatioBar(
    colorA: Color,
    colorB: Color,
    winsA: Int,
    ties: Int,
    winsB: Int,
    modifier: Modifier = Modifier,
) {
    val total = winsA + ties + winsB
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(50)),
    ) {
        if (total == 0) {
            Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant))
        } else {
            if (winsA > 0) Box(Modifier.weight(winsA.toFloat()).fillMaxHeight().background(colorA))
            if (ties > 0) Box(Modifier.weight(ties.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant))
            if (winsB > 0) Box(Modifier.weight(winsB.toFloat()).fillMaxHeight().background(colorB))
        }
    }
}

@Composable
private fun RivalryHeader(
    playerA: RosterMember,
    playerB: RosterMember,
    onPickA: () -> Unit,
    onPickB: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Equal-weight columns act as fixed anchors: a long name truncates within its own half.
        SelectablePlayer(player = playerA, onClick = onPickA, modifier = Modifier.weight(1f))
        Text(
            text = "VS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.wrapContentWidth(),
        )
        SelectablePlayer(player = playerB, onClick = onPickB, modifier = Modifier.weight(1f))
    }
}

/** Avatar + name + dropdown arrow, dead-centered, ripple-free clickable to open the picker. */
@Composable
private fun SelectablePlayer(
    player: RosterMember,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlayerAvatar(initial = player.initial, backgroundColor = player.colorKey.toAvatarColor(), size = 56.dp, photoUrl = player.photoUrl)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "Change player",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RivalrySummaryCard(
    nameA: String,
    winsA: Int,
    ties: Int,
    winsB: Int,
    nameB: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SummaryStat(value = winsA.toString(), label = "$nameA wins")
            SummaryStat(value = ties.toString(), label = "Ties")
            SummaryStat(value = winsB.toString(), label = "$nameB wins")
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LastMeetingsRow(
    meetings: List<H2HMeeting>,
    playerA: RosterMember,
    playerB: RosterMember,
    modifier: Modifier = Modifier,
) {
    // Newest first already; show up to the last 5.
    val recent = meetings.take(5)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Last ${recent.size} meetings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        LazyRow(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            items(recent) { meeting ->
                val winner = when (meeting.outcome) {
                    H2HOutcome.A_WON -> playerA
                    H2HOutcome.B_WON -> playerB
                    H2HOutcome.TIE -> null
                }
                MeetingMiniCard(gameName = meeting.gameName, winner = winner)
            }
        }
    }
}

/** Compact card: game name on top, winner avatar (or a tie dash) in the middle, label below. */
@Composable
private fun MeetingMiniCard(
    gameName: String,
    winner: RosterMember?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = gameName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (winner != null) {
                PlayerAvatar(initial = winner.initial, backgroundColor = winner.colorKey.toAvatarColor(), size = 32.dp, photoUrl = winner.photoUrl)
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("–", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = winner?.name ?: "Tie",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ByGameBreakdown(records: List<H2HGameRecord>, modifier: Modifier = Modifier) {
    if (records.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "By game",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            records.forEach { record -> ByGameRow(record) }
        }
    }
}

/** Sleek flat card per game: name on the left, score in an accent pill on the right. */
@Composable
private fun ByGameRow(record: H2HGameRecord, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = record.gameName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = record.summary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerPickerSheet(
    roster: List<RosterMember>,
    onDismiss: () -> Unit,
    onSelect: (RosterMember) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Select player",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            roster.forEach { member ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onSelect(member) })
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PlayerAvatar(initial = member.initial, backgroundColor = member.colorKey.toAvatarColor(), photoUrl = member.photoUrl)
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** Subtle centered spinner shown while the first DB emission is loading. */
@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** Styled centered empty/hint message. */
@Composable
private fun CenteredMessage(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = SIDE_PADDING.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
