package com.tally.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.tally.app.data.local.entity.GameEntity
import com.tally.app.data.local.relation.GameWithSessionCount
import com.tally.app.data.local.relation.GameWithSessions
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: GameEntity)

    /**
     * Insert only if the row doesn't already exist. Unlike REPLACE (which DELETEs then INSERTs and
     * cascade-wipes the game's sessions), IGNORE leaves an existing game — and its history — intact.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<GameEntity>)

    @Update
    suspend fun update(game: GameEntity)

    @Delete
    suspend fun delete(game: GameEntity)

    @Query("SELECT * FROM games WHERE circleId = :circleId ORDER BY displayName")
    fun getGamesForCircle(circleId: String): Flow<List<GameEntity>>

    /**
     * Games for a circle, each with a live session count via a correlated COUNT over the sessions
     * table — so the Games list reflects real play history instead of the stale stored column.
     */
    @Query(
        """
        SELECT *, (SELECT COUNT(id) FROM sessions WHERE sessions.gameId = games.id) AS dynamicSessionCount
        FROM games WHERE circleId = :circleId ORDER BY dynamicSessionCount DESC
        """,
    )
    fun getGamesWithSessionCount(circleId: String): Flow<List<GameWithSessionCount>>

    @Query("SELECT * FROM games WHERE id = :gameId")
    fun getGame(gameId: String): Flow<GameEntity?>

    /** A game with every session logged under it — @Transaction keeps the two reads consistent. */
    @Transaction
    @Query("SELECT * FROM games WHERE id = :gameId")
    fun getGameWithSessions(gameId: String): Flow<GameWithSessions?>
}
