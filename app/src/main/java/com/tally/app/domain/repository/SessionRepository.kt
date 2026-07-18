package com.tally.app.domain.repository

import com.tally.app.data.SessionSummary
import com.tally.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Session CRUD. Interim: typed on [SessionEntity] because there's no domain Session model yet —
 * a session's results shape (SessionResults) still lives in the ui.session layer. Swap to a domain
 * Session (+ mapper) once that shape is finalized in the data layer.
 */
interface SessionRepository {
    fun getSessionsForCircle(circleId: String): Flow<List<SessionEntity>>
    fun getSessionsForGame(gameId: String): Flow<List<SessionEntity>>
    /** Feed timeline: each session in the circle joined to its game + resolved names, newest first. */
    fun getFeedForCircle(circleId: String): Flow<List<SessionSummary>>
    suspend fun addSession(session: SessionEntity, isOnline: Boolean = false)
    suspend fun deleteSession(session: SessionEntity)
    /** Bulk delete sessions by id (Feed multi-select). */
    suspend fun deleteSessions(sessionIds: List<String>, circleId: String)
}
