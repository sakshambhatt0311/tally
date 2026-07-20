package com.tally.app.ui.session

import com.tally.app.data.RosterMember
import com.tally.app.data.ScoringType
import com.tally.app.data.TrackedGame

const val TOTAL_STEPS = 3

/** Games restricted to 2–4 players (template ids). */
private val RACKET_SPORTS = setOf("Table Tennis", "Badminton", "Tennis")

/**
 * The finishing-order/points container for Step 3. Its shape adapts to the selected game's
 * [ScoringType] — this is the "map or list" the results live in, made type-safe per mode so a
 * screen can never, say, read a winner off a placement game.
 */
sealed interface SessionResults {
    /** No game chosen yet / results not seeded. */
    data object None : SessionResults

    /** Placement games (Catan, Smash): players held in finishing order — index 0 is 1st place. */
    data class Placement(val orderedPlayers: List<RosterMember>) : SessionResults

    /** Points games: a text-field-friendly String per player, keyed by player name. */
    data class Points(val pointsByPlayer: Map<String, String>) : SessionResults

    /** Win/loss games: the set of winners' names (empty until picked). Supports ties / co-op wins. */
    data class WinLoss(val winners: Set<String>) : SessionResults
}

/**
 * Immutable snapshot of the whole 3-step logging flow. All transitions are pure functions that
 * return a new state, so a ViewModel can later hold this in a `MutableStateFlow` and call these
 * same methods from its event handlers with zero changes to the composables that render it.
 */
data class LogSessionState(
    val currentStep: Int = 1,
    val selectedGame: TrackedGame? = null,
    val selectedPlayers: List<RosterMember> = emptyList(),
    val results: SessionResults = SessionResults.None,
) {
    val isFirstStep: Boolean get() = currentStep == 1
    val isLastStep: Boolean get() = currentStep == TOTAL_STEPS

    private fun templateIs(name: String): Boolean =
        selectedGame?.templateId?.equals(name, ignoreCase = true) == true

    /** Chess is strict: exactly two players and a custom win/draw step. */
    val isChess: Boolean get() = templateIs("Chess")

    /** Billiards is strictly 1v1 — the win/loss step is single-select (radio), not multi-select. */
    val isBilliards: Boolean get() = templateIs("Billiards")

    /** Tekken is strictly 1v1 with no draws — single-select winner, exactly one winner required. */
    val isTekken: Boolean get() = templateIs("Tekken")

    /** Racket sports (Table Tennis / Badminton / Tennis) allow 2 to 4 players. */
    val isRacketSport: Boolean
        get() = selectedGame?.templateId?.let { id -> RACKET_SPORTS.any { it.equals(id, ignoreCase = true) } } == true

    /** Step 2 helper text for games with a player-count rule, or null. */
    val playerRequirementHint: String?
        get() = when {
            isChess -> "Chess requires exactly 2 players"
            isRacketSport -> "This game requires 2 to 4 players"
            else -> null
        }

    /** Gate for the Next button on steps 1 & 2, honoring per-game player-count rules. */
    val canAdvance: Boolean
        get() = when (currentStep) {
            1 -> selectedGame != null
            2 -> when {
                isChess -> selectedPlayers.size == 2
                isRacketSport -> selectedPlayers.size in 2..4
                else -> selectedPlayers.size >= 2
            }
            else -> true
        }

    /** Gate for the Save Session button on step 3 — only win/loss demands an explicit choice. */
    val canSave: Boolean
        get() = when (val r = results) {
            is SessionResults.WinLoss -> r.winners.isNotEmpty()
            is SessionResults.Placement -> r.orderedPlayers.isNotEmpty()
            is SessionResults.Points -> true
            SessionResults.None -> false
        }

    fun isSelected(player: RosterMember): Boolean = selectedPlayers.any { it.id == player.id }

    // --- Transitions (pure) --------------------------------------------------

    fun selectGame(game: TrackedGame): LogSessionState = copy(selectedGame = game)

    fun togglePlayer(player: RosterMember): LogSessionState {
        val next = if (isSelected(player)) {
            selectedPlayers.filterNot { it.id == player.id }
        } else {
            selectedPlayers + player
        }
        return copy(selectedPlayers = next)
    }

    /** Advance one step. Entering step 3 seeds [results] from the game's scoring type. */
    fun advance(): LogSessionState {
        if (isLastStep) return this
        val nextStep = currentStep + 1
        val seeded = if (nextStep == 3) initialResultsFor(selectedGame, selectedPlayers) else results
        return copy(currentStep = nextStep, results = seeded)
    }

    fun goBack(): LogSessionState = if (isFirstStep) this else copy(currentStep = currentStep - 1)

    /** Placement reorder — moves the player at [fromIndex] to [toIndex]. No-op off placement mode. */
    fun movePlayer(fromIndex: Int, toIndex: Int): LogSessionState {
        val r = results as? SessionResults.Placement ?: return this
        if (fromIndex !in r.orderedPlayers.indices || toIndex !in r.orderedPlayers.indices) return this
        val reordered = r.orderedPlayers.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        return copy(results = SessionResults.Placement(reordered))
    }

    fun setPoints(playerUid: String, points: String): LogSessionState {
        val r = results as? SessionResults.Points ?: return this
        return copy(results = SessionResults.Points(r.pointsByPlayer + (playerUid to points)))
    }

    /** Toggle a player in/out of the winners set — supports multiple winners (ties, co-op wins). */
    fun toggleWinner(player: RosterMember): LogSessionState {
        val r = results as? SessionResults.WinLoss ?: return this
        val next = if (player.id in r.winners) r.winners - player.id else r.winners + player.id
        return copy(results = SessionResults.WinLoss(next))
    }

    /** Replace the winners set outright — used by Chess's exclusive A / B / Draw choice. */
    fun setWinners(winners: Set<String>): LogSessionState {
        if (results !is SessionResults.WinLoss) return this
        return copy(results = SessionResults.WinLoss(winners))
    }
}

/** Builds the starting results object for step 3 based on the chosen game's scoring type. */
fun initialResultsFor(game: TrackedGame?, players: List<RosterMember>): SessionResults =
    when (game?.scoringType) {
        ScoringType.PLACEMENT -> SessionResults.Placement(players)
        ScoringType.POINTS -> SessionResults.Points(players.associate { it.id to "" })
        ScoringType.WIN_LOSS -> SessionResults.WinLoss(emptySet())
        null -> SessionResults.None
    }
