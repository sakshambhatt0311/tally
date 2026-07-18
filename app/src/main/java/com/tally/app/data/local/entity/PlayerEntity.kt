package com.tally.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tally.app.data.MembershipType
import com.tally.app.data.PlayerColorKey
import java.util.UUID

/** A player/member belonging to one circle. Deleting the circle cascades its players away. */
@Entity(
    tableName = "players",
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
data class PlayerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val circleId: String,
    val name: String,
    val initial: String,
    val colorKey: PlayerColorKey,
    val membershipType: MembershipType,
)
