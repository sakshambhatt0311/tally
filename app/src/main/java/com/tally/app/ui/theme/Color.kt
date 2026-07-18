package com.tally.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Brand — the two greens that appear on nearly every screen
// ---------------------------------------------------------------------------

/** Solid brand green — "Continue Solo", the Circles FAB, "Create Circle", "Join". */
val TallyGreen = Color(0xFF1B7A46)
/** Pressed/emphasis state for the solid green. */
val TallyGreenPressed = Color(0xFF155E37)

/** Pastel green container — "Sign in to Sync", "Add Players Instead", "Add a Game" pill. */
val TallyGreenContainer = Color(0xFFD3EFDC)
/** Text/icon drawn on the pastel green container (e.g. "Local player" / "This device only" chips). */
val TallyOnGreenContainer = Color(0xFF1B5E3A)

// ---------------------------------------------------------------------------
// Neutrals — backgrounds, surfaces, text
// ---------------------------------------------------------------------------

/** Warm off-white app background behind list screens (My Circles, Feed, Board). */
val TallyBackground = Color(0xFFF4F5EF)
/** Card / list-row / bottom-sheet surface — clean white. */
val TallySurface = Color(0xFFFFFFFF)
/** Subtle surface used for input field fills, dividers, unfilled progress track. */
val TallySurfaceVariant = Color(0xFFE9EAE3)
/** Primary text color — near-black, used for titles like "My Circles", player names. */
val TallyOnSurface = Color(0xFF1A1B18)
/** Muted secondary text — timestamps ("2h ago"), helper copy, eyebrow labels. */
val TallyOnSurfaceVariant = Color(0xFF6F7268)
/** Hairline border color for outlined text fields (Circle Name, Share Code). */
val TallyOutline = Color(0xFFD7D9CF)
/** Softer hairline for dividers between list rows — recedes more than [TallyOutline]. */
val TallyOutlineVariant = Color(0xFFE6E7DE)

// MD3 tonal surface-container ramp. Depth comes from these color steps, NOT shadows —
// screen sits on a low container, cards lift by being pure white ([TallySurface]).
val TallySurfaceContainerLowest = Color(0xFFFFFFFF) // pure white — cards, sheets
val TallySurfaceContainerLow = Color(0xFFFAFBF5)    // subtle raised fills
val TallySurfaceContainer = Color(0xFFF1F2EC)       // default container tone
val TallySurfaceContainerHigh = Color(0xFFEBECE4)   // pressed / highlighted rows
val TallySurfaceContainerHighest = Color(0xFFE5E6DD) // strongest neutral fill

/** Dark navy from the hero/marketing header block — reserved for a future dark-hero section. */
val TallyNavy = Color(0xFF15172B)

// ---------------------------------------------------------------------------
// Player avatar palette — one fixed color per player, reused everywhere
// (roster rows, "Who's Playing", drag-to-rank, feed, leaderboard, head-to-head)
// ---------------------------------------------------------------------------

/** Jordan — avatar fill + initial color. */
val AvatarBlueContainer = Color(0xFFCFDBFA)
val AvatarBlueOnContainer = Color(0xFF3E5FC4)

/** Alex — avatar fill + initial color. */
val AvatarCoralContainer = Color(0xFFF7D6D2)
val AvatarCoralOnContainer = Color(0xFFD2695C)

/** Priya — avatar fill + initial color. */
val AvatarPurpleContainer = Color(0xFFE4D6F7)
val AvatarPurpleOnContainer = Color(0xFF8A5CC7)

/** Sam — avatar fill + initial color. */
val AvatarTealContainer = Color(0xFFCDEEE0)
val AvatarTealOnContainer = Color(0xFF2E9C7C)

/** Maya — avatar fill + initial color. */
val AvatarYellowContainer = Color(0xFFF8E3B8)
val AvatarYellowOnContainer = Color(0xFFC9932E)

// ---------------------------------------------------------------------------
// Game brand colors — each game keeps one identity across Feed, Board, and
// session-logging screens (glyph background + glyph tint)
// ---------------------------------------------------------------------------

/** Catan — hexagon glyph, gold/amber identity. */
val GameCatan = Color(0xFFE3A73B)
val GameCatanContainer = Color(0xFFF7E4BE)

/** Poker — spade glyph, deep red identity. */
val GamePoker = Color(0xFF9E2B2B)
val GamePokerContainer = Color(0xFFF6D4D2)

/** Smash Bros — star glyph, blue identity. */
val GameSmash = Color(0xFF3462C9)
val GameSmashContainer = Color(0xFFD8E1F8)

/** Uno — target glyph, near-black/dark identity (its "color" is deliberately neutral). */
val GameUno = Color(0xFF1B1B1F)
val GameUnoContainer = Color(0xFFE4E4E6)

// ---------------------------------------------------------------------------
// Status accents — leaderboard medals and the active win-streak badge
// ---------------------------------------------------------------------------

/** Rank 1/2/3 medal fills on the Board tab leaderboard; rank 4+ gets no medal color. */
val MedalGold = Color(0xFFC9962A)
val MedalSilver = Color(0xFFA6AAAE)
val MedalBronze = Color(0xFFB0723C)

/** Warm flame accent for the active-streak badge — deliberately its own token, not reused
 *  from a specific game's brand color, since a streak isn't tied to any one game. */
val StreakFlame = Color(0xFFE0912F)
val StreakFlameContainer = Color(0xFFF7E6C4)

// ---------------------------------------------------------------------------
// Dark theme — flat, zero-elevation. Depth from color contrast only, never shadow.
// ---------------------------------------------------------------------------

/** Very dark forest-green canvas (never pure black), distinctly green-tinted. */
val DarkBackground = Color(0xFF111A15)
/** Cards, text fields, sheets — a green-tinted charcoal, one step lighter than the canvas. */
val DarkSurface = Color(0xFF1B241F)
val DarkSurfaceVariant = Color(0xFF212B26)
val DarkOnSurface = Color(0xFFFFFFFF)          // primary text — pure white
val DarkOnSurfaceVariant = Color(0xFF9BA79E)   // subtitles, metadata, helper copy (green-gray)
val DarkOutline = Color(0xFF334139)            // hairline borders on dark surfaces
val DarkOutlineVariant = Color(0xFF212B26)     // row dividers

/** Bright mint primary — "Continue Solo", FAB. High contrast against the dark canvas. */
val BrandMint = Color(0xFF58D690)
val OnBrandMint = Color(0xFF003820)
/** Muted forest secondary container — "Sign in to Sync", pastel chips inverted. */
val BrandDarkGreen = Color(0xFF233B30)
val OnBrandDarkGreen = Color(0xFFDDFBE9)
