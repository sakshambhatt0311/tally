package com.tally.app.data.repository

import com.tally.app.data.TrackedGame
import com.tally.app.data.local.dao.GameDao
import com.tally.app.data.mapper.toDomain
import com.tally.app.data.mapper.toEntity
import com.tally.app.di.ApplicationScope
import com.tally.app.domain.repository.GameRepository
import com.tally.app.domain.repository.CircleRepository
import com.tally.app.data.GameCatalog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject

class GameRepositoryImpl @Inject constructor(
    private val gameDao: GameDao,
    private val firestore: FirebaseFirestore,
    private val circleRepository: CircleRepository,
    private val gameCatalog: GameCatalog,
    @ApplicationScope scope: CoroutineScope,
) : GameRepository {

    // Hot per-circle games cache (counted query) — instant on re-entry, live counts.
    private val games = KeyedFlowCache(scope) { circleId ->
        circleRepository.getCircle(circleId).flatMapLatest { circle ->
            if (circle?.isDeviceOnly == false) {
                observeOnlineGames(circleId)
            } else {
                gameDao.getGamesWithSessionCount(circleId).map { rows -> rows.map { it.toDomain() } }
            }
        }
    }

    override fun getGamesForCircle(circleId: String): Flow<List<TrackedGame>> = games[circleId]

    override suspend fun upsertGame(circleId: String, game: TrackedGame) {
        val entity = game.toEntity(circleId, id = gameId(circleId, game))
        // Non-destructive upsert. A plain @Insert(REPLACE) here DELETEs the existing game row and
        // re-inserts it — and the games→sessions FK CASCADE wipes every session logged under it
        // (the "second session overwrites the first" bug). Insert-if-absent, then update fields.
        gameDao.insertIfAbsent(entity)
        gameDao.update(entity)
    }

    override suspend fun deleteGame(circleId: String, game: TrackedGame) =
        gameDao.delete(game.toEntity(circleId, id = gameId(circleId, game)))

    /** Deterministic id — one row per template within a circle, so upsert/delete round-trip. */
    private fun gameId(circleId: String, game: TrackedGame): String = "$circleId:${game.templateId}"

    private fun observeOnlineGames(circleId: String): Flow<List<TrackedGame>> = callbackFlow {
        val listener = firestore.collection("sessions")
            .whereEqualTo("circleId", circleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val gameCounts = mutableMapOf<String, Int>()
                    for (doc in snapshot.documents) {
                        val fullGameId = doc.getString("gameId") ?: continue
                        val templateId = fullGameId.substringAfter(":")
                        gameCounts[templateId] = gameCounts.getOrDefault(templateId, 0) + 1
                    }
                    
                    val trackedGames = gameCounts.mapNotNull { (templateId, count) ->
                        val template = gameCatalog.byId(templateId) ?: return@mapNotNull null
                        TrackedGame(
                            templateId = template.id,
                            displayName = template.name,
                            scoringType = template.scoringType,
                            sessionCount = count,
                            scoringUnit = template.scoringUnit,
                            isLowerScoreBetter = template.isLowerScoreBetter
                        )
                    }
                    trySend(trackedGames)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }
}
