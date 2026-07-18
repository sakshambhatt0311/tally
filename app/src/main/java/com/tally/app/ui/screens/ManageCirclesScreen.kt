package com.tally.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.app.data.Circle
import com.tally.app.data.MembershipType
import com.tally.app.ui.theme.PlusJakartaSans
import com.tally.app.ui.theme.TallyPillShape
import com.tally.app.ui.viewmodel.CirclesViewModel

/**
 * Manage Circles (stateful) — collects the live circle list from Room via [CirclesViewModel] and
 * hands it to the stateless content. Delete and Leave both remove the circle locally for now
 * (real leave-vs-delete semantics arrive with cloud sync).
 */
@Composable
fun ManageCirclesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CirclesViewModel = hiltViewModel(),
) {
    val circles by viewModel.circles.collectAsStateWithLifecycle()
    ManageCirclesContent(
        circles = circles,
        onBackClick = onBackClick,
        onDeleteCircle = viewModel::deleteCircle,
        onLeaveCircle = viewModel::deleteCircle,
        modifier = modifier,
    )
}

/**
 * Manage Circles content (stateless) — a flat list of every circle you're in, each with the one
 * destructive action that fits your membership: OWNER/LOCAL you can Delete, LINKED you can Leave.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageCirclesContent(
    circles: List<Circle>?,
    onBackClick: () -> Unit,
    onDeleteCircle: (Circle) -> Unit,
    onLeaveCircle: (Circle) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The circle awaiting a Delete/Leave confirmation, or null when no dialog is open.
    var pendingCircle by remember { mutableStateOf<Circle?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manage Circles",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = PlusJakartaSans,
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
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        when {
            // null = first DB emission pending: blank spinner, never flash "No circles found".
            circles == null -> {
                Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            circles.isEmpty() -> {
                Column(
                    modifier = contentModifier.padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "No circles found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Circles you create or join will show up here.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = PlusJakartaSans,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = contentModifier,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(circles, key = { it.id }) { circle ->
                        ManageCircleCard(
                            circle = circle,
                            onActionClick = { pendingCircle = circle },
                        )
                    }
                }
            }
        }
    }

    // Confirm before the destructive action actually fires.
    pendingCircle?.let { circle ->
        ConfirmActionDialog(
            circle = circle,
            onConfirm = {
                if (circle.membershipType == MembershipType.LINKED) onLeaveCircle(circle) else onDeleteCircle(circle)
                pendingCircle = null
            },
            onDismiss = { pendingCircle = null },
        )
    }
}

@Composable
private fun ManageCircleCard(
    circle: Circle,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = circle.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = memberCountLabel(circle.memberCount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            // OWNER/LOCAL circles are yours to Delete; LINKED circles you can only Leave.
            when (circle.membershipType) {
                MembershipType.OWNER, MembershipType.LOCAL -> CircleActionButton(
                    text = "Delete",
                    container = MaterialTheme.colorScheme.error,
                    content = MaterialTheme.colorScheme.onError,
                    onClick = onActionClick,
                )
                MembershipType.LINKED -> CircleActionButton(
                    text = "Leave",
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onActionClick,
                )
            }
        }
    }
}

/** Chunky compact pill for the per-card destructive action. */
@Composable
private fun CircleActionButton(
    text: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        // Fixed width so Delete and Leave render identically sized across cards.
        modifier = modifier
            .width(104.dp)
            .heightIn(min = 48.dp),
        shape = TallyPillShape,
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
        )
    }
}

/** Theme-aware "are you sure?" dialog for the destructive Delete/Leave action. */
@Composable
private fun ConfirmActionDialog(
    circle: Circle,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isLeave = circle.membershipType == MembershipType.LINKED
    val action = if (isLeave) "Leave" else "Delete"
    val confirmColor = if (isLeave) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
    val detail = if (isLeave) "You can rejoin later with an invite." else "This can't be undone."

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "$action this group?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
            )
        },
        text = {
            Text(
                text = "Are you sure you want to ${action.lowercase()} “${circle.name}”? $detail",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = PlusJakartaSans,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    color = confirmColor,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

private fun memberCountLabel(count: Int): String =
    if (count == 1) "1 member" else "$count members"
