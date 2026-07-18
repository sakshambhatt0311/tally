package com.tally.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterNone
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.SportsMartialArts
import androidx.compose.material.icons.rounded.Style
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.tally.app.data.GameId
import com.tally.app.data.PlayerColorKey
import com.tally.app.ui.theme.AvatarBlueContainer
import com.tally.app.ui.theme.AvatarCoralContainer
import com.tally.app.ui.theme.AvatarPurpleContainer
import com.tally.app.ui.theme.AvatarTealContainer
import com.tally.app.ui.theme.AvatarYellowContainer
import com.tally.app.ui.theme.GameCatan
import com.tally.app.ui.theme.GameCatanContainer
import com.tally.app.ui.theme.GamePoker
import com.tally.app.ui.theme.GamePokerContainer
import com.tally.app.ui.theme.GameSmash
import com.tally.app.ui.theme.GameSmashContainer
import com.tally.app.ui.theme.GameUno
import com.tally.app.ui.theme.GameUnoContainer

/**
 * Maps the plain-Kotlin mock/domain identities ([PlayerColorKey], [GameId]) to actual
 * Compose [Color]/[GameShape] values. Kept out of `com.tally.app.data` on purpose — data
 * models shouldn't depend on `androidx.compose.ui`, only the UI layer should know how an
 * identity is rendered.
 */
fun PlayerColorKey.toAvatarColor(): Color = when (this) {
    PlayerColorKey.BLUE -> AvatarBlueContainer
    PlayerColorKey.CORAL -> AvatarCoralContainer
    PlayerColorKey.PURPLE -> AvatarPurpleContainer
    PlayerColorKey.TEAL -> AvatarTealContainer
    PlayerColorKey.YELLOW -> AvatarYellowContainer
}

fun GameId.toShape(): GameShape = when (this) {
    GameId.CATAN -> GameShape.HEXAGON
    GameId.POKER -> GameShape.SPADE
    GameId.SMASH -> GameShape.STAR
    GameId.UNO -> GameShape.SQUARE
}

fun GameId.toTintColor(): Color = when (this) {
    GameId.CATAN -> GameCatan
    GameId.POKER -> GamePoker
    GameId.SMASH -> GameSmash
    GameId.UNO -> GameUno
}

/**
 * Thematic Material icon per game — single source of truth for the whole app. Render it colorful
 * ([toTintColor], i.e. the brand color) on Feed/Games cards, or monochrome on detail headers.
 */
fun GameId.toIcon(): ImageVector = when (this) {
    GameId.CATAN -> Icons.Rounded.Hexagon
    GameId.POKER -> Icons.Rounded.Style
    GameId.SMASH -> Icons.Rounded.SportsMartialArts
    GameId.UNO -> Icons.Rounded.FilterNone
}

/** Soft pastel fill behind a game's glyph — used by the "Pick a Game" selection tiles. */
fun GameId.toContainerColor(): Color = when (this) {
    GameId.CATAN -> GameCatanContainer
    GameId.POKER -> GamePokerContainer
    GameId.SMASH -> GameSmashContainer
    GameId.UNO -> GameUnoContainer
}

/** Display name shown throughout the Games tab, Feed cards, and Head-to-Head breakdown. */
fun GameId.displayName(): String = when (this) {
    GameId.CATAN -> "Catan"
    GameId.POKER -> "Poker"
    GameId.SMASH -> "Smash Bros"
    GameId.UNO -> "Uno"
}
