package com.tally.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tally.app.data.local.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<PlayerEntity>)

    @Update
    suspend fun update(player: PlayerEntity)

    @Delete
    suspend fun delete(player: PlayerEntity)

    @Query("DELETE FROM players WHERE id = :playerId")
    suspend fun deleteById(playerId: String)

    @Query("SELECT * FROM players WHERE circleId = :circleId ORDER BY name")
    fun getPlayersForCircle(circleId: String): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :playerId")
    fun getPlayer(playerId: String): Flow<PlayerEntity?>
}
