package com.tally.app.data

import androidx.compose.runtime.Immutable

/**
 * One logged session, ready for the Feed timeline. Derived from a persisted session joined to its
 * game (see SessionWithGame) — [templateId] resolves the icon/brand via the catalog, [gameName]/
 * [participantNames]/[recap] are the human-readable summary, [playedAt] drives the relative time.
 */
@Immutable
data class SessionSummary(
    val id: String,
    val templateId: String,
    val gameName: String,
    val scoringType: ScoringType,
    val playedAt: Long,
    val participantNames: List<String>,
    val recap: String,
)
