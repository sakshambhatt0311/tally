package com.tally.app.data.repository

import android.util.Log
import com.tally.app.data.Circle
import com.tally.app.data.GuestMember
import com.tally.app.data.local.dao.CircleDao
import com.tally.app.data.local.dao.PlayerDao
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.tally.app.data.mapper.toDomain
import com.tally.app.domain.model.User
import com.tally.app.data.mapper.toEntity
import com.tally.app.di.ApplicationScope
import kotlinx.coroutines.tasks.await
import com.tally.app.domain.repository.CircleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject

private const val TAG = "TallyDB"

class CircleRepositoryImpl @Inject constructor(
    private val circleDao: CircleDao,
    private val playerDao: PlayerDao,
    private val firestore: FirebaseFirestore,
    @ApplicationScope scope: CoroutineScope,
) : CircleRepository {

    // Hot cache for the My Circles list — stays in memory app-wide (single, un-keyed flow).
    private val circles: Flow<List<Circle>> = circleDao.getCirclesWithMemberCount()
        .onEach { Log.d(TAG, "READ circles: ${it.size}") }
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.Default)
        .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 10_000), replay = 1)

    // Hot per-circle detail cache (circle + live roster).
    private val circleDetail = KeyedFlowCache(scope) { circleId ->
        combine(
            circleDao.getCircle(circleId),
            playerDao.getPlayersForCircle(circleId),
        ) { circle, players ->
            circle?.toDomain(members = players.map { it.initial to it.colorKey })
                ?.copy(memberCount = players.size)
        }.flatMapLatest { localCircle ->
            if (localCircle != null) {
                flowOf(localCircle)
            } else {
                observeOnlineCircle(circleId)
            }
        }
    }

    private fun observeOnlineCircle(circleId: String): Flow<Circle?> = callbackFlow {
        val listener = firestore.collection("circles").document(circleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val id = snapshot.getString("id") ?: return@addSnapshotListener
                    val name = snapshot.getString("name") ?: "Unnamed Circle"
                    val creatorId = snapshot.getString("creatorId") ?: ""
                    val creatorEmail = snapshot.getString("creatorEmail")
                    @Suppress("UNCHECKED_CAST")
                    val memberIds = snapshot.get("memberIds") as? List<String> ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val guestMembers = (snapshot.get("guestMembers") as? Map<String, String>)
                        ?.map { (guestId, guestName) -> GuestMember(guestId, guestName) }
                        ?: emptyList()
                    val circle = Circle(
                        id = id,
                        name = name,
                        creatorId = creatorId,
                        creatorEmail = creatorEmail,
                        memberIds = memberIds,
                        members = emptyList(),
                        memberCount = memberIds.size + guestMembers.size,
                        activityLabel = "Online",
                        membershipType = com.tally.app.data.MembershipType.LINKED,
                        isDeviceOnly = false,
                        inviteCode = snapshot.getString("inviteCode") ?: "",
                        lastSessionAt = snapshot.getLong("lastSessionAt"),
                        guestMembers = guestMembers,
                    )
                    trySend(circle)
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getCircles(): Flow<List<Circle>> = circles

    /** Enrich the single circle with its roster; member count = the live roster size. */
    override fun getCircle(circleId: String): Flow<Circle?> = circleDetail[circleId]

    override suspend fun upsertCircle(circle: Circle) {
        Log.d(TAG, "WRITE upsertCircle: ${circle.id} '${circle.name}'")
        circleDao.insert(circle.toEntity())
    }

    override suspend fun deleteCircle(circle: Circle) {
        Log.d(TAG, "WRITE deleteCircle: ${circle.id} '${circle.name}'")
        circleDao.delete(circle.toEntity())
    }

    private fun generateInviteCode(): String {
        val letters = (1..4).map { ('A'..'Z').random() }.joinToString("")
        val numbers = (1..2).map { ('0'..'9').random() }.joinToString("")
        return letters + numbers
    }

    override suspend fun createOnlineCircle(name: String, creatorId: String, creatorEmail: String?): Result<String> {
        return try {
            val docRef = firestore.collection("circles").document()
            val circleId = docRef.id
            val circle = Circle(
                id = circleId,
                name = name,
                creatorId = creatorId,
                creatorEmail = creatorEmail,
                memberIds = listOf(creatorId),
                memberCount = 1,
                activityLabel = "Online",
                membershipType = com.tally.app.data.MembershipType.OWNER,
                isDeviceOnly = false,
                inviteCode = generateInviteCode(),
                lastSessionAt = System.currentTimeMillis()
            )
            docRef.set(circle).await()
            Log.d(TAG, "WRITE createOnlineCircle: $circleId '$name'")
            Result.success(circleId)
        } catch (e: Exception) {
            Log.e(TAG, "createOnlineCircle failed", e)
            Result.failure(e)
        }
    }

    override suspend fun joinCircleByCode(inviteCode: String, userId: String): Result<String> {
        return try {
            val snapshot = firestore.collection("circles")
                .whereEqualTo("inviteCode", inviteCode.uppercase())
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return Result.failure(Exception("Circle doesn't exist"))
            }

            val doc = snapshot.documents.first()
            val circleId = doc.id
            val currentMemberIds = doc.get("memberIds") as? List<String> ?: emptyList()

            if (currentMemberIds.contains(userId)) {
                return Result.failure(Exception("You are already a member of this circle"))
            }

            doc.reference.update(
                mapOf(
                    "memberIds" to com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                    "memberCount" to com.google.firebase.firestore.FieldValue.increment(1)
                )
            ).await()
            
            Log.d(TAG, "User $userId joined circle $circleId via invite code $inviteCode")
            Result.success(circleId)
        } catch (e: Exception) {
            Log.e(TAG, "joinCircleByCode failed", e)
            Result.failure(e)
        }
    }

    override fun observeOnlineCircles(userId: String): Flow<List<Circle>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("circles")
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeOnlineCircles error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                try {
                    val onlineCircles = snapshot?.documents?.mapNotNull { doc ->
                        val id = doc.getString("id") ?: return@mapNotNull null
                        val name = doc.getString("name") ?: "Unnamed Circle"
                        val creatorId = doc.getString("creatorId") ?: ""
                        val creatorEmail = doc.getString("creatorEmail")
                        val memberIds = doc.get("memberIds") as? List<String> ?: emptyList()
                        // Guests live in the `guestMembers` map (not memberIds), so add their count in
                        // or the card would never grow past the registered members.
                        val guestCount = (doc.get("guestMembers") as? Map<*, *>)?.size ?: 0
                        val membershipType = if (creatorId == userId) com.tally.app.data.MembershipType.OWNER else com.tally.app.data.MembershipType.LINKED

                        Circle(
                            id = id,
                            name = name,
                            creatorId = creatorId,
                            creatorEmail = creatorEmail,
                            memberIds = memberIds,
                            members = emptyList(), // Can fetch members if needed
                            memberCount = memberIds.size + guestCount,
                            activityLabel = "Online",
                            membershipType = membershipType,
                            isDeviceOnly = false,
                            inviteCode = doc.getString("inviteCode") ?: "",
                            lastSessionAt = doc.getLong("lastSessionAt")
                        )
                    }?.sortedByDescending { it.lastSessionAt ?: 0L } ?: emptyList()
                    
                    trySend(onlineCircles)
                } catch (e: Exception) {
                    Log.e(TAG, "observeOnlineCircles processing error", e)
                    trySend(emptyList())
                }
            }
            
        awaitClose { listener.remove() }
    }

    override suspend fun updateOnlineCircle(circle: Circle): Result<Unit> {
        return try {
            val docRef = firestore.collection("circles").document(circle.id)
            val updates = mapOf(
                "name" to circle.name
                // Add other mutable fields as necessary
            )
            docRef.update(updates).await()
            Log.d(TAG, "WRITE updateOnlineCircle: ${circle.id} '${circle.name}'")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateOnlineCircle failed", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteOnlineCircle(circleId: String): Result<Unit> {
        return try {
            firestore.collection("circles").document(circleId).delete().await()
            Log.d(TAG, "WRITE deleteOnlineCircle: $circleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteOnlineCircle failed", e)
            Result.failure(e)
        }
    }

    override suspend fun removeMemberFromOnlineCircle(circleId: String, memberId: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("circles").document(circleId)
            // The id may be a registered member (in memberIds) or a guest (a key in the guestMembers
            // map). Clear it from both in one write — the non-matching one is a harmless no-op — so a
            // single decrement is correct either way.
            docRef.update(
                FieldPath.of("memberIds"), FieldValue.arrayRemove(memberId),
                FieldPath.of("guestMembers", memberId), FieldValue.delete(),
                FieldPath.of("memberCount"), FieldValue.increment(-1),
            ).await()
            Log.d(TAG, "Member $memberId removed from circle $circleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "removeMemberFromOnlineCircle failed", e)
            Result.failure(e)
        }
    }

    override suspend fun addGuestMemberToOnlineCircle(circleId: String, guestName: String): Result<Unit> {
        return try {
            val guestId = java.util.UUID.randomUUID().toString()
            // Store the guest ON the circle doc (guestMembers map: id -> name), NOT as a fake `users`
            // doc. Writing an arbitrary users/{id} is blocked by security rules (a client may only
            // write its own uid), which is why guests never appeared. Updating the shared circle doc
            // is already permitted for members (same path add/remove uses), so this syncs to everyone.
            firestore.collection("circles").document(circleId)
                .update(
                    FieldPath.of("guestMembers", guestId), guestName,
                    FieldPath.of("memberCount"), FieldValue.increment(1),
                ).await()

            Log.d(TAG, "WRITE addGuestMemberToOnlineCircle: guest $guestId added to $circleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "addGuestMemberToOnlineCircle failed", e)
            Result.failure(e)
        }
    }

    override suspend fun addRegisteredMemberToOnlineCircle(circleId: String, email: String): Result<Unit> {
        return try {
            val usersQuery = firestore.collection("users").whereEqualTo("email", email).get().await()
            if (usersQuery.isEmpty) {
                return Result.failure(Exception("No registered user found with that email."))
            }
            val userId = usersQuery.documents.first().id
            firestore.collection("circles").document(circleId)
                .update(
                    "memberIds", FieldValue.arrayUnion(userId),
                    "memberCount", FieldValue.increment(1)
                ).await()
                
            Log.d(TAG, "WRITE addRegisteredMemberToOnlineCircle: user $userId added to $circleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "addRegisteredMemberToOnlineCircle failed", e)
            Result.failure(e)
        }
    }

    override fun observeOnlineMembers(memberIds: List<String>): Flow<List<User>> = callbackFlow {
        Log.d("MembersDebug", "observeOnlineMembers starting query with memberIds: $memberIds")
        if (memberIds.isEmpty()) {
            Log.d("MembersDebug", "memberIds is empty, returning empty list immediately")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val chunks = memberIds.chunked(10)
        val chunkResults = mutableMapOf<Int, List<User>>()
        val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

        try {
            chunks.forEachIndexed { index, chunk ->
                val listener = firestore.collection("users")
                    .whereIn(FieldPath.documentId(), chunk)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("MembersDebug", "observeOnlineMembers snapshot listener error", error)
                            return@addSnapshotListener
                        }
                        
                        try {
                            val users = snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(User::class.java)
                            } ?: emptyList()
                            
                            chunkResults[index] = users
                            
                            // Combine all chunk results
                            val allUsers = chunkResults.values.flatten()
                            trySend(allUsers)
                        } catch (e: Exception) {
                            Log.e("MembersDebug", "Exception during document mapping", e)
                        }
                    }
                listeners.add(listener)
            }
        } catch (e: Exception) {
            Log.e("MembersDebug", "Failed to setup observeOnlineMembers listeners", e)
            trySend(emptyList())
        }
        
        awaitClose { 
            Log.d("MembersDebug", "observeOnlineMembers flow closed, removing listeners")
            listeners.forEach { it.remove() }
        }
    }
}
