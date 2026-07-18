package com.tally.app.domain.repository

import com.tally.app.data.TrackedGame
import kotlinx.coroutines.flow.Flow

/** Tracked-game CRUD scoped to a circle. Writes take the owning circleId explicitly. */
interface GameRepository {
    fun getGamesForCircle(circleId: String): Flow<List<TrackedGame>>
    suspend fun upsertGame(circleId: String, game: TrackedGame)
    suspend fun deleteGame(circleId: String, game: TrackedGame)
}
