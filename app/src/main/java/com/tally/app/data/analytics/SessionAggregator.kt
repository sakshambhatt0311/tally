package com.tally.app.data.analytics

import com.tally.app.data.LeaderboardEntry
import com.tally.app.data.RosterMember
import com.tally.app.data.ScoringType
import com.tally.app.data.TrackedGame
import com.tally.app.data.local.entity.SessionEntity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Pure, stateless aggregation over session history — the single place that turns [SessionEntity]
 * rows into stats. Every analytics ViewModel derives from here + the repositories, so no stat is
 * ever stored twice. Player identity is the deterministic id "$circleId:$name", stripped back to a
 * display name for matching against the roster.
 *
 * "Winning" is a [Set] — a session can have several winners (a points tie, or a co-op win-loss
 * result), and each of them is credited a win by the leaderboard.
 */
object SessionAggregator {

    private val json = Json { ignoreUnknownKeys = true }

    /** Decoded result of one session: who took part, who won (possibly several), and per-player points. */
    data class Outcome(
        val participants: Set<String>,
        val winners: Set<String>,
        val points: Map<String, Int>,
    )

    /** Decode each session (optionally narrowed to one game) into an [Outcome]. */
    fun outcomes(
        circleId: String,
        sessions: List<SessionEntity>,
        games: List<TrackedGame>,
        filterTemplateId: String? = null,
    ): List<Outcome> {
        val prefix = "$circleId:"
        val scoringByTemplate = games.associate { it.templateId to it.scoringType }
        val invertedByTemplate = games.associate { it.templateId to it.isLowerScoreBetter }
        return sessions
            .filter { filterTemplateId == null || it.gameId.removePrefix(prefix) == filterTemplateId }
            .map { session ->
                val templateId = session.gameId.removePrefix(prefix)
                val scoring = scoringByTemplate[templateId]
                val inverted = invertedByTemplate[templateId] == true
                val participants = session.playerIds.map { it.removePrefix(prefix) }.toSet()
                Outcome(participants, winnersOf(scoring, session.resultsJson, participants.size, inverted), pointsOf(scoring, session.resultsJson))
            }
    }

    /** Ranked standings: wins desc, then win% desc, then games desc. Players with no games are dropped. */
    fun leaderboard(
        circleId: String,
        players: List<RosterMember>,
        sessions: List<SessionEntity>,
        games: List<TrackedGame>,
        filterTemplateId: String? = null,
    ): List<LeaderboardEntry> {
        val outcomes = outcomes(circleId, sessions, games, filterTemplateId)
        return players
            .map { player ->
                val uid = player.id
                val played = outcomes.count { uid in it.participants }
                val wins = outcomes.count { uid in it.winners }   // credited to every (tied) winner
                val totalScore = outcomes.sumOf { it.points[uid] ?: 0 }
                LeaderboardEntry(
                    player = player,
                    gamesPlayed = played,
                    wins = wins,
                    winPercentage = if (played > 0) wins * 100 / played else 0,
                    totalScore = totalScore,
                )
            }
            .filter { it.gamesPlayed > 0 }
            .sortedWith(
                compareByDescending<LeaderboardEntry> { it.wins }
                    .thenByDescending { it.winPercentage }
                    .thenByDescending { it.gamesPlayed },
            )
    }

    /**
     * All winners of a session — a set so ties/co-wins are first-class. Mirrors
     * LogSessionViewModel.encodeResults; fails soft.
     *  - PLACEMENT: the 1st-placed player.
     *  - POINTS: every player tied at the top score (Standard Competition Ranking → joint 1st).
     *  - WIN_LOSS: every explicitly-selected winner (stored as a name list).
     */
    fun winnersOf(scoring: ScoringType?, resultsJson: String, participantCount: Int = 0, isLowerScoreBetter: Boolean = false): Set<String> =
        try {
            when (scoring) {
                ScoringType.PLACEMENT ->
                    json.decodeFromString<List<String>>(resultsJson).firstOrNull()?.let { setOf(it) } ?: emptySet()
                ScoringType.POINTS -> {
                    val points = pointsMap(resultsJson)
                    val actualCount = if (participantCount > 0) participantCount else points.size
                    val targetScore = if (isLowerScoreBetter) points.values.minOrNull() else points.values.maxOrNull()
                    if (targetScore == null) return emptySet()
                    val winners = points.filterValues { it == targetScore }.keys
                    if (actualCount == 2 && winners.size == 2) emptySet() else winners
                }
                ScoringType.WIN_LOSS -> {
                    val winners = json.decodeFromString<List<String>>(resultsJson).toSet()
                    val actualCount = if (participantCount > 0) participantCount else 2
                    if (actualCount == 2 && winners.size == 2) emptySet() else winners
                }
                null -> emptySet()
            }
        } catch (e: Exception) {
            emptySet()
        }

    /** Per-player points for a points game (empty for other modes). */
    fun pointsOf(scoring: ScoringType?, resultsJson: String): Map<String, Int> =
        if (scoring == ScoringType.POINTS) {
            try {
                pointsMap(resultsJson)
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }

    private fun pointsMap(resultsJson: String): Map<String, Int> =
        json.decodeFromString<Map<String, String>>(resultsJson).mapValues { it.value.toIntOrNull() ?: 0 }

    /**
     * Full finishing order for a session (placement games) for pairwise comparison. Empty for other modes.
     */
    fun placementOrder(scoring: ScoringType?, resultsJson: String): List<String> =
        try {
            if (scoring == ScoringType.PLACEMENT) json.decodeFromString<List<String>>(resultsJson) else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
}
