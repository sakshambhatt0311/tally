package com.tally.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.tally.app.ui.viewmodel.JoinCircleViewModel
import com.tally.app.ui.viewmodel.JoinCircleUiState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tally.app.ui.components.TallyPrimaryButton

private const val SHARE_CODE_LEN = 6

/** Join a Circle — the one flow that genuinely requires connectivity up front. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinCircleScreen(
    onBackClick: () -> Unit,
    onJoined: (circleId: String, circleName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoinCircleViewModel = hiltViewModel()
) {
    var code by rememberSaveable { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(uiState) {
        if (uiState is JoinCircleUiState.Success) {
            val success = uiState as JoinCircleUiState.Success
            onJoined(success.circleId, success.circleName)
            viewModel.resetState()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Join a Circle",
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
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "SHARE CODE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = code,
                // Keep raw state to <=6 alphanumerics; the template is purely visual.
                onValueChange = { input ->
                    code = input.filter { it.isLetterOrDigit() }.uppercase().take(SHARE_CODE_LEN)
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                visualTransformation = ShareCodeVisualTransformation,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Ask a circle member for their code",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.weight(1f))

            if (uiState is JoinCircleUiState.Error) {
                Text(
                    text = (uiState as JoinCircleUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            TallyPrimaryButton(
                text = if (uiState is JoinCircleUiState.Loading) "Joining..." else "Join",
                onClick = { viewModel.joinCircle(code) },
                enabled = code.length == SHARE_CODE_LEN && uiState !is JoinCircleUiState.Loading,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            )
        }
    }
}

/**
 * Renders the raw code against a fixed 6-slot dashed template: "7 9 7 – – –".
 * Typed characters fill slots left-to-right; empty slots stay as em-dashes, so the field always
 * shows all six placeholders. Layout is fixed-width — each slot is `char + " "` (2 transformed
 * chars) except the last has no trailing space → transformed length is always 11.
 */
val ShareCodeVisualTransformation = object : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.take(SHARE_CODE_LEN)
        val rendered = (0 until SHARE_CODE_LEN).joinToString(" ") { i ->
            if (i < raw.length) raw[i].toString() else "–"
        }

        val mapping = object : OffsetMapping {
            // o typed chars → cursor sits right after slot (o-1)'s char: index 2*o-1 (0 → 0).
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 0) 0 else (2 * offset - 1).coerceAtMost(rendered.length)

            // Inverse: every 2 transformed chars = 1 original; never past the count actually typed.
            override fun transformedToOriginal(offset: Int): Int =
                ((offset + 1) / 2).coerceIn(0, raw.length)
        }

        return TransformedText(AnnotatedString(rendered), mapping)
    }
}
