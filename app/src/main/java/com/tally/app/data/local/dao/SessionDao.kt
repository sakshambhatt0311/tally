package com.tally.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.tally.app.data.local.entity.SessionEntity
import com.tally.app.data.local.relation.SessionWithGame
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    // Sessions are append-only history — a new row must never overwrite an existing one. ABORT throws
    // SQLiteConstraintException on a genuine id collision instead of masking it with a silent REPLACE.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    /** Bulk delete by id — backs the Feed's multi-select "Edit Sessions" delete. */
    @Query("DELETE FROM sessions WHERE id IN (:sessionIds)")
    suspend fun deleteByIds(sessionIds: List<String>)

    @Query("SELECT * FROM sessions WHERE gameId = :gameId ORDER BY playedAt DESC")
    fun getSessionsForGame(gameId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE circleId = :circleId ORDER BY playedAt DESC")
    fun getSessionsForCircle(circleId: String): Flow<List<SessionEntity>>

    /**
     * Feed source: every session in the circle joined to its game, newest first. @Transaction keeps
     * the session read and its game @Relation consistent; the parent query's ORDER BY is preserved.
     */
    @Transaction
    @Query("SELECT * FROM sessions WHERE circleId = :circleId ORDER BY playedAt DESC")
    fun getSessionsWithGameForCircle(circleId: String): Flow<List<SessionWithGame>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSession(sessionId: String): Flow<SessionEntity?>
}
