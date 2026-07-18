package com.tally.app.data.local.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.tally.app.data.local.entity.CircleEntity

/**
 * A circle plus its live member count, computed by a correlated COUNT over the players table.
 *
 * The computed column is aliased `memberCountDynamic` (not `memberCount`) on purpose: `CircleEntity`
 * already has a static `memberCount` column that `@Embedded val circle` maps, so reusing the name
 * would make Room's column resolution ambiguous.
 */
data class CircleWithMemberCount(
    @Embedded val circle: CircleEntity,
    @ColumnInfo(name = "memberCountDynamic") val memberCount: Int,
)
