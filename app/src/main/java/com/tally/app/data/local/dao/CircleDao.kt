package com.tally.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tally.app.data.local.entity.CircleEntity
import com.tally.app.data.local.relation.CircleWithMemberCount
import kotlinx.coroutines.flow.Flow

@Dao
interface CircleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(circle: CircleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(circles: List<CircleEntity>)

    @Update
    suspend fun update(circle: CircleEntity)

    @Delete
    suspend fun delete(circle: CircleEntity)

    @Query("SELECT * FROM circles ORDER BY name")
    fun getAllCircles(): Flow<List<CircleEntity>>

    /** Circles with a live member count computed from the players table (not the static column). */
    @Query(
        "SELECT c.*, (SELECT COUNT(id) FROM players WHERE circleId = c.id) AS memberCountDynamic " +
            "FROM circles c ORDER BY c.name",
    )
    fun getCirclesWithMemberCount(): Flow<List<CircleWithMemberCount>>

    @Query("SELECT * FROM circles WHERE id = :circleId")
    fun getCircle(circleId: String): Flow<CircleEntity?>
}
