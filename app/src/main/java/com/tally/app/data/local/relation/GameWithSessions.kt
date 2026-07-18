package com.tally.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.tally.app.data.local.entity.GameEntity
import com.tally.app.data.local.entity.SessionEntity

/** One game plus all sessions logged under it — a 1:N join Room fills in automatically. */
data class GameWithSessions(
    @Embedded val game: GameEntity,
    @Relation(parentColumn = "id", entityColumn = "gameId")
    val sessions: List<SessionEntity>,
)
