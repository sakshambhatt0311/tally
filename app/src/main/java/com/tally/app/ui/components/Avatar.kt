package com.tally.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tally.app.ui.theme.TallyAvatarShape

/**
 * Circular profile bubble with a centered initial. Every player keeps one [backgroundColor]
 * everywhere in the app (Jordan is always blue, Alex always coral) — this single composable
 * is reused across the roster, "Who's Playing", drag-to-rank, feed, and leaderboard.
 */
@Composable
fun PlayerAvatar(
    initial: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    photoUrl: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(TallyAvatarShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        if (!photoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Text(
                text = initial.take(1).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = backgroundColor.readableTextColor(),
            )
        }
    }
}

/**
 * A darker, more saturated shade of the same hue — pairs each pastel avatar background
 * with legible initial text without needing to hand-pick a second color per player.
 */
private fun Color.readableTextColor(factor: Float = 0.45f): Color = Color(
    red = red * factor,
    green = green * factor,
    blue = blue * factor,
    alpha = alpha,
)

/**
 * Overlapping avatar stack + member count, as seen on every "My Circles" card
 * ("J A P S M · 5 members"). [initialsWithColors] may be shorter than [memberCount],
 * or even empty — "Poker Sundays" shows only the count text, no avatars at all.
 */
@Composable
fun OverlappingAvatars(
    initialsWithColors: List<Pair<String, Color>>,
    memberCount: Int,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 34.dp,
    overlap: Dp = 11.dp,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Inner row owns the overlap so the negative spacing never bleeds into the count text.
        Row(horizontalArrangement = Arrangement.spacedBy(-overlap)) {
            initialsWithColors.forEach { (initial, color) ->
                PlayerAvatar(
                    initial = initial,
                    backgroundColor = color,
                    // 2dp ring in the card's own surface color = clean cutout that blends in
                    // both light and dark (white on light cards, green-charcoal on dark).
                    modifier = Modifier.border(width = 2.dp, color = MaterialTheme.colorScheme.surface, shape = TallyAvatarShape),
                    size = avatarSize,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "$memberCount member${if (memberCount == 1) "" else "s"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
