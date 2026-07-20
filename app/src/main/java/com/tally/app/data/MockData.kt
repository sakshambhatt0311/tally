package com.tally.app.data

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/** Stable identity for each game a circle can track — mapped to a shape + brand color in the UI layer. */
enum class GameId { CATAN, POKER, SMASH, UNO }

/** Stable identity for each player's fixed color across the whole app (Jordan is always blue, etc.). */
enum class PlayerColorKey { BLUE, CORAL, PURPLE, TEAL, YELLOW }

enum class ScoringType { PLACEMENT, POINTS, WIN_LOSS }

fun ScoringType.label(): String = when (this) {
    ScoringType.PLACEMENT -> "placement"
    ScoringType.POINTS -> "points"
    ScoringType.WIN_LOSS -> "win/loss"
}

enum class MembershipType { OWNER, LINKED, LOCAL }

/** One row on the "My Circles" landing screen. */
@IgnoreExtraProperties
@Immutable
data class Circle(
    val id: String,
    val name: String,
    val creatorId: String = "",
    val creatorEmail: String? = null,
    val memberIds: List<String> = emptyList(),
    @get:Exclude val members: List<Pair<String, PlayerColorKey>> = emptyList(),
    val memberCount: Int = 0,
    val activityLabel: String = "",
    /** OWNER = you created it (or it's device-only), LINKED = you joined someone else's. */
    val membershipType: MembershipType = MembershipType.OWNER,
    val isDeviceOnly: Boolean = false,
    val inviteCode: String = "",
    val lastSessionAt: Long? = null,
)

/** One card in the circle Feed tab's session timeline. */
@Immutable
data class FeedItem(
    val id: String,
    val gameId: GameId,
    val isSynced: Boolean,
    val participantNames: List<String>,
    val recap: String,
    val relativeTime: String,
)

/** One row on the Board tab's leaderboard. */
@Immutable
data class LeaderboardPlayer(
    val rank: Int,
    val name: String,
    val initial: String,
    val colorKey: PlayerColorKey,
    val winRatePercent: Int,
    val gamesPlayed: Int,
    val hasStreak: Boolean = false,
    val streakCount: Int = 0,
)

/**
 * One game a circle tracks. Identity now comes from the [GameCatalog] template (unlimited games),
 * not the 4-value [GameId] — [templateId] preserves the real game, [displayName] its label.
 */
@Immutable
data class TrackedGame(
    val templateId: String,
    val displayName: String,
    val scoringType: ScoringType,
    val sessionCount: Int = 0,
    /** Score noun for this game (e.g. "goals" for FIFA), resolved from its catalog template. */
    val scoringUnit: String = "points",
    val isLowerScoreBetter: Boolean = false,
)

/**
 * Human scoring descriptor for a game: the [scoringUnit] for point-scored games ("goals", "points"),
 * otherwise the scoring-type label ("placement", "win/loss"). Keeps unit logic out of the UI layer.
 */
fun TrackedGame.scoringSummary(): String = when (scoringType) {
    ScoringType.POINTS -> scoringUnit
    ScoringType.PLACEMENT -> "placement"
    ScoringType.WIN_LOSS -> "win/loss"
}

/** One row on the Members tab's roster. */
@Immutable
data class RosterMember(
    val id: String = "",
    val name: String,
    val initial: String,
    val colorKey: PlayerColorKey,
    val membershipType: MembershipType,
    val photoUrl: String? = null
)

/** A single rival on the Head-to-Head screen. */
@Immutable
data class HeadToHeadPlayer(
    val name: String,
    val initial: String,
    val colorKey: PlayerColorKey,
)

/** One game's record within a Head-to-Head rivalry — e.g. Catan, "4–1 Jordan". */
@Immutable
data class GameRecord(
    val gameId: GameId,
    val summary: String,
)

/** Full rivalry comparison between two members, shown on the Head-to-Head screen. */
@Immutable
data class HeadToHeadStats(
    val playerA: HeadToHeadPlayer,
    val playerB: HeadToHeadPlayer,
    val playerAWins: Int,
    val ties: Int,
    val playerBWins: Int,
    /** true = playerA won that meeting, false = playerB — oldest to newest, left to right. */
    val lastMeetings: List<Boolean>,
    val byGame: List<GameRecord>,
)

// ---------------------------------------------------------------------------
// Mock data — mirrors the exact names/numbers shown in the mockup so the
// static screens in Step 4 render pixel-for-pixel what the reference shows.
// ---------------------------------------------------------------------------

val mockCircles = listOf(
    Circle(
        id = "circle-1",
        name = "Common Room Crew",
        members = listOf(
            "J" to PlayerColorKey.BLUE,
            "A" to PlayerColorKey.CORAL,
            "P" to PlayerColorKey.PURPLE,
            "S" to PlayerColorKey.TEAL,
            "M" to PlayerColorKey.YELLOW,
        ),
        memberCount = 5,
        activityLabel = "Last night · Catan · 2h ago",
        membershipType = MembershipType.OWNER,
    ),
    Circle(
        id = "circle-2",
        name = "Poker Sundays",
        members = emptyList(),
        memberCount = 3,
        activityLabel = "3 days ago",
        membershipType = MembershipType.LINKED,
    ),
    Circle(
        id = "circle-3",
        name = "Solo Practice",
        members = emptyList(),
        memberCount = 1,
        activityLabel = "yesterday",
        membershipType = MembershipType.OWNER,
        isDeviceOnly = true,
    ),
)

