package com.tally.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.preferences.ThemeConfig
import com.tally.app.data.preferences.UserPreferencesRepository
import android.util.Log
import com.tally.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Top-level app init state — gates the launch splash. Stays [Loading] until DataStore yields the
 * first persisted theme, then flips to [Success] carrying it so the app renders the right theme with
 * no light↔dark flash.
 */
sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState
    data class Success(val theme: ThemeConfig) : MainActivityUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    // Initialization block removed to prevent redundant anonymous sign-in races.
    // AuthViewModel handles this safely based on AuthUser state.

    /**
     * Eagerly started so the DataStore read begins at Activity construction and [uiState].value
     * progresses to [Success] even though the splash's keep-on-screen check only reads `.value`
     * (a synchronous read is not a Flow subscriber, so WhileSubscribed would never fire here).
     */
    val uiState: StateFlow<MainActivityUiState> = userPreferencesRepository.themeConfig
        .map { MainActivityUiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainActivityUiState.Loading)
}
