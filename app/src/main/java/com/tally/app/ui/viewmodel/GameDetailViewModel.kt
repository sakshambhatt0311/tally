package com.tally.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.GameRecordEntry
import com.tally.app.data.GameStats
import com.tally.app.data.RosterMember
import com.tally.app.data.ScoringType
import com.tally.app.data.TrackedGame
import com.tally.app.data.analytics.SessionAggregator
import com.tally.app.data.local.entity.SessionEntity
import com.tally.app.data.scoringSummary
import com.tally.app.domain.repository.GameRepository
import com.tally.app.domain.repository.PlayerRepository
import com.tally.app.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs Game Detail — the tracked game, its circle roster, and its sessions, all live from Room. */
@HiltViewModel
class GameDetailViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    playerRepository: PlayerRepository,
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Nav args from GameDetail(circleId, circleName, gameId).
    private val circleId: String = checkNotNull(savedStateHandle["circleId"])
    private val gameId: String = checkNotNull(savedStateHandle["gameId"])

    val game: StateFlow<TrackedGame?> = gameRepository.getGamesForCircle(circleId)
        .map { games -> games.firstOrNull { it.templateId == gameId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val players: StateFlow<List<RosterMember>> = playerRepository.getPlayersForCircle(circleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Session rows key gameId as "$circleId:$templateId"; the nav arg is the bare templateId, so
    // rebuild the full row id here — querying the bare id matched nothing.
    private val gameRowId: String = "$circleId:$gameId"

    val sessions: StateFlow<List<SessionEntity>> = sessionRepository.getSessionsForGame(gameRowId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Live per-game analytics: standings + record book, derived from this game's sessions. null =
     * first emission pending; non-null with sessionCount 0 = the game has no logged sessions yet.
     */
    val stats: StateFlow<GameStats?> = combine(
        gameRepository.getGamesForCircle(circleId),
        playerRepository.getPlayersForCircle(circleId),
        sessionRepository.getSessionsForGame(gameRowId),
    ) { games, players, sessions ->
        val trackedGame = games.firstOrNull { it.templateId == gameId } ?: return@combine null
        buildGameStats(circleId, trackedGame, players, sessions)
    }
        // Standings + record aggregation is CPU work — off the main thread.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Edit a game's name/type (upsert on same id = update). [game] re-emits on write. */
    fun editGame(updated: TrackedGame) {
        viewModelScope.launch(Dispatchers.IO) {
            gameRepository.upsertGame(circleId, updated)
        }
    }

    /** Delete this game. FK CASCADE removes its sessions too; the Flows re-emit automatically. */
    fun deleteGame(game: TrackedGame) {
        viewModelScope.launch(Dispatchers.IO) {
            gameRepository.deleteGame(circleId, game)
        }
    }

    /**
     * Delete one historical session. Stats (win%, avg) are derived on-the-fly from the session
     * list, so removing a row recomputes them via the reactive [sessions] emission — no manual
     * stat rewrite needed.
     */
    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            sessionRepository.deleteSession(session)
        }
    }
}

/** Assemble one game's standings + record-book superlatives from its sessions. */
private fun buildGameStats(
    circleId: String,
    game: TrackedGame,
    players: List<RosterMember>,
    sessions: List<SessionEntity>,
): GameStats {
    val standings = SessionAggregator.leaderboard(circleId, players, sessions, listOf(game), game.templateId)
    val outcomes = SessionAggregator.outcomes(circleId, sessions, listOf(game), game.templateId)
    return GameStats(
        sessionCount = sessions.size,
        scoringLabel = game.scoringSummary(),
        standings = standings,
        records = buildRecords(game, standings, outcomes),
    )
}

private fun buildRecords(
    game: TrackedGame,
    standings: List<com.tally.app.data.LeaderboardEntry>,
    outcomes: List<SessionAggregator.Outcome>,
): List<GameRecordEntry> {
    if (outcomes.isEmpty()) return emptyList()
    val records = mutableListOf<GameRecordEntry>()
    // Most wins — standings are already sorted wins-desc.
    standings.firstOrNull { it.wins > 0 }?.let {
        records += GameRecordEntry("Most wins", "${it.player.name} (${it.wins})")
    }
    // Most games played.
    standings.maxByOrNull { it.gamesPlayed }?.let {
        records += GameRecordEntry("Games played", "${it.player.name} (${it.gamesPlayed})")
    }
    if (game.scoringType == ScoringType.POINTS) {
        outcomes.flatMap { it.points.entries }
            .maxByOrNull { it.value }
            ?.takeIf { it.value > 0 }
            ?.let { entry -> 
                val playerName = standings.find { it.player.id == entry.key }?.player?.name ?: entry.key
                records += GameRecordEntry("Highest score", "$playerName (${entry.value} ${game.scoringUnit})") 
            }
    }
    return records
}
