package com.tally.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tally.app.data.Circle
import com.tally.app.ui.components.DeviceOnlyChip
import com.tally.app.ui.components.OverlappingAvatars
import com.tally.app.ui.components.toAvatarColor
import com.tally.app.ui.theme.TallyCardShape
import com.tally.app.ui.theme.TallyPillShape

/** Landing screen after onboarding — every circle this device knows about, local or synced. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCirclesScreen(
    circles: List<Circle>?,
    onCircleClick: (circleId: String, circleName: String) -> Unit,
    onCreateCircleClick: () -> Unit,
    onJoinCircleClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isAuthenticated: Boolean = false,
) {
    var fabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Circles",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick, modifier = Modifier.padding(end = 4.dp)) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
        floatingActionButton = {
            ExpandingFab(
                expanded = fabExpanded,
                onToggle = { fabExpanded = !fabExpanded },
                onNewCircle = {
                    fabExpanded = false
                    onCreateCircleClick()
                },
                onJoinCircle = {
                    fabExpanded = false
                    onJoinCircleClick()
                },
                isAuthenticated = isAuthenticated,
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        when {
            // null = first DB emission not in yet: stay blank, never flash the empty state.
            circles == null -> LoadingBox(contentModifier)
            circles.isEmpty() -> MyCirclesEmptyState(contentModifier)
            else -> {
                // No scrim — the canvas stays fully bright while the FAB menu is open.
                LazyColumn(
                    modifier = contentModifier,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(circles, key = { it.id }) { circle ->
                        CircleCard(circle = circle, onClick = { onCircleClick(circle.id, circle.name) })
                    }
                }
            }
        }
    }
}

/** Subtle centered spinner shown while the first DB emission is still loading. */
@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** Shown on a fresh install (empty DB) — a calm prompt pointing at the + button. */
@Composable
private fun MyCirclesEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No circles yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Tap the + button to create your first circle or join one with a code.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ExpandingFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onNewCircle: () -> Unit,
    onJoinCircle: () -> Unit,
    isAuthenticated: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(if (expanded) 45f else 0f, label = "fabRotation")

    // Tight 12dp gap between the action stack and the main FAB.
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            // 8dp between the two action buttons.
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAuthenticated) {
                    FabAction(label = "Join Circle", icon = Icons.Outlined.GroupAdd, onClick = onJoinCircle)
                }
                FabAction(label = "New Circle", icon = Icons.Filled.Add, onClick = onNewCircle)
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            modifier = Modifier.size(64.dp), // uniform size, shared by all three buttons
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = flatFabElevation(), // no shadow, ever — kills the animation flicker
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = if (expanded) "Close menu" else "Add circle",
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotation),
            )
        }
    }
}

@Composable
private fun FabAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onClick,
            shape = TallyPillShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 0.dp,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(64.dp), // identical to the main FAB → perfect vertical stack
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            elevation = flatFabElevation(),
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}

/** Zero elevation in every interaction state — no default shadow to flicker mid-animation. */
@Composable
private fun flatFabElevation() = FloatingActionButtonDefaults.elevation(
    defaultElevation = 0.dp,
    pressedElevation = 0.dp,
    focusedElevation = 0.dp,
    hoveredElevation = 0.dp,
)

@Composable
private fun CircleCard(
    circle: Circle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = TallyCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = circle.name,
                    style = MaterialTheme.typography.titleLarge, // matches dashboard game-name scale
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (circle.isDeviceOnly) {
                    DeviceOnlyChip()
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                )
            }

            if (circle.members.isNotEmpty()) {
                OverlappingAvatars(
                    initialsWithColors = circle.members.map { (initial, colorKey) -> initial to colorKey.toAvatarColor() },
                    memberCount = circle.memberCount,
                )
                Text(
                    text = circle.activityLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                val plural = if (circle.memberCount == 1) "member" else "members"
                Text(
                    text = "${circle.memberCount} $plural · ${circle.activityLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