val mockFeedItems = listOf(
    FeedItem(
        id = "feed-1",
        gameId = GameId.CATAN,
        isSynced = true,
        participantNames = listOf("Jordan", "Alex", "Priya", "Sam"),
        recap = "Jordan clinched a come-from-behind Catan win, snapping Alex's 4-game streak.",
        relativeTime = "Today · 9:42 PM",
    ),
    FeedItem(
        id = "feed-2",
        gameId = GameId.POKER,
        isSynced = true,
        participantNames = listOf("Priya", "Jordan", "Alex"),
        recap = "Priya's on a 3-game poker streak, nobody's cracked her yet.",
        relativeTime = "Yesterday",
    ),
    FeedItem(
        id = "feed-3",
        gameId = GameId.SMASH,
        isSynced = true,
        participantNames = listOf("Maya", "Sam"),
        recap = "Maya swept Sam 3–0. Not close.",
        relativeTime = "3 days ago",
    ),
)

val mockLeaderboard = listOf(
    LeaderboardPlayer(
        rank = 1,
        name = "Jordan",
        initial = "J",
        colorKey = PlayerColorKey.BLUE,
        winRatePercent = 62,
        gamesPlayed = 8,
        hasStreak = true,
        streakCount = 4,
    ),
    LeaderboardPlayer(rank = 2, name = "Alex", initial = "A", colorKey = PlayerColorKey.CORAL, winRatePercent = 45, gamesPlayed = 11),
    LeaderboardPlayer(rank = 3, name = "Priya", initial = "P", colorKey = PlayerColorKey.PURPLE, winRatePercent = 40, gamesPlayed = 5),
    LeaderboardPlayer(rank = 4, name = "Sam", initial = "S", colorKey = PlayerColorKey.TEAL, winRatePercent = 33, gamesPlayed = 6),
)

/** Per-game standings so the Board filter chips actually re-rank the list (real aggregates land with Room). */
val mockLeaderboardByFilter: Map<String, List<LeaderboardPlayer>> = mapOf(
    "All Games" to mockLeaderboard,
    "Catan" to listOf(
        LeaderboardPlayer(1, "Priya", "P", PlayerColorKey.PURPLE, winRatePercent = 71, gamesPlayed = 7, hasStreak = true, streakCount = 3),
        LeaderboardPlayer(2, "Jordan", "J", PlayerColorKey.BLUE, winRatePercent = 58, gamesPlayed = 12),
        LeaderboardPlayer(3, "Sam", "S", PlayerColorKey.TEAL, winRatePercent = 42, gamesPlayed = 5),
        LeaderboardPlayer(4, "Alex", "A", PlayerColorKey.CORAL, winRatePercent = 30, gamesPlayed = 10),
    ),
    "Poker" to listOf(
        LeaderboardPlayer(1, "Priya", "P", PlayerColorKey.PURPLE, winRatePercent = 66, gamesPlayed = 9, hasStreak = true, streakCount = 3),
        LeaderboardPlayer(2, "Alex", "A", PlayerColorKey.CORAL, winRatePercent = 50, gamesPlayed = 8),
        LeaderboardPlayer(3, "Jordan", "J", PlayerColorKey.BLUE, winRatePercent = 44, gamesPlayed = 6),
    ),
    "Smash" to listOf(
        LeaderboardPlayer(1, "Maya", "M", PlayerColorKey.YELLOW, winRatePercent = 80, gamesPlayed = 5, hasStreak = true, streakCount = 5),
        LeaderboardPlayer(2, "Jordan", "J", PlayerColorKey.BLUE, winRatePercent = 55, gamesPlayed = 6),
        LeaderboardPlayer(3, "Sam", "S", PlayerColorKey.TEAL, winRatePercent = 25, gamesPlayed = 4),
    ),
)

val mockRoster = listOf(
    RosterMember(id = "player-1", name = "Jordan", initial = "J", colorKey = PlayerColorKey.BLUE, membershipType = MembershipType.OWNER),
    RosterMember(id = "player-2", name = "Alex", initial = "A", colorKey = PlayerColorKey.CORAL, membershipType = MembershipType.LINKED),
    RosterMember(id = "player-3", name = "Priya", initial = "P", colorKey = PlayerColorKey.PURPLE, membershipType = MembershipType.LOCAL),
    RosterMember(id = "player-4", name = "Sam", initial = "S", colorKey = PlayerColorKey.TEAL, membershipType = MembershipType.LOCAL),
    RosterMember(id = "player-5", name = "Maya", initial = "M", colorKey = PlayerColorKey.YELLOW, membershipType = MembershipType.LINKED),
)

val mockHeadToHead = HeadToHeadStats(
    playerA = HeadToHeadPlayer("Jordan", "J", PlayerColorKey.BLUE),
    playerB = HeadToHeadPlayer("Alex", "A", PlayerColorKey.CORAL),
    playerAWins = 6,
    ties = 1,
    playerBWins = 3,
    lastMeetings = listOf(true, false, true, true, false),
    byGame = listOf(
        GameRecord(GameId.CATAN, "4–1 Jordan"),
        GameRecord(GameId.POKER, "2–2"),
    ),
)
