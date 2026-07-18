package com.tally.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data class Anonymous(val uid: String) : AuthUiState
    data class GoogleLinked(val uid: String, val email: String?, val displayName: String? = null, val photoUrl: String? = null) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    init {
        if (authRepository.getCurrentAuthUser() == null) {
            viewModelScope.launch {
                authRepository.signInAnonymously()
            }
        }
    }

    val uiState: StateFlow<AuthUiState> = authRepository.observeAuthState()
        .map { authUser ->
            if (authUser == null) {
                AuthUiState.Loading
            } else if (authUser.isAnonymous) {
                AuthUiState.Anonymous(authUser.uid)
            } else {
                AuthUiState.GoogleLinked(authUser.uid, authUser.email, authUser.displayName, authUser.photoUrl)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.getCurrentAuthUser().let { authUser ->
                if (authUser == null) {
                    AuthUiState.Loading
                } else if (authUser.isAnonymous) {
                    AuthUiState.Anonymous(authUser.uid)
                } else {
                    AuthUiState.GoogleLinked(authUser.uid, authUser.email, authUser.displayName, authUser.photoUrl)
                }
            }
        )

    // Using a simple state to track linked status for the UI specifically
    private val _linkedState = kotlinx.coroutines.flow.MutableStateFlow<AuthUiState>(AuthUiState.Anonymous(""))
    val linkedState: StateFlow<AuthUiState> = _linkedState

    private val _isGoogleSignInLoading = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isGoogleSignInLoading: StateFlow<Boolean> = _isGoogleSignInLoading

    private val _signInErrorMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val signInErrorMessage: StateFlow<String?> = _signInErrorMessage

    fun setSignInErrorMessage(message: String?) {
        _signInErrorMessage.value = message
    }

    fun setGoogleSignInLoading(isLoading: Boolean) {
        _isGoogleSignInLoading.value = isLoading
    }

    fun linkWithGoogle(idToken: String) {
        viewModelScope.launch {
            _linkedState.value = AuthUiState.Loading
            val result = authRepository.linkWithGoogle(idToken)
            if (result.isSuccess) {
                val user = authRepository.getCurrentUserId() ?: ""
                _linkedState.value = AuthUiState.GoogleLinked(user, null)
            } else {
                val exception = result.exceptionOrNull()
                android.util.Log.e("AuthViewModel", "Google Sign-In failed", exception)
                _linkedState.value = AuthUiState.Error(exception?.message ?: "Unknown error")
            }
        }
    }

    fun setAuthError(message: String) {
        _linkedState.value = AuthUiState.Error(message)
    }

    fun signOut() {
        viewModelScope.launch {
            _linkedState.value = AuthUiState.Loading
            authRepository.signOut()
            _linkedState.value = AuthUiState.Anonymous("") // Simple fallback state
        }
    }
    fun updateUserProfile(newName: String) {
        viewModelScope.launch {
            authRepository.updateUserProfile(newName)
        }
    }
}
