package com.tally.app.domain.repository

import com.tally.app.data.RosterMember
import kotlinx.coroutines.flow.Flow

/**
 * Player CRUD scoped to a circle. [RosterMember] carries no id/circleId, so writes take the
 * owning circleId explicitly; the mapper mints a stable id on insert.
 */
interface PlayerRepository {
    fun getPlayersForCircle(circleId: String): Flow<List<RosterMember>>
    suspend fun upsertPlayer(circleId: String, player: RosterMember)
    suspend fun deletePlayer(circleId: String, player: RosterMember)
}
