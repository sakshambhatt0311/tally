package com.tally.app.data.local.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.tally.app.data.local.entity.GameEntity

/**
 * A tracked game plus its live session count, computed by a correlated COUNT over the sessions table
 * (never stored — the static [GameEntity.sessionCount] column is legacy). Aliased [dynamicSessionCount]
 * so it doesn't collide with the embedded entity's own `sessionCount` column, exactly like
 * [CircleWithMemberCount] aliases `memberCountDynamic`.
 */
data class GameWithSessionCount(
    @Embedded val game: GameEntity,
    @ColumnInfo(name = "dynamicSessionCount") val sessionCount: Int,
)
