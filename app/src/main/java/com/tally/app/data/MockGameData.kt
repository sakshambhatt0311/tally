package com.tally.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Castle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FilterNone
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.LocalPolice
import androidx.compose.material.icons.rounded.MapsHomeWork
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SportsMartialArts
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One selectable game in the library, with its UI identity (icon + brand color) and rules metadata.
 * Screens render whichever fields they need (Add Game = monochrome icon; Pick a Game = colorful).
 */
@Immutable
data class GameTemplate(
    val name: String,
    val icon: ImageVector,
    val brandColor: Color,
    val matchType: String,
    val primaryMetric: String,
    val secondaryMetric: String,
    val scoringRules: String,
    val scoringType: ScoringType,
    /** Short, engaging description for the Game Detail hero card. */
    val description: String = "",
    val startsAdded: Boolean = false,
    /** Noun for a score in this game — "goals", "runs", "laps"… Defaults to "points". */
    val scoringUnit: String = "points",
    val isLowerScoreBetter: Boolean = false,
) {
    /** Stable template identity. Names are unique across the catalog, so the name is the id. */
    val id: String get() = name
}

/**
 * Single source of truth for the categorized game library. Previously duplicated inside
 * AddGameScreen and the Log Session "Pick a Game" step — both now read from here.
 */
object MockGameData {

