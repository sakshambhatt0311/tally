package com.tally.app.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tally.app.ui.theme.TallyPillShape
import com.tally.app.ui.theme.TallyShapes

/** The two entry modes of the Add Player sheet — pass-and-play locals vs. a real cross-device invite. */
enum class AddPlayerTab(val label: String) {
    LOCAL_PLAYER("Local Player"),
    INVITE_BY_CODE("Invite by Code"),
}

/**
 * Fully hoisted state for [AddPlayerBottomSheet]. Owning this in a ViewModel later means the
 * sheet survives config changes and its "recently added" list stays authoritative there, not here.
 */
data class AddPlayerSheetState(
    val selectedTab: AddPlayerTab = AddPlayerTab.LOCAL_PLAYER,
    val nameInput: String = "",
    val recentlyAdded: List<String> = emptyList(),
    val shareCode: String = "",
)

/**
 * Stateless Add Player sheet. Holds only the transient [rememberModalBottomSheetState] (pure view
 * state); every piece of business state arrives via [state] and every mutation leaves via a lambda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerBottomSheet(
    state: AddPlayerSheetState,
    onTabSelected: (AddPlayerTab) -> Unit,
    onNameChange: (String) -> Unit,
    onAddPlayerClicked: () -> Unit,
    onRemoveRecent: (String) -> Unit,
    onCopyCode: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Add a Player",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface, // was hardcoded near-black → invisible in dark
            )

            if (isOnline) {
                SegmentedTabs(selected = state.selectedTab, onSelected = onTabSelected)
                when (state.selectedTab) {
                    AddPlayerTab.LOCAL_PLAYER -> LocalPlayerMode(
                        name = state.nameInput,
                        onNameChange = onNameChange,
                        onAdd = onAddPlayerClicked,
                    )
                    AddPlayerTab.INVITE_BY_CODE -> InviteByCodeMode(
                        shareCode = state.shareCode,
                        onCopyCode = onCopyCode,
                    )
                }
            } else {
                // Offline circle: no tabs, just local player input
                LocalPlayerMode(
                    name = state.nameInput,
                    onNameChange = onNameChange,
                    onAdd = onAddPlayerClicked,
                )
            }

            if (state.recentlyAdded.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.recentlyAdded, key = { it }) { name ->
                        AddedChip(name = name, onRemove = { onRemoveRecent(name) })
                    }
                }
            }

            Text(
                text = "Local players don't need the app or an account. Great for pass-and-play.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Custom pill segmented control — theme-aware track with the active mode marked in the brand container. */
@Composable
private fun SegmentedTabs(
    selected: AddPlayerTab,
    onSelected: (AddPlayerTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(TallyPillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant) // subtle pill track (was pure pastel green)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AddPlayerTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Surface(
                onClick = { onSelected(tab) },
                modifier = Modifier.weight(1f),
                shape = TallyPillShape,
                // Selected: brand container (mint/forest). Unselected: transparent + muted text.
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun LocalPlayerMode(
    name: String,
    onNameChange: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = {
                Text("Player name", color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            singleLine = true,
            shape = TallyShapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { onAdd() }
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        // Solid primary action — was greyed/disabled-looking; now always the brand color.
        Button(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = TallyPillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(text = "Add", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InviteByCodeMode(
    shareCode: String,
    onCopyCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (shareCode.isNotBlank()) {
            Text(
                text = "Share this code with players to let them join your circle.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Surface(shape = TallyShapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = shareCode,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                    letterSpacing = 4.sp
                )
            }
            OutlinedButton(onClick = onCopyCode, shape = TallyPillShape) {
                Text("Copy")
            }
        } else {
            Text(
                text = "Code not available for this circle.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }
    }
}

/** "Added: Riley ×" — the just-added confirmation chip, theme-aware brand container. */
@Composable
private fun AddedChip(
    name: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = TallyPillShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Added: $name", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove $name",
                modifier = Modifier
                    .size(14.dp)
                    .clickable(onClick = onRemove),
            )
        }
    }
}
