package com.tally.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tally.app.data.ScoringType
import java.util.UUID

/** A game a circle tracks. Deleting the circle cascades its tracked games away. */
@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = CircleEntity::class,
            parentColumns = ["id"],
            childColumns = ["circleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("circleId")],
)
data class GameEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val circleId: String,
    /** References a GameTemplate.id in the catalog — preserves the game's real identity. */
    val templateId: String,
    /** The game's label (defaults to the template name; allows custom names later). */
    val displayName: String,
    val scoringType: ScoringType,
    val sessionCount: Int = 0,
    val isLowerScoreBetter: Boolean = false,
)
