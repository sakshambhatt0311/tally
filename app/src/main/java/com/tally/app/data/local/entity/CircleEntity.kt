package com.tally.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tally.app.data.MembershipType
import java.util.UUID

/** A circle (group). Root of the ownership tree — players/games/sessions all cascade off it. */
@Entity(tableName = "circles")
data class CircleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val creatorEmail: String? = null,
    val memberCount: Int,
    val activityLabel: String,
    val membershipType: MembershipType,
    val isDeviceOnly: Boolean = false,
    val lastSessionAt: Long? = null,
)
