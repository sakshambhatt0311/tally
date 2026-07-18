package com.tally.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.Circle
import com.tally.app.data.MembershipType
import com.tally.app.data.PlayerColorKey
import com.tally.app.data.RosterMember
import com.tally.app.di.ApplicationScope
import com.tally.app.domain.repository.AuthRepository
import com.tally.app.domain.repository.CircleRepository
import com.tally.app.domain.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.UUID
import javax.inject.Inject

/** Backs My Circles — the full circle list, streamed live from Room instead of static MockData. */
@HiltViewModel
class CirclesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val circleRepository: CircleRepository,
    private val playerRepository: PlayerRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    // null = still loading the first DB emission; a non-null empty list = genuinely no circles.
    @OptIn(ExperimentalCoroutinesApi::class)
    val circles: StateFlow<List<Circle>?> = authRepository.observeAuthState()
        .flatMapLatest { authUser ->
            val localCircles = circleRepository.getCircles()
            if (authUser != null && !authUser.isAnonymous) {
                val onlineCircles = circleRepository.observeOnlineCircles(authUser.uid)
                kotlinx.coroutines.flow.combine(localCircles, onlineCircles) { local, online ->
                    local + online
                }
            } else {
                localCircles
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Create a circle from the New Circle form. [isOnline] false => local/device-only.
     */
    suspend fun createCircle(name: String, isOnline: Boolean): String? {
        if (isOnline) {
            val user = authRepository.getCurrentAuthUser() ?: return null
            val result = circleRepository.createOnlineCircle(name.trim(), user.uid, user.email)
            return result.getOrNull()
        } else {
            val id = UUID.randomUUID().toString()
            val newCircle = Circle(
                id = id,
                name = name.trim(),
                members = emptyList(),
                memberCount = 0,
                activityLabel = "New circle",
                membershipType = MembershipType.OWNER,
                isDeviceOnly = true,
            )
            val creator = RosterMember(
                name = "You",
                initial = "Y",
                colorKey = PlayerColorKey.BLUE,
                membershipType = MembershipType.OWNER,
            )
            // Local writes are fast enough to just do in this suspend function
            circleRepository.upsertCircle(newCircle)
            playerRepository.upsertPlayer(id, creator)
            return id
        }
    }

    /** Rename a circle (upsert on same id = update). [circles] re-emits on write. */
    fun renameCircle(circle: Circle, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedCircle = circle.copy(name = trimmed)
            if (!circle.isDeviceOnly) {
                circleRepository.updateOnlineCircle(updatedCircle)
            } else {
                circleRepository.upsertCircle(updatedCircle)
            }
        }
    }

    /**
     * Delete a circle. Room's FK(onDelete = CASCADE) removes its players/games/sessions too, and
     * every affected Flow re-emits automatically — no manual state cleanup.
     */
    fun deleteCircle(circle: Circle) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!circle.isDeviceOnly) {
                circleRepository.deleteOnlineCircle(circle.id)
            } else {
                circleRepository.deleteCircle(circle)
            }
        }
    }
}
