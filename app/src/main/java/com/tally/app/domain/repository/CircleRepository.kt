package com.tally.app.domain.repository

import com.tally.app.data.Circle
import kotlinx.coroutines.flow.Flow

/** Circle CRUD over the local store. Read = reactive Flow, writes = suspend on domain models. */
interface CircleRepository {
    fun getCircles(): Flow<List<Circle>>
    fun getCircle(circleId: String): Flow<Circle?>
    suspend fun upsertCircle(circle: Circle)
    suspend fun deleteCircle(circle: Circle)
    suspend fun createOnlineCircle(name: String, creatorId: String, creatorEmail: String?): Result<String>
    fun observeOnlineCircles(userId: String): Flow<List<Circle>>
    suspend fun updateOnlineCircle(circle: Circle): Result<Unit>
    suspend fun deleteOnlineCircle(circleId: String): Result<Unit>
    suspend fun addGuestMemberToOnlineCircle(circleId: String, guestName: String): Result<Unit>
    suspend fun addRegisteredMemberToOnlineCircle(circleId: String, email: String): Result<Unit>
    suspend fun removeMemberFromOnlineCircle(circleId: String, memberId: String): Result<Unit>
    suspend fun joinCircleByCode(inviteCode: String, userId: String): Result<String>
    fun observeOnlineMembers(memberIds: List<String>): Flow<List<com.tally.app.domain.model.User>>
}
