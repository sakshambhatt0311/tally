package com.tally.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.TrackedGame
import com.tally.app.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the Games tab — tracked games streamed from Room, scoped by the nav circleId. */
@HiltViewModel
class GamesViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val circleId: String = checkNotNull(savedStateHandle["circleId"])

    // null = first DB emission pending (avoids an empty-state flash); non-null empty = no games.
    // SharingStarted.Lazily: once started it stays warm for the whole Circle session, so switching
    // back to this tab replays the cached list instantly (never re-queries or flashes null).
    val games: StateFlow<List<TrackedGame>?> = gameRepository.getGamesForCircle(circleId)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    /**
     * Track a new game in this circle. The repo derives a stable row id from circleId + templateId, so
     * re-adding the same game upserts rather than duplicating. [games] re-emits on write.
     */
    fun addGame(game: TrackedGame) {
        viewModelScope.launch(Dispatchers.IO) {
            gameRepository.upsertGame(circleId, game)
        }
    }

    /** Un-track a game from this circle. [games] re-emits on write. */
    fun removeGame(game: TrackedGame) {
        viewModelScope.launch(Dispatchers.IO) {
            gameRepository.deleteGame(circleId, game)
        }
    }
}
