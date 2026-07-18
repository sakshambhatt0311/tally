package com.tally.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * One logged session. References its circle and game; deleting either cascades the session away.
 * [playerIds] and [resultsJson] hold denormalized JSON (see Converters) until per-result rows exist.
 */
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = CircleEntity::class,
            parentColumns = ["id"],
            childColumns = ["circleId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("circleId"), Index("gameId")],
)
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val circleId: String,
    val gameId: String,
    val playedAt: Long = System.currentTimeMillis(),
    /** Player ids in finishing order — stored as a JSON string list. */
    val playerIds: List<String>,
    /** Per-player results (points/placement/winner), serialized JSON. */
    val resultsJson: String,
)
