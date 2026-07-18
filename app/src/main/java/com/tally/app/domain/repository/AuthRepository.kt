    package com.tally.app.domain.repository

import com.tally.app.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUserId(): String?
    fun getCurrentAuthUser(): AuthUser?
    suspend fun signInAnonymously(): Result<String>
    suspend fun linkWithGoogle(idToken: String): Result<Unit>
    suspend fun signOut()
    fun observeAuthState(): Flow<AuthUser?>
    suspend fun updateUserProfile(newName: String): Result<Unit>
}
