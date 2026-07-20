package com.tally.app.data.mapper

import com.tally.app.data.Circle
import com.tally.app.data.FeedMessageGenerator
import com.tally.app.data.MockGameData
import com.tally.app.data.PlayerColorKey
import com.tally.app.data.RosterMember
import com.tally.app.data.ScoringType
import com.tally.app.data.SessionSummary
import com.tally.app.data.TrackedGame
import com.tally.app.data.analytics.SessionAggregator
import com.tally.app.data.local.entity.CircleEntity
import com.tally.app.data.local.entity.GameEntity
import com.tally.app.data.local.entity.PlayerEntity
import com.tally.app.data.local.relation.CircleWithMemberCount
import com.tally.app.data.local.relation.GameWithSessionCount
import com.tally.app.data.local.relation.SessionWithGame
import java.util.UUID

// ---------------------------------------------------------------------------
// Circle  <->  CircleEntity
// ---------------------------------------------------------------------------

/**
 * [members] are stored in the separate `players` table, so the entity can't rebuild them alone —
 * the repository passes the joined roster in. Defaults to empty for a bare circle read.
 */
fun CircleEntity.toDomain(members: List<Pair<String, PlayerColorKey>> = emptyList()): Circle =
    Circle(
        id = id,
        name = name,
        creatorEmail = creatorEmail,
        members = members,
        memberCount = memberCount,
        activityLabel = activityLabel,
        membershipType = membershipType,
        isDeviceOnly = isDeviceOnly,
        lastSessionAt = lastSessionAt,
    )

fun Circle.toEntity(): CircleEntity =
    CircleEntity(
        id = id,
        name = name,
        creatorEmail = creatorEmail,
        memberCount = memberCount,
        activityLabel = activityLabel,
        membershipType = membershipType,
        isDeviceOnly = isDeviceOnly,
        lastSessionAt = lastSessionAt,
    )

/** Maps the counted POJO to the domain model, overriding the static count with the live one. */
fun CircleWithMemberCount.toDomain(): Circle =
    circle.toDomain().copy(memberCount = memberCount)

// ---------------------------------------------------------------------------
// RosterMember  <->  PlayerEntity
// ---------------------------------------------------------------------------

fun PlayerEntity.toDomain(): RosterMember =
    RosterMember(
        id = id,
        name = name,
        initial = initial,
        colorKey = colorKey,
        membershipType = membershipType,
    )

/** [RosterMember] has no id; mint one on insert (or reuse an existing one for updates). */
fun RosterMember.toEntity(circleId: String, id: String = UUID.randomUUID().toString()): PlayerEntity =
    PlayerEntity(
        id = id,
        circleId = circleId,
        name = name,
        initial = initial,
        colorKey = colorKey,
        membershipType = membershipType,
    )

// ---------------------------------------------------------------------------
// TrackedGame  <->  GameEntity
// ---------------------------------------------------------------------------

/** Games round-trip by [TrackedGame.templateId] — unlimited game identities, no GameId collapse. */
fun GameEntity.toDomain(): TrackedGame =
    TrackedGame(
        templateId = templateId,
        displayName = displayName,
        scoringType = scoringType,
        sessionCount = sessionCount,
        // Score unit isn't stored — resolve it from the catalog template (FIFA -> "goals").
        scoringUnit = MockGameData.templateById(templateId)?.scoringUnit ?: "points",
        isLowerScoreBetter = isLowerScoreBetter,
    )

/** Same game, but with the live correlated session count overriding the stale stored one. */
fun GameWithSessionCount.toDomain(): TrackedGame =
    game.toDomain().copy(sessionCount = sessionCount)

fun TrackedGame.toEntity(circleId: String, id: String = UUID.randomUUID().toString()): GameEntity =
    GameEntity(
        id = id,
        circleId = circleId,
        templateId = templateId,
        displayName = displayName,
        scoringType = scoringType,
        sessionCount = sessionCount,
        isLowerScoreBetter = isLowerScoreBetter,
    )

// ---------------------------------------------------------------------------
// SessionWithGame  ->  SessionSummary  (Feed)
// ---------------------------------------------------------------------------

/**
 * Flattens a persisted session + its game into a Feed-ready [SessionSummary]. Winners/losers/score
 * are recovered from the denormalized JSON (via [SessionAggregator]) and handed to
 * [FeedMessageGenerator] for the fun, game-specific flavor line. Seeded by the session id so the
 * card shows a stable message across re-emissions.
 */
fun SessionWithGame.toSummary(roster: List<RosterMember>): SessionSummary {
    val prefix = "${session.circleId}:"
    val participantIds = session.playerIds.map { it.removePrefix(prefix) }
    
    fun resolveName(id: String) = roster.find { it.id == id }?.name ?: id
    
    val participantNames = participantIds.map { resolveName(it) }

    val templateId = game?.templateId ?: session.gameId.removePrefix(prefix)
    val scoring = game?.scoringType
    val inverted = game?.isLowerScoreBetter ?: false

    // Winners = placement 1; keep them in participant order for stable, readable grammar.
    val winnerSet = SessionAggregator.winnersOf(scoring, session.resultsJson, participantIds.size, inverted)
    val winners = participantIds.filter { it in winnerSet }.map { resolveName(it) }
    val losers = participantIds.filterNot { it in winnerSet }.map { resolveName(it) }
    // Full per-player scores so the generator can compute the margin of victory (empty for non-points).
    val scores = SessionAggregator.pointsOf(scoring, session.resultsJson).mapKeys { resolveName(it.key) }

    return SessionSummary(
        id = session.id,
        templateId = templateId,
        gameName = game?.displayName ?: templateId,
        scoringType = scoring ?: ScoringType.WIN_LOSS,
        playedAt = session.playedAt,
        participantNames = participantNames,
        recap = FeedMessageGenerator.generate(
            templateId = templateId,
            winners = winners,
            losers = losers,
            scores = scores,
            seed = session.id.hashCode(),
        ),
    )
}
