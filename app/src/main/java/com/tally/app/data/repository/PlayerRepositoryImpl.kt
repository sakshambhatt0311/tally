package com.tally.app.data.repository

import com.tally.app.data.RosterMember
import com.tally.app.data.local.dao.PlayerDao
import com.tally.app.data.mapper.toDomain
import com.tally.app.data.mapper.toEntity
import com.tally.app.di.ApplicationScope
import com.tally.app.domain.repository.PlayerRepository
import com.tally.app.domain.repository.CircleRepository
import com.tally.app.data.MembershipType
import com.tally.app.data.PlayerColorKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class PlayerRepositoryImpl @Inject constructor(
    private val playerDao: PlayerDao,
    private val circleRepository: CircleRepository,
    @ApplicationScope scope: CoroutineScope,
) : PlayerRepository {

    // Hot per-circle roster cache — survives ViewModel death so re-entering a Circle is instant.
    private val roster = KeyedFlowCache(scope) { circleId ->
        circleRepository.getCircle(circleId).flatMapLatest { circle ->
            if (circle == null) {
                flowOf(emptyList())
            } else if (circle.isDeviceOnly) {
                playerDao.getPlayersForCircle(circleId).map { players -> players.map { it.toDomain() } }
            } else {
                circleRepository.observeOnlineMembers(circle.memberIds).map { users ->
                    users.map { user ->
                        val name = user.displayName ?: "Unknown"
                        RosterMember(
                            id = user.uid,
                            name = name,
                            initial = name.take(1).uppercase(),
                            colorKey = PlayerColorKey.BLUE, // Fallback, could assign dynamically
                            membershipType = when {
                                user.uid == circle.creatorId -> MembershipType.OWNER
                                user.isGuest -> MembershipType.LOCAL
                                else -> MembershipType.LINKED
                            },
                            photoUrl = user.photoUrl
                        )
                    }
                }
            }
        }
    }

    override fun getPlayersForCircle(circleId: String): Flow<List<RosterMember>> = roster[circleId]

    override suspend fun upsertPlayer(circleId: String, player: RosterMember) =
        playerDao.insert(player.toEntity(circleId, id = playerId(circleId, player.name)))

    override suspend fun deletePlayer(circleId: String, player: RosterMember) =
        playerDao.deleteById(playerId(circleId, player.name))

    /** Deterministic id — a player is unique by name within a circle, so upsert/delete round-trip. */
    private fun playerId(circleId: String, name: String): String = "$circleId:$name"
}
