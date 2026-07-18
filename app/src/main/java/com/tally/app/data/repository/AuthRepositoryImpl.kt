package com.tally.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.tally.app.domain.model.AuthUser
import com.tally.app.domain.model.User
import com.tally.app.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val _authState = kotlinx.coroutines.flow.MutableStateFlow<AuthUser?>(null)

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _authState.value = getCurrentAuthUser()
        }
        
        firebaseAuth.currentUser?.let { user ->
            if (!user.isAnonymous) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    saveUserToFirestore(user)
                }
            }
        }
    }

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    override fun getCurrentAuthUser(): AuthUser? {
        val user = firebaseAuth.currentUser
        return if (user != null) {
            AuthUser(
                uid = user.uid, 
                isAnonymous = user.isAnonymous, 
                email = user.getBestEmail(), 
                displayName = user.getBestDisplayName(), 
                photoUrl = user.getBestPhotoUrl()
            )
        } else {
            null
        }
    }

    override suspend fun signInAnonymously(): Result<String> {
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val uid = authResult.user?.uid
            if (uid != null) {
                Result.success(uid)
            } else {
                Result.failure(Exception("Sign-in succeeded but UID is null."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun linkWithGoogle(idToken: String): Result<Unit> {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        return try {
            val user = firebaseAuth.currentUser
            
            val authResult = if (user != null && user.isAnonymous) {
                user.linkWithCredential(credential).await()
            } else {
                firebaseAuth.signInWithCredential(credential).await()
            }
            
            authResult.user?.let { saveUserToFirestore(it) }
            
            Result.success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            try {
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                authResult.user?.let { saveUserToFirestore(it) }
                Result.success(Unit)
            } catch (signInEx: Exception) {
                Result.failure(signInEx)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveUserToFirestore(firebaseUser: FirebaseUser) {
        try {
            val docRef = firestore.collection("users").document(firebaseUser.uid)
            val snapshot = docRef.get().await()
            
            val bestNameFromGoogle = firebaseUser.getBestDisplayName()
            val bestPhotoFromGoogle = firebaseUser.getBestPhotoUrl()
            val bestEmailFromGoogle = firebaseUser.getBestEmail()

            if (snapshot.exists()) {
                // Read-first: preserve existing edits by syncing Firestore -> FirebaseAuth
                val existingUser = snapshot.toObject(User::class.java)
                val existingName = existingUser?.displayName?.takeIf { it.isNotBlank() }
                
                // If the user document existed but had no name, apply the new Google token name.
                val finalName = existingName ?: bestNameFromGoogle
                
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = finalName
                    photoUri = bestPhotoFromGoogle?.let { android.net.Uri.parse(it) }
                }
                firebaseUser.updateProfile(profileUpdates).await()
                
                // Ensure Firestore is patched with the final name if it was previously missing
                val updates = mutableMapOf<String, Any?>()
                bestPhotoFromGoogle?.let { updates["photoUrl"] = it }
                if (existingName == null && finalName != null) {
                    updates["displayName"] = finalName
                }
                
                if (updates.isNotEmpty()) {
                    firestore.collection("users").document(firebaseUser.uid).update(updates).await()
                }
                    
                Log.d("AuthRepo", "Preserved custom name, refreshed Google photo/name from Firestore for: ${firebaseUser.uid}")
            } else {
                // New document: write Google token data to Firestore
                val user = User(
                    uid = firebaseUser.uid,
                    email = bestEmailFromGoogle,
                    displayName = bestNameFromGoogle,
                    photoUrl = bestPhotoFromGoogle
                )
                
                // Ensure local FirebaseAuth cache is immediately hydrated with these values
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = bestNameFromGoogle
                    photoUri = bestPhotoFromGoogle?.let { android.net.Uri.parse(it) }
                }
                firebaseUser.updateProfile(profileUpdates).await()
                
                docRef.set(user, SetOptions.merge()).await()
                Log.d("AuthRepo", "Successfully synced new user to Firestore: ${firebaseUser.uid}")
            }
            
            // Force a token reload to guarantee FirebaseAuth holds the new displayName and photoUrl internally
            firebaseUser.reload().await()
            
            // Emit the fully hydrated user back to the active state flow so the Profile UI recomposes instantly
            _authState.value = getCurrentAuthUser()
            
        } catch (e: Exception) {
            Log.e("AuthRepo", "Failed to sync user to Firestore", e)
        }
    }

    override suspend fun updateUserProfile(newName: String): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(Exception("No user logged in"))
        return try {
            // Update Firestore
            val updates = mapOf(
                "displayName" to newName
            )
            firestore.collection("users").document(user.uid).update(updates).await()

            // Update local FirebaseAuth session so state flows emit the new data
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                displayName = newName
            }
            user.updateProfile(profileUpdates).await()
            
            // Force a token reload to trigger the AuthStateListener in observeAuthState
            user.reload().await()
            
            // Force immediate UI update
            _authState.value = getCurrentAuthUser()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Failed to update user profile", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        signInAnonymously() // Revert to a safe, fresh anonymous state
    }

    override fun observeAuthState(): Flow<AuthUser?> = _authState

    private fun FirebaseUser.getBestDisplayName(): String? {
        return this.displayName?.takeIf { it.isNotBlank() } 
            ?: this.providerData.firstOrNull { it.providerId == "google.com" }?.displayName?.takeIf { it.isNotBlank() }
    }

    private fun FirebaseUser.getBestPhotoUrl(): String? {
        return this.photoUrl?.toString()?.takeIf { it.isNotBlank() } 
            ?: this.providerData.firstOrNull { it.providerId == "google.com" }?.photoUrl?.toString()?.takeIf { it.isNotBlank() }
    }

    private fun FirebaseUser.getBestEmail(): String? {
        return this.email?.takeIf { it.isNotBlank() } 
            ?: this.providerData.firstOrNull { it.providerId == "google.com" }?.email?.takeIf { it.isNotBlank() }
    }
}
