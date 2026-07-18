package com.tally.app.data

import androidx.compose.runtime.Immutable

/**
 * One ranked row on the Board tab, derived live from session history (never stored). Stats are
 * aggregated per circle from [com.tally.app.data.local.entity.SessionEntity] rows so the board can
 * never drift from the Feed — the session data is the single source of truth.
 */
@Immutable
data class LeaderboardEntry(
    val player: RosterMember,
    val gamesPlayed: Int,
    val wins: Int,
    /** Whole-number win rate, 0..100. */
    val winPercentage: Int,
    /** Sum of points across points-scored games (0 for placement/win-loss games). */
    val totalScore: Int,
)
