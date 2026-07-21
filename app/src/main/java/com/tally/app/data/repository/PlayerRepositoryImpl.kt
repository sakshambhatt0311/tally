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
                    val palette = PlayerColorKey.entries
                    val registered = users.mapIndexed { index, user ->
                        val name = user.displayName ?: "Unknown"
                        RosterMember(
                            id = user.uid,
                            name = name,
                            initial = name.take(1).uppercase(),
                            colorKey = palette[index % palette.size],
                            membershipType = when {
                                user.uid == circle.creatorId -> MembershipType.OWNER
                                user.isGuest -> MembershipType.LOCAL
                                else -> MembershipType.LINKED
                            },
                            photoUrl = user.photoUrl
                        )
                    }
                    // Guests live on the circle doc, not the users collection — append them so they're
                    // selectable everywhere the roster is used (Log Session, Head-to-Head, stats).
                    val guests = circle.guestMembers.mapIndexed { index, guest ->
                        RosterMember(
                            id = guest.id,
                            name = guest.name,
                            initial = guest.name.take(1).uppercase().ifBlank { "?" },
                            colorKey = palette[(registered.size + index) % palette.size],
                            membershipType = MembershipType.LOCAL,
                        )
                    }
                    registered + guests
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
