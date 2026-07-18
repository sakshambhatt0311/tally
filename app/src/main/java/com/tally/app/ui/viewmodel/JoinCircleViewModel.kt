package com.tally.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.domain.repository.AuthRepository
import com.tally.app.domain.repository.CircleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class JoinCircleUiState {
    object Idle : JoinCircleUiState()
    object Loading : JoinCircleUiState()
    data class Success(val circleId: String, val circleName: String) : JoinCircleUiState()
    data class Error(val message: String) : JoinCircleUiState()
}

@HiltViewModel
class JoinCircleViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val circleRepository: CircleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<JoinCircleUiState>(JoinCircleUiState.Idle)
    val uiState: StateFlow<JoinCircleUiState> = _uiState.asStateFlow()

    fun joinCircle(inviteCode: String) {
        if (inviteCode.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = JoinCircleUiState.Loading
            
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.value = JoinCircleUiState.Error("You must be logged in to join a circle.")
                return@launch
            }
            
            val result = circleRepository.joinCircleByCode(inviteCode, userId)
            
            result.onSuccess { circleId ->
                // Provide a generic name or fetch it if needed; the dashboard will load the actual name
                _uiState.value = JoinCircleUiState.Success(circleId, "Circle")
            }.onFailure { exception ->
                _uiState.value = JoinCircleUiState.Error(exception.message ?: "An unknown error occurred.")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = JoinCircleUiState.Idle
    }
}
