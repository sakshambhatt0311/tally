package com.tally.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.app.data.GameRecordEntry
import com.tally.app.data.GameTemplate
import com.tally.app.data.LeaderboardEntry
import com.tally.app.data.MockGameData
import com.tally.app.ui.components.LeaderboardRow
import com.tally.app.ui.components.toAvatarColor
import com.tally.app.ui.theme.PlusJakartaSans
import com.tally.app.ui.theme.StreakFlame
import com.tally.app.ui.viewmodel.GameDetailViewModel

/**
 * Game detail — matches the mockup: identity top bar (circle + game), a brand-tinted hero card,
 * the circle's standings for this game (Board-tab gold/silver/bronze rank logic), and a Record Book.
 * Stateless; standings/records are empty until the session-aggregation data layer lands.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    circleName: String,
    gameId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameDetailViewModel = hiltViewModel(),
) {
    val template = MockGameData.templateById(gameId)
    // Primary source: the live TrackedGame from Room (has the real displayName even when the
    // catalog entry is stale/missing after a rename or split). Catalog template is enrichment only.
    val trackedGame by viewModel.game.collectAsStateWithLifecycle()
    val gameName = template?.name ?: trackedGame?.displayName ?: "Game"
    // Live per-game analytics derived from this game's sessions. null = still loading.
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = circleName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = gameName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = PlusJakartaSans,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        val gameStats = stats
        when {
            // Loading — never flash the empty text while the first DB emission is pending.
            gameStats == null -> Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            else -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    HeroCard(
                        template = template,
                        gameName = gameName,
                        sessions = gameStats.sessionCount,
                        scoring = gameStats.scoringLabel,
                    )
                }

                item {
                    SectionHeader("Standings in $gameName")
                }
                if (gameStats.standings.isEmpty()) {
                    item { DetailEmptyText("No sessions logged for this game yet.") }
                } else {
                    itemsIndexedStandings(gameStats.standings)
                }

                item {
                    RecordBookCard(records = gameStats.records)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

/** Adds the standings rows to a LazyListScope with the shared Board-tab LeaderboardRow. */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedStandings(
    standings: List<LeaderboardEntry>,
) {
    // Rank is the list position — the aggregator already sorted by wins, then win%, then games.
    items(standings.size, key = { standings[it].player.name }) { index ->
        val entry = standings[index]
        LeaderboardRow(
            rank = index + 1,
            name = entry.player.name,
            statSummary = "Win rate ${entry.winPercentage}% · ${entry.wins} wins · ${entry.gamesPlayed} games",
            avatarInitial = entry.player.initial,
            avatarColor = entry.player.colorKey.toAvatarColor(),
            hasStreak = false, // streaks not modeled yet
            streakCount = 0,
            modifier = Modifier.padding(vertical = 6.dp),
            photoUrl = entry.player.photoUrl,
        )
    }
}

@Composable
private fun HeroCard(
    template: GameTemplate?,
    gameName: String,
    sessions: Int,
    scoring: String,
    modifier: Modifier = Modifier,
) {
    val description = template?.description.orEmpty()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(template?.brandColor ?: MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (template != null) {
                    Icon(
                        imageVector = template.icon,
                        contentDescription = null,
                        tint = Color.White, // icon on a colored medal/badge = white
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gameName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$sessions sessions · $scoring scoring",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PlusJakartaSans,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = PlusJakartaSans,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 4.dp),
    )
}

/** Styled empty line used inside the scrolling detail body (standings section). */
@Composable
private fun DetailEmptyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = PlusJakartaSans,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun RecordBookCard(records: List<GameRecordEntry>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Record book",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = PlusJakartaSans,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (records.isEmpty()) {
                Text(
                    text = "No records yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            records.forEach { entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = StreakFlame,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PlusJakartaSans,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = entry.holder,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
