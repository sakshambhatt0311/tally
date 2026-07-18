package com.tally.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.H2HGameRecord
import com.tally.app.data.H2HMeeting
import com.tally.app.data.H2HOutcome
import com.tally.app.data.HeadToHeadRecord
import com.tally.app.data.RosterMember
import com.tally.app.data.ScoringType
import com.tally.app.data.TrackedGame
import com.tally.app.data.analytics.SessionAggregator
import com.tally.app.data.local.entity.SessionEntity
import com.tally.app.domain.repository.GameRepository
import com.tally.app.domain.repository.PlayerRepository
import com.tally.app.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Backs Head-to-Head. Holds the two chosen players (by name) and derives their rivalry purely from
 * the sessions they both played — nothing stored. The route's stub playerA/B ids aren't used;
 * selection defaults to the first two roster members and is driven by the in-screen pickers.
 */
@HiltViewModel
class HeadToHeadViewModel @Inject constructor(
    playerRepository: PlayerRepository,
    sessionRepository: SessionRepository,
    gameRepository: GameRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val circleId: String = checkNotNull(savedStateHandle["circleId"])

    // Chosen player names; null = "not yet chosen", resolved to a default in the aggregation.
    private val selectedA = MutableStateFlow<String?>(null)
    private val selectedB = MutableStateFlow<String?>(null)

    // null = first emission pending (avoids empty-state flash). Loaded roster used to build the pickers.
    val roster: StateFlow<List<RosterMember>?> = playerRepository.getPlayersForCircle(circleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // null while loading OR when the circle has fewer than two players (can't compare).
    val record: StateFlow<HeadToHeadRecord?> = combine(
        playerRepository.getPlayersForCircle(circleId),
        sessionRepository.getSessionsForCircle(circleId),
        gameRepository.getGamesForCircle(circleId),
        selectedA,
        selectedB,
    ) { players, sessions, games, aName, bName ->
        buildHeadToHead(circleId, players, sessions, games, aName, bName)
    }
        // Pairwise tally is CPU work — off the main thread.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectA(name: String) {
        selectedA.value = name
    }

    fun selectB(name: String) {
        selectedB.value = name
    }
}

/**
 * Resolve the two combatants (falling back to the first two members), then tally every session they
 * both played. Returns null if the circle has fewer than two players.
 */
private fun buildHeadToHead(
    circleId: String,
    players: List<RosterMember>,
    sessions: List<SessionEntity>,
    games: List<TrackedGame>,
    aName: String?,
    bName: String?,
): HeadToHeadRecord? {
    if (players.size < 2) return null
    val a = players.firstOrNull { it.name == aName } ?: players[0]
    val b = players.firstOrNull { it.name == bName && it.name != a.name }
        ?: players.first { it.name != a.name }

    val prefix = "$circleId:"
    val scoringByTemplate = games.associate { it.templateId to it.scoringType }
    val nameByTemplate = games.associate { it.templateId to it.displayName }

    // Sessions where BOTH players took part. getSessionsForCircle is already newest-first.
    val meetings = sessions
        .filter { session ->
            val uids = session.playerIds.map { it.removePrefix(prefix) }.toSet()
            a.id in uids && b.id in uids
        }
        .map { session ->
            val templateId = session.gameId.removePrefix(prefix)
            val scoring = scoringByTemplate[templateId]
            H2HMeeting(
                gameName = nameByTemplate[templateId] ?: templateId,
                playedAt = session.playedAt,
                outcome = outcomeBetween(scoring, session.resultsJson, a.id, b.id, session.playerIds.size, games.firstOrNull { it.templateId == templateId }?.isLowerScoreBetter == true),
            )
        }

    val aWins = meetings.count { it.outcome == H2HOutcome.A_WON }
    val bWins = meetings.count { it.outcome == H2HOutcome.B_WON }
    val ties = meetings.count { it.outcome == H2HOutcome.TIE }

    val byGame = meetings
        .groupBy { it.gameName }
        .map { (game, ms) ->
            val aw = ms.count { it.outcome == H2HOutcome.A_WON }
            val bw = ms.count { it.outcome == H2HOutcome.B_WON }
            H2HGameRecord(game, rivalrySummary(aw, bw, a.name, b.name))
        }

    return HeadToHeadRecord(a, b, aWins, ties, bWins, meetings, byGame)
}

/** Who placed higher between the two players in one session, per the game's scoring type. */
private fun outcomeBetween(scoring: ScoringType?, resultsJson: String, aId: String, bId: String, participantCount: Int, isLowerScoreBetter: Boolean): H2HOutcome =
    when (scoring) {
        ScoringType.PLACEMENT -> {
            val order = SessionAggregator.placementOrder(scoring, resultsJson)
            val ia = order.indexOf(aId)
            val ib = order.indexOf(bId)
            when {
                ia == -1 || ib == -1 -> H2HOutcome.TIE
                ia < ib -> H2HOutcome.A_WON   // earlier index = higher finish
                ib < ia -> H2HOutcome.B_WON
                else -> H2HOutcome.TIE
            }
        }
        ScoringType.POINTS -> {
            val points = SessionAggregator.pointsOf(scoring, resultsJson)
            val pa = points[aId] ?: 0
            val pb = points[bId] ?: 0
            when {
                pa == pb -> H2HOutcome.TIE
                isLowerScoreBetter -> if (pa < pb) H2HOutcome.A_WON else H2HOutcome.B_WON
                else -> if (pa > pb) H2HOutcome.A_WON else H2HOutcome.B_WON
            }
        }
        ScoringType.WIN_LOSS -> {
            val winners = SessionAggregator.winnersOf(scoring, resultsJson, participantCount, isLowerScoreBetter)
            when {
                aId in winners && bId in winners -> H2HOutcome.TIE // co-winners
                aId in winners -> H2HOutcome.A_WON
                bId in winners -> H2HOutcome.B_WON
                else -> H2HOutcome.TIE // neither won this one
            }
        }
        null -> H2HOutcome.TIE
    }

/** "3–1 Jordan" when someone leads, "2–2" on an even split. */
private fun rivalrySummary(aWins: Int, bWins: Int, aName: String, bName: String): String = when {
    aWins > bWins -> "$aWins–$bWins $aName"
    bWins > aWins -> "$bWins–$aWins $bName"
    else -> "$aWins–$bWins"
}
