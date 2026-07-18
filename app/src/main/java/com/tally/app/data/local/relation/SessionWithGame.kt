package com.tally.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.tally.app.data.local.entity.GameEntity
import com.tally.app.data.local.entity.SessionEntity

/**
 * One session joined to the game it was played under, so the Feed can render the real game
 * name/icon without a second lookup. Players stay denormalized in [SessionEntity.playerIds] /
 * resultsJson — a JSON list column can't be a Room @Relation — and are resolved in the mapper.
 */
data class SessionWithGame(
    @Embedded val session: SessionEntity,
    @Relation(parentColumn = "gameId", entityColumn = "id")
    val game: GameEntity?,
)
