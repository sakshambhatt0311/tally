package com.tally.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.SessionSummary
import com.tally.app.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the Feed tab — the circle's session timeline (newest first), plus multi-select edit mode. */
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Nav arg from CircleDashboard(circleId, ...) — inherited via the dashboard tab host's SavedStateHandle.
    private val circleId: String = checkNotNull(savedStateHandle["circleId"])

    // null = first DB emission pending (avoids an empty-state flash); non-null empty = no sessions yet.
    // Lazily keeps the generated feed lines cached for the whole Circle session (no re-map on return).
    val feed: StateFlow<List<SessionSummary>?> = sessionRepository.getFeedForCircle(circleId)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _selectedSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSessionIds: StateFlow<Set<String>> = _selectedSessionIds.asStateFlow()

    /** Enter/leave edit mode. Leaving always clears the current selection. */
    fun toggleEditMode() {
        val entering = !_isEditMode.value
        _isEditMode.value = entering
        if (!entering) _selectedSessionIds.value = emptySet()
    }

    /** Explicitly leave edit mode (Cancel / system back), clearing the selection. */
    fun exitEditMode() {
        _isEditMode.value = false
        _selectedSessionIds.value = emptySet()
    }

    /** Add/remove one session from the selection. */
    fun toggleSelection(sessionId: String) {
        _selectedSessionIds.value = _selectedSessionIds.value.let {
            if (sessionId in it) it - sessionId else it + sessionId
        }
    }

    /** Delete every selected session, then leave edit mode. [feed] re-emits on the DB change. */
    fun deleteSelected() {
        val ids = _selectedSessionIds.value.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                sessionRepository.deleteSessions(ids, circleId)
            }
        }
        exitEditMode()
    }
}
