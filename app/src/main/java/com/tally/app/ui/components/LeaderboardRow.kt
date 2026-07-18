package com.tally.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tally.app.ui.theme.MedalBronze
import com.tally.app.ui.theme.MedalGold
import com.tally.app.ui.theme.MedalSilver
import com.tally.app.ui.theme.StreakFlame

/**
 * One leaderboard row — uniform sizing for every rank (40dp rank circle + avatar, name
 * titleMedium SemiBold). Only the rank circle's color distinguishes the top 3.
 */
@Composable
fun LeaderboardRow(
    rank: Int,
    name: String,
    statSummary: String,
    avatarInitial: String,
    avatarColor: Color,
    hasStreak: Boolean,
    modifier: Modifier = Modifier,
    streakCount: Int = 0,
    photoUrl: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RankIndicator(rank = rank, size = 40.dp)
        PlayerAvatar(initial = avatarInitial, backgroundColor = avatarColor, size = 40.dp, photoUrl = photoUrl)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = statSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasStreak) {
            StreakIndicator(streakCount = streakCount)
        }
    }
}

/** Consistent circular rank badge — gold/silver/bronze for the top 3, muted surface for the rest. */
@Composable
private fun RankIndicator(rank: Int, size: Dp, modifier: Modifier = Modifier) {
    val background: Color
    val foreground: Color
    when (rank) {
        1 -> { background = MedalGold; foreground = Color.White }
        2 -> { background = MedalSilver; foreground = Color.White }
        3 -> { background = MedalBronze; foreground = Color.White }
        else -> {
            background = MaterialTheme.colorScheme.surfaceVariant
            foreground = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = foreground,
        )
    }
}

/** Minimal flame + count, no pill — a sleek accent for whoever's on an active streak. */
@Composable
private fun StreakIndicator(streakCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.LocalFireDepartment,
            contentDescription = "On a $streakCount-game streak",
            tint = StreakFlame,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = streakCount.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = StreakFlame,
        )
    }
}
