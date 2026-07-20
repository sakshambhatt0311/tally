package com.tally.app.ui.screens

import com.tally.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.tally.app.ui.viewmodel.AuthUiState
import com.tally.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val isGoogleSignInLoading by authViewModel.isGoogleSignInLoading.collectAsStateWithLifecycle()
    val signInErrorMessage by authViewModel.signInErrorMessage.collectAsStateWithLifecycle()
    
    if (signInErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { authViewModel.setSignInErrorMessage(null) },
            title = { Text("Sign In Error") },
            text = { Text(signInErrorMessage!!) },
            confirmButton = {
                TextButton(onClick = { authViewModel.setSignInErrorMessage(null) }) {
                    Text("Dismiss")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Profile Image
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (authState is AuthUiState.GoogleLinked) {
                    val linkedState = authState as AuthUiState.GoogleLinked
                    val displayName = linkedState.displayName ?: ""
                    val photoUrl = linkedState.photoUrl

                    if (!photoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (displayName.isNotBlank()) {
                        Text(
                            text = displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // User Name and Email
            if (authState is AuthUiState.GoogleLinked) {
                val linkedState = authState as AuthUiState.GoogleLinked
                Text(
                    text = linkedState.displayName ?: "No Name",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = linkedState.email ?: "No Email",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                var showEditDialog by remember { mutableStateOf(false) }
                
                Button(
                    onClick = { showEditDialog = true },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Profile")
                }
                
                if (showEditDialog) {
                    EditProfileDialog(
                        currentName = linkedState.displayName ?: "",
                        onDismiss = { showEditDialog = false },
                        onSave = { newName ->
                            authViewModel.updateUserProfile(newName)
                            showEditDialog = false
                        }
                    )
                }
            } else {
                Text(
                    text = "Not connected",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Guest Session",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Google Sign-In / Sign-Out Button
            SignInPill(
                isLinked = authState is AuthUiState.GoogleLinked,
                isLoading = isGoogleSignInLoading,
                onClick = {
                    if (authState is AuthUiState.GoogleLinked) {
                        authViewModel.signOut()
                    } else {
                        coroutineScope.launch {
                            try {
                                authViewModel.setGoogleSignInLoading(true)
                                
                                val clientId = getWebClientId(context)

                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(clientId)
                                    .build()

                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val activity = context.findActivity()
                                if (activity == null) {
                                    authViewModel.setAuthError("Could not find Activity context")
                                    return@launch
                                }
                                
                                val result = credentialManager.getCredential(
                                    request = request,
                                    context = activity,
                                )
                                val credential = result.credential
                                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                    authViewModel.linkWithGoogle(googleIdTokenCredential.idToken)
                                } else {
                                    authViewModel.setAuthError("Received unexpected credential type.")
                                }
                            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                                // User dismissed the bottom sheet; do not show an error toast.
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                // Propagate coroutine cancellation.
                                throw e
                            } catch (e: Exception) {
                                e.printStackTrace()
                                val rawError = e.message ?: e.toString()
                                authViewModel.setSignInErrorMessage(rawError)
                                authViewModel.setAuthError(rawError)
                            } finally {
                                authViewModel.setGoogleSignInLoading(false)
                            }
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SignInPill(isLinked: Boolean, isLoading: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(50),
        color = if (isLinked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (isLinked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        if (isLoading) {
            Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    strokeWidth = 2.dp
                )
            }
        } else {
            Text(
                text = if (isLinked) "Sign Out" else "Sign In",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

private fun getWebClientId(context: android.content.Context): String =
    context.getString(R.string.default_web_client_id)

@Composable
private fun EditProfileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name) },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
