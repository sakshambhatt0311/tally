package com.tally.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.MembershipType
import com.tally.app.data.PlayerColorKey
import com.tally.app.data.RosterMember
import com.tally.app.domain.repository.AuthRepository
import com.tally.app.domain.repository.CircleRepository
import com.tally.app.domain.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the Members tab for one circle — roster streamed from Room, scoped by the nav circleId. */
@HiltViewModel
class MembersViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val circleRepository: CircleRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Nav arg from CircleDashboard(circleId, ...) — Navigation puts route args into SavedStateHandle.
    private val circleId: String = checkNotNull(savedStateHandle["circleId"])

    // null = first DB emission pending (avoids an empty-state flash); non-null empty = no members.
    // Lazily keeps the roster warm for the whole Circle session → instant cached replay on tab return.
    val members: StateFlow<List<RosterMember>?> = circleRepository.getCircle(circleId)
        .flatMapLatest { circle ->
            if (circle == null) return@flatMapLatest flowOf(null)
            
            if (circle.isDeviceOnly) {
                playerRepository.getPlayersForCircle(circleId)
            } else {
                circleRepository.observeOnlineMembers(circle.memberIds)
                    .map { users ->
                        val palette = PlayerColorKey.entries
                        val registered = users.mapIndexed { index, user ->
                            RosterMember(
                                id = user.uid,
                                name = user.displayName ?: "Unknown",
                                initial = user.displayName?.take(1)?.uppercase() ?: "?",
                                colorKey = palette[index % palette.size],
                                membershipType = when {
                                    user.uid == circle.creatorId -> MembershipType.OWNER
                                    user.isGuest -> MembershipType.LOCAL
                                    else -> MembershipType.LINKED
                                },
                                photoUrl = user.photoUrl
                            )
                        }
                        // Guests live on the circle doc (not the users collection); append them as
                        // LOCAL members so they show for every account sharing this circle.
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
        .onEach { roster ->
            android.util.Log.d("MembersDebug", "ViewModel received roster of size: ${roster?.size ?: "null"}")
        }
        .catch { e ->
            android.util.Log.e("MembersViewModel", "Error collecting members", e)
            emit(emptyList())
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val isDeviceOnly: StateFlow<Boolean> = circleRepository.getCircle(circleId)
        .map { it?.isDeviceOnly ?: true }
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val inviteCode: StateFlow<String> = circleRepository.getCircle(circleId)
        .map { it?.inviteCode ?: "" }
        .stateIn(viewModelScope, SharingStarted.Lazily, "")
        
    val isCurrentUserOwner: StateFlow<Boolean> = circleRepository.getCircle(circleId)
        .map { circle ->
            val userId = authRepository.getCurrentUserId()
            circle != null && circle.creatorId == userId
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    /**
     * Add a locally-added member. Color cycles the fixed palette by roster size so each new player
     * still gets a stable tint. Write is fire-and-forget; [members] re-emits on insert.
     */
    fun addMember(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val palette = PlayerColorKey.entries
        val newMember = RosterMember(
            name = trimmed,
            initial = trimmed.take(1).uppercase(),
            colorKey = palette[members.value.orEmpty().size % palette.size],
            membershipType = MembershipType.LOCAL,
        )
        viewModelScope.launch(Dispatchers.IO) {
            playerRepository.upsertPlayer(circleId, newMember)
        }
    }

    fun onAddGuestClicked(name: String, isOnline: Boolean) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (isOnline) {
                circleRepository.addGuestMemberToOnlineCircle(circleId, trimmed)
            } else {
                addMember(trimmed)
            }
        }
    }

    fun onAddRegisteredUserClicked(email: String, isOnline: Boolean) {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (isOnline) {
                circleRepository.addRegisteredMemberToOnlineCircle(circleId, trimmed)
            } else {
                addMember(trimmed)
            }
        }
    }

    /** Remove every selected member (by name or ID) from this circle. */
    fun removeMembers(selectedNamesOrIds: Set<String>) {
        val toRemove = members.value.orEmpty().filter { it.name in selectedNamesOrIds || it.id in selectedNamesOrIds }
        
        viewModelScope.launch(Dispatchers.IO) {
            val isOnline = !isDeviceOnly.value
            
            if (isOnline) {
                toRemove.forEach { member ->
                    if (member.id.isNotBlank()) {
                        val result = circleRepository.removeMemberFromOnlineCircle(circleId, member.id)
                        if (result.isFailure) {
                            android.util.Log.e("MembersViewModel", "Failed to remove member ${member.id}", result.exceptionOrNull())
                        }
                    }
                }
            } else {
                toRemove.forEach { playerRepository.deletePlayer(circleId, it) }
            }
        }
    }
}
