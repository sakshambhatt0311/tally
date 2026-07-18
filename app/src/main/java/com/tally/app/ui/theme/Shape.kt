package com.tally.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * M3 corner scale used for standard components (text fields, chips, sheets).
 * Rows/list-items lean on `medium`; the "New Circle" text field and bottom
 * sheet ("Add a Player") lean on `large`.
 */
val TallyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Fully rounded "stadium" shape for the solid/pastel pill buttons — Continue Solo, Join, Add a Game. */
val TallyPillShape: Shape = RoundedCornerShape(50)

/** Rounded-rectangle card shape for circle rows, session/feed cards, and the leaderboard rows. */
val TallyCardShape: Shape = RoundedCornerShape(16.dp)

/** Circular shape for player avatars and the "+" FAB. */
val TallyAvatarShape: Shape = CircleShape
