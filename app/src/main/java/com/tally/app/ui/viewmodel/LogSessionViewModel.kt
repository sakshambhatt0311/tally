package com.tally.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.RosterMember
import com.tally.app.data.TrackedGame
import com.tally.app.data.local.entity.SessionEntity
import com.tally.app.di.ApplicationScope
import com.tally.app.domain.repository.CircleRepository
import com.tally.app.domain.repository.GameRepository
import com.tally.app.domain.repository.PlayerRepository
import com.tally.app.domain.repository.SessionRepository
import com.tally.app.ui.session.SessionResults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

/**
 * Backs the Log Session wizard — sources the circle's real roster for Step 2 and persists one
 * [SessionEntity] on Save. Session/game/player ids follow the deterministic scheme so FKs resolve.
 */
@HiltViewModel
class LogSessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val gameRepository: GameRepository,
    private val circleRepository: CircleRepository,
    playerRepository: PlayerRepository,
    @ApplicationScope private val appScope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val circleId: String = checkNotNull(savedStateHandle["circleId"])
    private val json = Json

    // null = first DB emission pending (gates the wizard's player step); non-null empty = no members.
    val roster: StateFlow<List<RosterMember>?> = playerRepository.getPlayersForCircle(circleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // One-shot "saved" signal for a single navigation pop. A Channel (not StateFlow) so it fires once
    // and is never re-delivered on recomposition.
    private val _sessionSaved = Channel<Unit>(Channel.BUFFERED)
    val sessionSaved: Flow<Unit> = _sessionSaved.receiveAsFlow()

    /**
     * Persist the finished session, then signal the screen to navigate back. Runs on [appScope]
     * because the wizard pops on save — a viewModelScope write would be cancelled mid-flight.
     */
    fun saveSession(game: TrackedGame, players: List<RosterMember>, results: SessionResults) {
        val session = SessionEntity(
            id = UUID.randomUUID().toString(),
            circleId = circleId,
            gameId = "$circleId:${game.templateId}",   // matches GameRepository's row id
            playedAt = System.currentTimeMillis(),
            playerIds = players.map { "$circleId:${it.id}" },
            resultsJson = encodeResults(results),
        )
        appScope.launch {
            val circle = circleRepository.getCircle(circleId).firstOrNull()
            val isOnline = circle?.isDeviceOnly == false
            
            if (!isOnline) {
                // Ensure the game is tracked in this circle first — Room enforces the session's gameId FK.
                gameRepository.upsertGame(circleId, game)
            }
            
            sessionRepository.addSession(session, isOnline)
            _sessionSaved.send(Unit)
        }
    }

    /** Serialize the per-mode outcome to a JSON string for the session's results column. */
    private fun encodeResults(results: SessionResults): String = when (results) {
        is SessionResults.Placement -> json.encodeToString(results.orderedPlayers.map { it.id })
        is SessionResults.Points -> json.encodeToString(results.pointsByPlayer)
        // Store all winners as a uid list; SessionAggregator credits each of them a win.
        is SessionResults.WinLoss -> json.encodeToString(results.winners.toList())
        SessionResults.None -> "{}"
    }
}
