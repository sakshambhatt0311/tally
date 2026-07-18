package com.tally.app.data

import androidx.compose.runtime.Immutable

// ---------------------------------------------------------------------------
// Head-to-Head — two players' rivalry, derived live from session history.
// ---------------------------------------------------------------------------

/** Who came out ahead in one meeting between the two compared players. */
enum class H2HOutcome { A_WON, B_WON, TIE }

/** One session both compared players took part in. */
@Immutable
data class H2HMeeting(
    val gameName: String,
    val playedAt: Long,
    val outcome: H2HOutcome,
)

/** Per-game tally within the rivalry — e.g. Catan "3–1 Jordan". */
@Immutable
data class H2HGameRecord(
    val gameName: String,
    val summary: String,
)

/** Full rivalry between two roster members, computed from the sessions they both played. */
@Immutable
data class HeadToHeadRecord(
    val playerA: RosterMember,
    val playerB: RosterMember,
    val aWins: Int,
    val ties: Int,
    val bWins: Int,
    /** Newest first. */
    val meetings: List<H2HMeeting>,
    val byGame: List<H2HGameRecord>,
) {
    val totalMeetings: Int get() = aWins + ties + bWins
}

// ---------------------------------------------------------------------------
// Per-game stats — one game's standings + record book, derived from its sessions.
// ---------------------------------------------------------------------------

/** One superlative in the Record Book (e.g. "Most wins" -> "Jordan (5)"). */
@Immutable
data class GameRecordEntry(
    val label: String,
    val holder: String,
)

/** Everything Game Detail shows, all derived from this game's session rows. */
@Immutable
data class GameStats(
    val sessionCount: Int,
    val scoringLabel: String,
    val standings: List<LeaderboardEntry>,
    val records: List<GameRecordEntry>,
)
