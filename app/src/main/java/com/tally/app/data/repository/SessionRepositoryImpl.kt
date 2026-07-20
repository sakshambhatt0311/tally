package com.tally.app.data.repository

import com.tally.app.data.SessionSummary
import com.tally.app.data.local.dao.SessionDao
import com.tally.app.data.local.entity.SessionEntity
import com.tally.app.data.mapper.toSummary
import com.tally.app.di.ApplicationScope
import com.tally.app.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

import com.google.firebase.firestore.FirebaseFirestore
import com.tally.app.domain.repository.CircleRepository
import com.tally.app.data.GameCatalog
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.channels.awaitClose
import com.google.firebase.firestore.Query
import com.tally.app.data.analytics.SessionAggregator
import com.tally.app.data.FeedMessageGenerator
import kotlinx.coroutines.flow.firstOrNull

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val firestore: FirebaseFirestore,
    private val circleRepository: CircleRepository,
    private val playerRepository: com.tally.app.domain.repository.PlayerRepository,
    private val gameCatalog: GameCatalog,
    @ApplicationScope scope: CoroutineScope,
) : SessionRepository {

    // Hot caches — keep the latest session data in memory across ViewModel/nav teardown.
    private val circleSessions = KeyedFlowCache(scope) { circleId ->
        circleRepository.getCircle(circleId).flatMapLatest { circle ->
            if (circle?.isDeviceOnly == false) {
                observeOnlineSessionsForCircle(circleId)
            } else {
                sessionDao.getSessionsForCircle(circleId)
            }
        }
    }
    private val gameSessions = KeyedFlowCache(scope) { gameRowId -> 
        val circleId = gameRowId.substringBefore(":")
        circleRepository.getCircle(circleId).flatMapLatest { circle ->
            if (circle?.isDeviceOnly == false) {
                observeOnlineSessionsForGame(circleId, gameRowId)
            } else {
                sessionDao.getSessionsForGame(gameRowId)
            }
        }
    }
    private val feed = KeyedFlowCache(scope) { circleId ->
        kotlinx.coroutines.flow.combine(
            circleRepository.getCircle(circleId),
            playerRepository.getPlayersForCircle(circleId)
        ) { circle, players -> Pair(circle, players) }
        .flatMapLatest { (circle, players) ->
            if (circle?.isDeviceOnly == false) {
                observeOnlineFeedForCircle(circleId, players)
            } else {
                sessionDao.getSessionsWithGameForCircle(circleId).map { rows -> rows.map { it.toSummary(players) } }
            }
        }
    }

    override fun getSessionsForCircle(circleId: String): Flow<List<SessionEntity>> = circleSessions[circleId]

    override fun getSessionsForGame(gameId: String): Flow<List<SessionEntity>> = gameSessions[gameId]

    override fun getFeedForCircle(circleId: String): Flow<List<SessionSummary>> = feed[circleId]

    override suspend fun addSession(session: SessionEntity, isOnline: Boolean) {
        if (isOnline) {
            try {
                val sessionMap = hashMapOf(
                    "id" to session.id,
                    "circleId" to session.circleId,
                    "gameId" to session.gameId,
                    "playedAt" to session.playedAt,
                    "playerIds" to session.playerIds,
                    "resultsJson" to session.resultsJson
                )
                val batch = firestore.batch()
                val sessionRef = firestore.collection("sessions").document(session.id)
                batch.set(sessionRef, sessionMap)
                
                val circleRef = firestore.collection("circles").document(session.circleId)
                batch.update(circleRef, "lastSessionAt", session.playedAt)
                
                batch.commit().await()
                android.util.Log.d("SessionRepository", "Online session ${session.id} saved to Firestore")
            } catch (e: Exception) {
                android.util.Log.e("SessionRepository", "Failed to save online session", e)
                throw e
            }
        } else {
            sessionDao.insert(session)
            circleRepository.getCircle(session.circleId).firstOrNull()?.let { circle ->
                circleRepository.upsertCircle(circle.copy(lastSessionAt = session.playedAt))
            }
        }
    }

    private fun observeOnlineSessionsForCircle(circleId: String): Flow<List<SessionEntity>> = callbackFlow {
        val listener = firestore.collection("sessions")
            .whereEqualTo("circleId", circleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val sessions = snapshot.documents.mapNotNull { doc ->
                        SessionEntity(
                            id = doc.getString("id") ?: doc.id,
                            circleId = doc.getString("circleId") ?: "",
                            gameId = doc.getString("gameId") ?: "",
                            playedAt = doc.getLong("playedAt") ?: 0L,
                            playerIds = doc.get("playerIds") as? List<String> ?: emptyList(),
                            resultsJson = doc.getString("resultsJson") ?: "{}"
                        )
                    }.sortedByDescending { it.playedAt }
                    trySend(sessions)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    private fun observeOnlineSessionsForGame(circleId: String, gameRowId: String): Flow<List<SessionEntity>> = callbackFlow {
        val listener = firestore.collection("sessions")
            .whereEqualTo("circleId", circleId)
            .whereEqualTo("gameId", gameRowId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val sessions = snapshot.documents.mapNotNull { doc ->
                        SessionEntity(
                            id = doc.getString("id") ?: doc.id,
                            circleId = doc.getString("circleId") ?: "",
                            gameId = doc.getString("gameId") ?: "",
                            playedAt = doc.getLong("playedAt") ?: 0L,
                            playerIds = doc.get("playerIds") as? List<String> ?: emptyList(),
                            resultsJson = doc.getString("resultsJson") ?: "{}"
                        )
                    }.sortedByDescending { it.playedAt }
                    trySend(sessions)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    private fun observeOnlineFeedForCircle(circleId: String, roster: List<com.tally.app.data.RosterMember>): Flow<List<SessionSummary>> = callbackFlow {
        val listener = firestore.collection("sessions")
            .whereEqualTo("circleId", circleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val prefix = "$circleId:"
                    val summaries = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: return@mapNotNull null
                        val fullGameId = doc.getString("gameId") ?: return@mapNotNull null
                        val templateId = fullGameId.substringAfter(":")
                        val template = gameCatalog.byId(templateId)
                        val playedAt = doc.getLong("playedAt") ?: 0L
                        val playerIds = doc.get("playerIds") as? List<String> ?: emptyList()
                        val resultsJson = doc.getString("resultsJson") ?: "{}"
                        
                        val participantIds = playerIds.map { it.removePrefix(prefix) }
                        fun resolveName(uid: String) = roster.find { it.id == uid }?.name ?: uid
                        val participantNames = participantIds.map { resolveName(it) }
                        
                        val scoring = template?.scoringType ?: com.tally.app.data.ScoringType.WIN_LOSS
                        val inverted = template?.isLowerScoreBetter ?: false
                        
                        val winnerSet = SessionAggregator.winnersOf(scoring, resultsJson, participantIds.size, inverted)
                        val winners = participantIds.filter { it in winnerSet }.map { resolveName(it) }
                        val losers = participantIds.filterNot { it in winnerSet }.map { resolveName(it) }
                        val scores = SessionAggregator.pointsOf(scoring, resultsJson).mapKeys { resolveName(it.key) }
                        
                        SessionSummary(
                            id = id,
                            templateId = templateId,
                            gameName = template?.name ?: templateId,
                            scoringType = scoring,
                            playedAt = playedAt,
                            participantNames = participantNames,
                            recap = FeedMessageGenerator.generate(
                                templateId = templateId,
                                winners = winners,
                                losers = losers,
                                scores = scores,
                                seed = id.hashCode()
                            )
                        )
                    }.sortedByDescending { it.playedAt }.take(50)
                    trySend(summaries)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun deleteSession(session: SessionEntity) {
        val circle = circleRepository.getCircle(session.circleId).firstOrNull()
        if (circle?.isDeviceOnly == false) {
            try {
                firestore.collection("sessions").document(session.id).delete().await()
                android.util.Log.d("SessionRepository", "Online session ${session.id} deleted from Firestore")
            } catch (e: Exception) {
                android.util.Log.e("SessionRepository", "Failed to delete online session", e)
                throw e
            }
        } else {
            sessionDao.delete(session)
        }
    }

    override suspend fun deleteSessions(sessionIds: List<String>, circleId: String) {
        val circle = circleRepository.getCircle(circleId).firstOrNull()
        if (circle?.isDeviceOnly == false) {
            try {
                val batch = firestore.batch()
                for (id in sessionIds) {
                    val ref = firestore.collection("sessions").document(id)
                    batch.delete(ref)
                }
                batch.commit().await()
                android.util.Log.d("SessionRepository", "Online sessions deleted from Firestore")
            } catch (e: Exception) {
                android.util.Log.e("SessionRepository", "Failed to delete online sessions", e)
                throw e
            }
        } else {
            sessionDao.deleteByIds(sessionIds)
        }
    }
}
