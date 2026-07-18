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
    val leaderboard: StateFlow<List<LeaderboardEntry>?> = combine(
        playerRepository.getPlayersForCircle(circleId),
        sessionRepository.getSessionsForCircle(circleId),
        gameRepository.getGamesForCircle(circleId),
        selectedTemplateId,
    ) { players, sessions, games, filterId ->
        // Single source of truth for ranking (ties/multi-winner aware) lives in SessionAggregator.
        SessionAggregator.leaderboard(circleId, players, sessions, games, filterId)
    }
        // Aggregation is CPU work — keep it off the main thread so list rendering never stutters.
        .flowOn(Dispatchers.Default)
        // Lazily: the computed standings stay cached for the whole Circle session (instant tab return).
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun selectFilter(templateId: String?) {
        selectedTemplateId.value = templateId
    }
}
