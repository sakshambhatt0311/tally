package com.tally.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.LeaderboardEntry
import com.tally.app.data.TrackedGame
import com.tally.app.data.analytics.SessionAggregator
import com.tally.app.domain.repository.GameRepository
import com.tally.app.domain.repository.PlayerRepository
import com.tally.app.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Backs the Board tab. Standings are computed live by combining the circle's roster, its sessions,
 * and its games (games supply each session's [ScoringType], which decides what "a win" means).
 * Nothing is persisted — the aggregation is a pure function of the session history, so the board
 * always matches the Feed. [selectedTemplateId] null = "All Games", else filters to one game.
 */
@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    playerRepository: PlayerRepository,
    sessionRepository: SessionRepository,
    gameRepository: GameRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val circleId: String = checkNotNull(savedStateHandle["circleId"])

    private val selectedTemplateId = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = selectedTemplateId.asStateFlow()

    /** The circle's tracked games with at least one session — drives the filter chips. */
    val games: StateFlow<List<TrackedGame>?> = gameRepository.getGamesForCircle(circleId)
        .map { games -> games.filter { it.sessionCount > 0 } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // null = first emission pending (avoids an empty-state flash); non-null empty = no sessions to rank.
    // The computed standings are paired with the filter they were computed for, so the UI always has a
    // consistent (filter, rows) snapshot — the filter changes in lock-step with the rows, never a frame
    // ahead. That lets the Board list remount cleanly on a game switch instead of scroll-anchoring to
    // the previous game's rows during the recompute gap.
    val standings: StateFlow<BoardStandings?> = combine(
        playerRepository.getPlayersForCircle(circleId),
        sessionRepository.getSessionsForCircle(circleId),
        gameRepository.getGamesForCircle(circleId),
        selectedTemplateId,
    ) { players, sessions, games, filterId ->
        // Single source of truth for ranking (ties/multi-winner aware) lives in SessionAggregator.
        BoardStandings(filterId, SessionAggregator.leaderboard(circleId, players, sessions, games, filterId))
    }
        // Aggregation is CPU work — keep it off the main thread so list rendering never stutters.
        .flowOn(Dispatchers.Default)
        // Lazily: the computed standings stay cached for the whole Circle session (instant tab return).
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun selectFilter(templateId: String?) {
        selectedTemplateId.value = templateId
    }
}

/** A consistent Board snapshot: the ranked [entries] together with the [filterTemplateId] they were
 *  computed for (null = "All Games"). Pairing them keeps the rendered filter and rows in lock-step. */
data class BoardStandings(
    val filterTemplateId: String?,
    val entries: List<LeaderboardEntry>,
)
