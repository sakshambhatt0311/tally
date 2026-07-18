package com.tally.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.tally.app.ui.theme.StreakFlame
import com.tally.app.ui.theme.StreakFlameContainer
import com.tally.app.ui.theme.TallyPillShape

/**
 * Shared pill-badge shell so every chip has identical shape/padding rules. Internal (not
 * private) so other screens in this module can build one-off badges — e.g. the "Owner" tag
 * and the Feed tab's "Synced"/"Queued" tag — without duplicating this Surface+Text wrapper.
 */
@Composable
internal fun TallyChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
) {
    Surface(
        modifier = modifier,
        shape = TallyPillShape,
        color = containerColor,
        contentColor = contentColor,
        border = border,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** Filled brand-container badge marking a guest with no login — Priya and Sam on the roster.
 *  Theme-aware: pastel green in light, muted forest in dark. */
@Composable
fun LocalPlayerChip(modifier: Modifier = Modifier) {
    TallyChip(
        text = "Local player",
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier,
    )
}

/** Transparent outline badge flagging a circle with no Firestore doc yet — "Solo Practice".
 *  Blends into the card in both themes instead of the old solid-white pill. */
@Composable
fun DeviceOnlyChip(modifier: Modifier = Modifier) {
    TallyChip(
        text = "This device only",
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    )
}

/**
 * Active win-streak indicator on the leaderboard — a small flame plus the streak count.
 * [streakCount] has no separate visual meaning below 1; callers should only show this
 * badge when a streak is actually active (see [LeaderboardRow]'s `hasStreak` flag).
 */
@Composable
fun StreakBadge(streakCount: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = TallyPillShape,
        color = StreakFlameContainer,
        contentColor = StreakFlame,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Canvas(modifier = Modifier.size(12.dp)) {
                drawFlame(StreakFlame)
            }
            Text(text = streakCount.toString(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Simple teardrop silhouette — pointed top, rounded base — standing in for a flame icon
 * without pulling in the full material-icons-extended artifact for a single glyph.
 */
private fun DrawScope.drawFlame(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.5f, 0f)
        cubicTo(w, h * 0.4f, w * 0.75f, h, w * 0.5f, h)
        cubicTo(w * 0.25f, h, 0f, h * 0.4f, w * 0.5f, 0f)
        close()
    }
    drawPath(path, color = color)
}