    /** Ordered category → games. LinkedHashMap keeps the category order for the UI headers. */
    val allGamesCategorized: Map<String, List<GameTemplate>> = linkedMapOf(
        "Video Games" to listOf(
            GameTemplate("FIFA", Icons.Rounded.SportsSoccer, Color(0xFF2E7D32), "H2H", "Goals", "Penalties", "Score vs. Score. Teams accumulate goals; penalties decide ties if applicable.", ScoringType.POINTS, description = "The beautiful game, digitized. Build your dream squad and settle rivalries on the virtual pitch.", scoringUnit = "goals"),
            GameTemplate("Mario Kart", Icons.Rounded.DirectionsCar, Color(0xFFD32F2F), "Free-for-all", "Placement", "Cup Points", "Placement per race or total Cup Points for a Grand Prix.", ScoringType.PLACEMENT, description = "Shell-slinging, banana-dropping chaos on rainbow-colored tracks. Friendships will be tested."),
            GameTemplate("Mario Party", Icons.Rounded.Casino, Color(0xFFEC407A), "Free-for-all", "Stars", "Coins", "Dual-currency system. Most stars win; coins serve as tie-breakers.", ScoringType.PLACEMENT, description = "A board game brawl packed with wild mini-games. Stars are king, coins break ties, and anything can happen on the last turn."),
            GameTemplate("Super Smash Bros", Icons.Rounded.SportsMartialArts, Color(0xFF1E88E5), "Free-for-all / H2H", "Stocks / Points", "Damage Dealt", "Eliminate opponents to reduce stocks, or gain points in timed mode.", ScoringType.PLACEMENT, description = "Nintendo's all-star platform fighter. Knock your rivals off the stage and claim the last stock standing.", startsAdded = true),
            GameTemplate("Football Manager", Icons.Rounded.Leaderboard, Color(0xFF00897B), "Career", "Trophies Won", "Win Percentage", "Solo/asynchronous simulation tracked by seasonal success.", ScoringType.WIN_LOSS, description = "Tactics, transfers, and the thrill of a title race. Manage your club from the lower leagues to continental glory."),
            GameTemplate("Call of Duty / Halo", Icons.Rounded.GpsFixed, Color(0xFFF4511E), "Team", "Kills / Objective Points", "Assists / Deaths", "Team-based win — pick every winner (whole team can win).", ScoringType.WIN_LOSS, description = "Squad up for fast-paced tactical FPS action. Coordinate your team, dominate the objective, and secure the win."),
            GameTemplate("Tekken", Icons.Rounded.SportsMartialArts, Color(0xFF4527A0), "H2H", "Win / Loss", "Rounds Won", "Strictly 1v1 — exactly one winner, no draws.", ScoringType.WIN_LOSS, description = "The king of fighting games. Two fighters enter, one walks away — read your opponent and settle it in a pure 1v1 duel."),
        ),
        "Board & Card Games" to listOf(
            GameTemplate("UNO (Single Winner)", Icons.Rounded.FilterNone, Color(0xFF546E7A), "Free-for-all", "Round Win", "None", "First to go out wins the round.", ScoringType.WIN_LOSS, description = "Race to empty your hand before everyone else. Draw Fours and Reverses keep the table on edge."),
            GameTemplate("UNO (Placements)", Icons.Rounded.FilterNone, Color(0xFF546E7A), "Free-for-all", "Placement", "None", "Rank everyone by finishing order (1st out to last).", ScoringType.PLACEMENT, description = "Full ranking mode — first player out takes the crown, and everyone else gets ranked by finishing order."),
            GameTemplate("Catan", Icons.Rounded.Hexagon, Color(0xFFE3A73B), "Free-for-all", "Victory Points", "None", "Race to a target. First to 10 Victory Points wins.", ScoringType.WIN_LOSS, description = "Trade, build, and settle the island. Brick and wheat are your best friends — or your worst enemies.", startsAdded = true),
            GameTemplate("Ludo", Icons.Rounded.GridView, Color(0xFF8E24AA), "Free-for-all", "Placement", "None", "Race to finish. Tracked by 1st through 4th place.", ScoringType.PLACEMENT, description = "A classic race around the board. Roll the dice, dodge opponents, and get all your pieces home first."),
            GameTemplate("Poker (Tournament)", Icons.Rounded.Style, Color(0xFF9E2B2B), "Free-for-all", "Placement", "None", "Tracked by order of elimination.", ScoringType.PLACEMENT, description = "All-in or fold — outlast the table in a high-stakes elimination format. Last player standing wins it all.", startsAdded = true),
            GameTemplate("Poker (Cash Games)", Icons.Rounded.Style, Color(0xFF2E7D32), "Free-for-all", "Net Profit/Loss", "None", "Tracked by financial net at the end of the session (+/-).", ScoringType.POINTS, description = "Play for chips and track the profit. Read your opponents, manage your bankroll, and leave the table up.", startsAdded = true),
            GameTemplate("Chess", Icons.Rounded.Castle, Color(0xFF6D4C41), "H2H", "Win / Loss / Draw", "None", "1 point for win, 0.5 for draw, 0 for loss.", ScoringType.WIN_LOSS, description = "The ultimate test of strategy and foresight. Checkmate your opponent or fight to a hard-earned draw."),
            GameTemplate("Monopoly", Icons.Rounded.MapsHomeWork, Color(0xFF43A047), "Free-for-all", "Placement / Net Worth", "Properties Owned", "Order of bankruptcy, or total net worth at a time limit.", ScoringType.PLACEMENT, description = "Buy, trade, and build your property empire. Watch your friends go bankrupt one hotel at a time."),
            GameTemplate("Scotland Yard", Icons.Rounded.LocalPolice, Color(0xFF37474F), "Team", "Win / Loss", "None", "Detectives vs. Mr. X — mark the winning side (one or more players).", ScoringType.WIN_LOSS, description = "Mr. X versus the detectives. Hunt the fugitive across the map, or slip away undetected into the night."),
        ),
        "Outdoor & Sports Games" to listOf(
            GameTemplate("Table Tennis", Icons.Rounded.SportsTennis, Color(0xFFFB8C00), "H2H", "Sets Won", "Total Points", "Points vs. Points to 11 (win by 2). Best of 3/5 sets.", ScoringType.POINTS, description = "Lightning-fast rallies across the net. Spin, smash, and serve your way to 11 points."),
            GameTemplate("Badminton", Icons.Rounded.SportsTennis, Color(0xFF7CB342), "H2H", "Sets Won", "Total Points", "Points vs. Points to 21 (win by 2, cap at 30).", ScoringType.POINTS, description = "Agility meets finesse in this fast-paced court sport. Smash the shuttlecock past your opponent to claim the rally."),
            GameTemplate("Tennis", Icons.Rounded.SportsTennis, Color(0xFFF9A825), "H2H", "Sets Won", "Games Won", "Track sets won (e.g., 2-1) and games within sets.", ScoringType.POINTS, description = "Grand Slam drama in your backyard. Aces, volleys, and match point — every set tells a story."),
            GameTemplate("Pool", Icons.Rounded.TripOrigin, Color(0xFF455A64), "Team / H2H", "Win / Loss", "None", "Pick every winner — supports doubles/team wins.", ScoringType.WIN_LOSS, description = "Team up or go solo in this classic 8-ball showdown. Sink your group and pocket the 8 to seal the deal."),
            GameTemplate("Billiards", Icons.Rounded.TripOrigin, Color(0xFF00695C), "H2H", "Win / Loss", "None", "Strictly 1v1 — exactly one winner per frame.", ScoringType.WIN_LOSS, description = "A classic cue sport of precision and angles. Clear the table frame by frame in a pure 1v1 battle of skill."),
        ),
        "Conversational & Party Games" to listOf(
            GameTemplate("Codenames", Icons.Rounded.GridOn, Color(0xFF3F51B5), "Team", "Win / Loss", "Assassin Hit", "First team to find all agents wins. Hitting the assassin is an instant loss.", ScoringType.WIN_LOSS, description = "Give one-word clues to guide your team to the right agents. But choose carefully — the assassin lurks on the board."),
            GameTemplate("Mafia", Icons.Rounded.TheaterComedy, Color(0xFF5D4037), "Team", "Faction Win / Loss", "Role Played", "Villagers vs. Mafia faction win. Can track specific roles.", ScoringType.WIN_LOSS, description = "Deception, deduction, and dramatic accusations. Root out the Mafia before they eliminate the village — or bluff your way to victory."),
            GameTemplate("Contact", Icons.Rounded.RecordVoiceOver, Color(0xFF00838F), "Team", "Win / Loss", "None", "Guesser vs. Word Master. Win based on successfully guessing the word.", ScoringType.WIN_LOSS, description = "A battle of wits between the Word Master and the guessers. Think fast, give clever clues, and make contact."),
            GameTemplate("Spyfall", Icons.Rounded.Policy, Color(0xFFC62828), "Team", "Faction Win", "None", "Spies vs. Non-Spies. Faction win metric.", ScoringType.WIN_LOSS, description = "Everyone knows the location — except the spy. Ask probing questions, blend in, and sniff out the imposter."),
            GameTemplate("Two Truths and a Lie", Icons.Rounded.Quiz, Color(0xFF7E57C2), "Free-for-all", "Correct Guesses", "None", "Tally correct guesses of the lie over the night.", ScoringType.POINTS, description = "How well do you really know your friends? Spot the lie hidden among two truths and prove it."),
        ),
        "Puzzle Games" to listOf(
            GameTemplate("Wordle", Icons.Rounded.GridOn, Color(0xFF6AAA64), "Free-for-all", "Guesses", "None", "Lowest number of guesses wins.", ScoringType.POINTS, description = "Guess the 5-letter word. Lowest score wins the day.", scoringUnit = "guesses", isLowerScoreBetter = true),
            GameTemplate("Connections", Icons.Rounded.GridView, Color(0xFFB4C8E0), "Free-for-all", "Mistakes", "None", "Lowest number of mistakes wins.", ScoringType.POINTS, description = "Group words that share a common thread. Lowest score wins.", scoringUnit = "mistakes", isLowerScoreBetter = true),
        ),
    )

    /** Flat, un-categorized view for screens that don't need the grouping. */
    fun getAllGamesFlatList(): List<GameTemplate> = allGamesCategorized.values.flatten()

    /** Resolve a template by its [GameTemplate.id] (used to render a tracked game's real identity). */
    fun templateById(id: String): GameTemplate? = getAllGamesFlatList().firstOrNull { it.id == id }
}
