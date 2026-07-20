package com.tally.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.tally.app.R
import com.tally.app.ui.components.TallyPrimaryButton
import com.tally.app.ui.components.TallySecondaryButton
import com.tally.app.ui.viewmodel.AuthUiState
import com.tally.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/**
 * The Get Started fork: both paths reach the identical app, signing in only adds sync
 * on top of what already works offline (see [TallyPrimaryButton]/[TallySecondaryButton]).
 */
@Composable
fun WelcomeScreen(
    onContinueSolo: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    val linkedState by authViewModel.linkedState.collectAsStateWithLifecycle()
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val isGoogleSignInLoading by authViewModel.isGoogleSignInLoading.collectAsStateWithLifecycle()
    val signInErrorMessage by authViewModel.signInErrorMessage.collectAsStateWithLifecycle()

    if (signInErrorMessage != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { authViewModel.setSignInErrorMessage(null) },
            title = { Text("Sign In Error") },
            text = { Text(signInErrorMessage!!) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { authViewModel.setSignInErrorMessage(null) }) {
                    Text("Dismiss")
                }
            }
        )
    }

    LaunchedEffect(linkedState) {
        if (linkedState is AuthUiState.GoogleLinked) {
            onContinueSolo()
        } else if (linkedState is AuthUiState.Error) {
            android.widget.Toast.makeText(
                context, 
                (linkedState as AuthUiState.Error).message, 
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Real app logo drawable (colors baked in) — Image, not Icon/Canvas, so its green bars and
        // gold slash aren't flattened to a single theme tint. Fit + fixed size = no stretch.
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Tally logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(120.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tally",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Score every game night. Automatically.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))

        TallyPrimaryButton(text = "Continue Solo", onClick = onContinueSolo)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Track games on this device, add friends later",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        if (linkedState is AuthUiState.Loading || uiState is AuthUiState.Loading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Preparing...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (linkedState is AuthUiState.GoogleLinked || uiState is AuthUiState.GoogleLinked) {
            val userEmail = (linkedState as? AuthUiState.GoogleLinked)?.email 
                ?: (uiState as? AuthUiState.GoogleLinked)?.email
            Text(
                text = "Synced as ${userEmail ?: "Linked User"}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            TallySecondaryButton(
                text = "Sign in to Sync", 
                isLoading = isGoogleSignInLoading,
                onClick = {
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
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Share stats live across your circle",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun getWebClientId(context: android.content.Context): String =
    context.getString(R.string.default_web_client_id)

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
