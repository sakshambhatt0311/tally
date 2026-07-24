package com.tally.app.data

import kotlin.random.Random

/**
 * Turns a logged session into a fun, game-specific Feed line. Pure/stateless — the mapper feeds it
 * the decoded winners/losers and the per-player scores, and it fills a randomly-chosen template.
 *
 * Placeholders: [W] winner(s), [L] loser(s), [S] = **margin of victory** (winner score − highest
 * loser score), NOT the raw total. The random pick is seeded by the session id so a card always
 * shows the SAME line across re-emissions (no reshuffle on every DB update).
 *
 * Keys are template ids (== catalog game names). Chess draw lines carry [DRAW_TAG].
 */
object FeedMessageGenerator {

    private const val DRAW_TAG = "**[DRAW]**"

    /**
     * @param templateId the game (catalog id / name)
     * @param winners    placement-1 players, stable order
     * @param losers     placement > 1 players
     * @param scores     per-player points/goals (empty for non-scored games)
     * @param seed       stable per-session seed (e.g. session id hashCode)
     */
    fun generate(
        templateId: String,
        winners: List<String>,
        losers: List<String>,
        scores: Map<String, Int>,
        seed: Int,
    ): String {
        val pool = messages[templateId]
        if (pool.isNullOrEmpty()) return genericFallback(winners)
        val rng = Random(seed)

        val isLowerScoreBetter = MockGameData.templateById(templateId)?.isLowerScoreBetter == true
        
        val winnerScore = if (scores.isNotEmpty() && winners.isNotEmpty()) {
            if (isLowerScoreBetter) {
                winners.mapNotNull { scores[it] }.minOrNull()
            } else {
                winners.mapNotNull { scores[it] }.maxOrNull()
            }
        } else null
        
        val loserScore = if (scores.isNotEmpty() && losers.isNotEmpty()) {
            if (isLowerScoreBetter) {
                losers.mapNotNull { scores[it] }.minOrNull()
            } else {
                losers.mapNotNull { scores[it] }.maxOrNull()
            }
        } else null

        val margin = if (winnerScore != null && loserScore != null) {
            kotlin.math.abs(winnerScore - loserScore)
        } else null

        // A complete tie means no one is in the winners list (e.g. 1v1 tie).
        val isTie = winners.isEmpty() && losers.isNotEmpty()

        val isChess = templateId.equals("Chess", ignoreCase = true)
        val isChessDraw = isChess && isTie

        var candidates = when {
            isChessDraw -> pool.filter { it.contains(DRAW_TAG) }
            isChess -> pool.filterNot { it.contains(DRAW_TAG) }
            else -> pool
        }

        // If it's a tie for a game without custom DRAW_TAG templates, bypass to dedicated tie strings
        if (isTie && !isChessDraw) {
            return if (losers.size == 2) {
                "A hard-fought draw between ${losers[0]} and ${losers[1]}."
            } else {
                "A shared draw between ${joinNames(losers)}."
            }
        }

        // Avoid [L] lines when there are no losers (co-op / everyone tied for 1st).
        if (losers.isEmpty()) {
            candidates = candidates.filterNot { it.contains("[L]") }.ifEmpty { candidates }
        }
        // Avoid [S], [WS], or [LS] lines when there's no score/margin to show.
        if (margin == null || winnerScore == null || loserScore == null) {
            candidates = candidates.filterNot { it.contains("[S]") || it.contains("[WS]") || it.contains("[LS]") }.ifEmpty { candidates }
        }
        if (candidates.isEmpty()) return sharedOrGenericFallback(winners, losers)

        return candidates[rng.nextInt(candidates.size)]
            .replace(DRAW_TAG, "")
            .replace("[W]", joinNames(winners).ifEmpty { "Someone" })
            .replace("[L]", joinNames(losers).ifEmpty { "everyone else" })
            .replace("[S]", margin?.toString() ?: "")
            .replace("[WS]", winnerScore?.toString() ?: "")
            .replace("[LS]", loserScore?.toString() ?: "")
            .trim()
    }

    /** Oxford-comma grammar: "Alice" / "Alice and Bob" / "Alice, Bob, and Charlie". */
    private fun joinNames(names: List<String>): String = when (names.size) {
        0 -> ""
        1 -> names[0]
        2 -> "${names[0]} and ${names[1]}"
        else -> names.dropLast(1).joinToString(", ") + ", and " + names.last()
    }

    private fun genericFallback(winners: List<String>): String =
        if (winners.isEmpty()) "Session logged." else "${joinNames(winners)} took the win."

    /** When every candidate got filtered out: a shared-victory line for ties, else the plain fallback. */
    private fun sharedOrGenericFallback(winners: List<String>, losers: List<String>): String =
        if (losers.isEmpty() && winners.isNotEmpty()) "${joinNames(winners)} shared the victory!"
        else genericFallback(winners)

    private val messages: Map<String, List<String>> = mapOf(
        "FIFA" to listOf(
            "[W] put on an absolute clinic, taking home the match ball with [S] goals!",
            "A defensive disaster for [L] as [W] danced through them for a [S]-goal victory.",
            "[W] parked the bus and countered perfectly to secure a [S]-goal win.",
            "Total football! [W] dominated the pitch and left [L] chasing shadows.",
            "[W] channeled prime Messi, racking up [S] goals to crush [L].",
            "It’s a game of two halves, but [W] dominated both against [L].",
            "[L] needs to fire their manager after that [S]-goal thrashing by [W].",
            "Late drama! [W] clutched up to secure a beautiful [S]-goal win.",
            "[W] brought the trophy home, leaving [L] to face the press.",
            "A tactical masterclass from [W] resulted in a clean [S]-goal triumph.",
            "Scripting? No, [L] is just terrible. [W] wins with [S] goals.",
            "Controller disconnected? No, [L] just couldn't handle [W]'s [S]-goal masterclass.",
            "EA Sports, it's in the game! And [W] was clearly the only one playing, scoring [S].",
            "Sweat mode activated. [W] cut back and sweated their way to a [S]-goal win over [L].",
            "Peter Crouch never played for Spurs, and [L] never showed up to this match. [W] wins with [S] goals.",
            "Get good. [W] completely humiliated [L] with [S] goals.",
            "Time to quick-sell the whole club, [L]. [W] just destroyed you with [S] goals.",
            "Did [L] even have their monitor on? [W] walks away with a [S]-goal win.",
            "[W] griddied on [L] [S] times in a brutal display of dominance.",
            "Absolute scenes! [W] parked the bus and hit the griddy [S] times on [L].",
            "No DDA can save [L] from [W]'s [S]-goal demolition.",
            "Someone check on [L]'s controller after that [S]-goal beating by [W].",
            "[L] is complaining about lag, but we all know [W] is just better. [S] goals!",
            "[W] hit the crossbar twice and STILL embarrassed [L] with [S] goals.",
            "Pace abuse at its finest. [W] sprinted past [L] for [S] goals.",
            "A prime icon performance from [W], leaving [L] in the dust with [S] goals.",
            "The referee should have stopped the match. [W] battered [L] with [S] goals.",
            "[L] tried to rage quit, but [W] already secured the [S]-goal victory.",
            "It's not the game's fault, [L]. [W] is just clear. [S] goals to prove it.",
            "[W] used the meta tactics to completely obliterate [L] [S]-0... metaphorically.",
        ),
        "Mario Kart" to listOf(
            "[W] dodged the Blue Shells and drifted into 1st place!",
            "[L] got absolutely Mario Karted at the finish line by [W].",
            "Rainbow Road claims another victim, but [W] survives to take gold.",
            "[W] hoarded the red shells and boosted past [L] for the win.",
            "No brakes, just vibes. [W] dominated the Grand Prix.",
            "[W] hit every apex and left [L] choking on exhaust fumes.",
            "[L] is still recovering from that lightning strike while [W] takes 1st.",
            "[W] proved that 200cc is light work, securing the top spot.",
            "A perfectly timed mushroom gave [W] the ultimate victory over [L].",
            "[W] crossed the finish line first, leaving [L] stuck in the banana peels.",
            "[W] didn't even need to drift to completely gap [L] for the win.",
            "[L] caught a stray green shell while [W] cruised into 1st place.",
            "[W] perfectly managed their items to secure a beautiful gold over [L].",
            "[L] fell off the map, giving [W] the easiest 1st place finish ever.",
            "[W] blue-shelled the competition and snatched victory from [L].",
        ),
        "Mario Party" to listOf(
            "[W] stole all the stars and completely ruined [L]'s day.",
            "Friendship ended with [L]. [W] is the new Super Star.",
            "[W] dominated the mini-games and crushed [L]'s dreams.",
            "Pure RNG? Nope, just skill. [W] takes the Mario Party crown!",
            "[W] bribed Boo, stole the star, and laughed all the way to 1st place.",
            "[L] landed on a Bowser space so [W] could walk away with the win.",
            "[W] proved that Chance Time is only terrifying if you're [L].",
            "[W] hoarded the coins and secured the victory over [L].",
            "The dice blocks heavily favored [W] in a crushing victory.",
            "[W] is the undisputed Party MVP, leaving [L] with nothing but tears.",
            "[W] won the bonus stars by doing absolutely nothing, crushing [L].",
            "[L] lost all their coins to a Bowser space while [W] danced to victory.",
            "[W] won the 1v3 minigame, completely humiliating [L].",
            "[W] used a golden pipe to steal the win right out from under [L].",
            "The game is rigged, but [W] doesn't care. A dominant win over [L].",
        ),
        "Super Smash Bros" to listOf(
            "[W] spiked [L] straight into the blast zone for the win!",
            "No items, Fox only, Final Destination. [W] reigns supreme.",
            "[W] read [L] like a book and secured the final stock.",
            "An absolute combo video! [W] barely broke a sweat against [L].",
            "[L] forgot how to recover, handing [W] the easiest 1st place.",
            "[W] mashed buttons slightly better than [L] to take the crown.",
            "Frame-perfect disrespect. [W] styled all over [L] for the victory.",
            "[W] clutched the final stock at 150% damage against [L].",
            "[L] got juggled across the stage while [W] claimed 1st place.",
            "[W] charged the smash attack, read the roll, and sent [L] flying.",
            "[W] ledge-guarded [L] into oblivion for the final stock.",
            "[W] hit a 0-to-death combo, leaving [L] contemplating their life choices.",
            "[L] SD'd on their last stock, handing [W] the most unearned win.",
            "[W] countered [L]'s smash attack perfectly to secure 1st place.",
            "[W] spammed projectiles until [L] gave up and lost.",
        ),
        "Tekken" to listOf(
            "[W] sent [L] flying with a perfectly-timed launcher for the KO!",
            "Round won! [W] read [L]'s every move and landed the finishing combo.",
            "[W] cornered [L] and unleashed a brutal juggle to take the match.",
            "Flawless. [W] didn't drop a single round against [L].",
            "[W] punished [L]'s whiff and cashed out the round for the win.",
            "[L] got read like a book - [W] blocked the mixup and countered for the win.",
            "[W] hit a wall combo of a lifetime, leaving [L] with nothing.",
            "EWGF after EWGF - [W] completely outclassed [L] to win the set.",
            "[W] snuffed out [L]'s comeback with a clutch low-parry finish.",
            "Perfect spacing from [W] kept [L] out and sealed the victory.",
            "[W] baited the reversal and blew [L] up for the winning round.",
            "[L] mashed on wakeup and [W] made them pay - GG.",
            "Down to the last hit - [W] landed the finisher to defeat [L].",
            "[W] styled on [L] with a taunt-into-combo to close it out.",
            "[W] hard-reads and pure fundamentals dismantled [L] for the win.",
        ),
        "Football Manager" to listOf(
            "[W] proved to be the ultimate tactical genius, outclassing [L].",
            "Move over Pep, [W] just delivered a managerial masterclass.",
            "[W] worked the transfer market and the pitch to secure the win.",
            "[L]’s dressing room lost faith, while [W] lifted the trophy.",
            "xG means nothing if you don't win. [W] got the result against [L].",
            "[W] parked the bus, hit on the counter, and tactically destroyed [L].",
            "The board is thrilled with [W], while [L] faces the sack.",
            "[W] turned a squad of wonderkids into absolute champions.",
            "[L] got out-managed, out-played, and out-classed by [W].",
            "[W] yelled from the touchline and willed their team to victory.",
            "[W] discovered a 15-year-old wonderkid who completely dismantled [L]'s squad.",
            "[L] suffered a late collapse while [W]'s tactical subs secured the win.",
            "[W] savescummed their way into our hearts and defeated [L].",
            "[L] got \"FM'd\" hard, dominating possession but losing to [W]'s single shot.",
            "[W] threw a water bottle at halftime and motivated the team to crush [L].",
        ),
        "Call of Duty / Halo" to listOf(
            "[W] dropped a tactical nuke on the lobby, leaving [L] in ruins.",
            "[L] needs to hit the aim trainer after [W]'s dominant performance.",
            "[W] secured the victory with a flawless K/D ratio against [L].",
            "360 no-scope! [W] absolutely styled on [L] for the win.",
            "[W] controlled the power weapons and completely shut down [L].",
            "Run and gun! [W] flanked [L] all match to take the crown.",
            "[W] held the angles, hit the headshots, and secured the W.",
            "[L] got caught lacking on the respawn by [W].",
            "Sweaty lobbies are no match for [W], who takes the ultimate victory.",
            "[W] carried the squad, cleared the objective, and defeated [L].",
            "[W] spawned trapped [L] the entire game for a ruthless win.",
            "[L] got drop-shotted into next week by [W].",
            "[W] hit a cross-map sticky grenade on [L] to seal the victory.",
            "[W] carried the lobby and absolutely styled on [L].",
            "[L] complained about SBMM while [W] effortlessly took the crown.",
        ),
        "UNO (Single Winner)" to listOf(
            "[W] dropped the devastating +4 to end [L]'s hopes and dreams.",
            "[W] held the final card while everyone else drew the deck.",
            "UNO out! [W] played their cards perfectly to take the win.",
            "[W] chained the skips and reverses, leaving [L] completely stuck.",
            "[L] forgot to say UNO, giving [W] the perfect opening to win.",
            "[W] dodged every draw card and smoothly exited the game.",
            "The color changed exactly when [W] needed it to secure the W.",
            "[W] proved that UNO is a game of ruthless strategy, not luck.",
            "[L] got hit with the +2 combo while [W] walked away victorious.",
            "[W] threw down the wildcard and walked away a champion.",
            "[W] hit [L] with the reverse-skip combo to end the game.",
            "[W] confidently dropped their last card while [L] held half the deck.",
            "[L] drew 4, and [W] drew the victory.",
            "[W] ended the game on a Wild card, causing absolute outrage from [L].",
            "[W] snuck in their final card while [L] was busy arguing about house rules.",
        ),
        "UNO (Placements)" to listOf(
            "[W] navigated the chaos of the draw pile to secure 1st place!",
            "[L] got skipped straight into last place while [W] took gold.",
            "[W] managed their hand beautifully to claim the top spot.",
            "[W] survived the barrage of +4s better than anyone else.",
            "While [L] drew half the deck, [W] comfortably secured 1st.",
            "[W] reverse-carded their way out of trouble and into the lead.",
            "[W] stacked their way to victory, leaving [L] in the dust.",
            "[W] played the long game and secured a well-earned top placement.",
            "A masterclass in hand management puts [W] at the top of the board.",
            "[W] cleared their hand efficiently, securing a dominant 1st place.",
            "[W] gracefully exited the chaos, leaving [L] stuck in the draw loop.",
            "[W] managed to avoid all the +2s and secured a clean 1st place.",
            "[L] became the target of everyone's Draw 4s, paving the way for [W].",
            "[W] expertly tracked the colors and cruised into the top placement.",
            "[W] dumped their high-point cards fast and secured the gold over [L].",
        ),
        "Catan" to listOf(
            "[W] built the Longest Road straight to victory!",
            "[W] monopolized the brick and left [L] completely stranded.",
            "No wood? No problem. [W] traded their way to the crown.",
            "The Robber was no match for [W]'s massive economic engine.",
            "[W] secured the Largest Army and crushed [L]'s hopes.",
            "[L] got stuck on 9 points while [W] quietly built a city for the win.",
            "[W] cornered the ore market and built a dominant empire.",
            "Victory points galore! [W] out-settled [L] across the board.",
            "[W] proved that sheep are the ultimate currency, securing the W.",
            "A perfect port strategy allowed [W] to dominate the island.",
            "[W] cut off [L]'s road network and laughed all the way to 10 points.",
            "[W] rolled 7s all night and stole every resource from [L].",
            "[L] refused to trade, so [W] built a metropolis without them.",
            "[W] flipped a hidden victory point card to shockingly defeat [L].",
            "[W] monopolized the wheat and starved [L] out of the game.",
        ),
        "Ludo" to listOf(
            "[W] rolled all the sixes and raced straight to the center!",
            "[W] ruthlessly cut [L]'s token right at the home stretch.",
            "Pure dice magic! [W] secures 1st place in a heated race.",
            "[L] was stuck at base while [W] did a full victory lap.",
            "[W] dodged everyone and perfectly timed their home run.",
            "[W] built blockades, cut tokens, and secured absolute victory.",
            "[L] got sent back to start, handing [W] the easiest 1st place.",
            "[W] mastered the board and comfortably took the Ludo crown.",
            "A lucky roll of the dice sent [W] flying into the winning spot.",
            "[W] played no games, eliminating [L] and claiming victory.",
            "[W] chased [L]'s token across the entire board just to send them home.",
            "[W] camped at the star spaces and safely secured 1st place.",
            "[L] rolled five 1s in a row while [W] sprinted to the finish.",
            "[W] created an unbreakable blockade, ruining [L]'s entire game.",
            "[W] played with zero mercy, eliminating [L] on the final stretch.",
        ),
        "Poker (Tournament)" to listOf(
            "[W] rivered a monster to bust [L] and win the whole tournament!",
            "All in! [W] called the bluff and walked away with the trophy.",
            "[W] chipped up all night and finally knocked [L] out in heads-up.",
            "[W] read [L]'s soul and made the hero call for the win.",
            "[W] got pocket aces at the perfect time to secure 1st place.",
            "[L] got coolered hard, allowing [W] to take down the tournament.",
            "[W] shoved the flop and completely dominated the final table.",
            "Ice in their veins. [W] bluffed [L] off the winning pot.",
            "[W] ground down [L]'s stack to claim the ultimate Poker victory.",
            "[W] went wire-to-wire, holding the chip lead to the very end.",
            "[W] slow-played a set and completely trapped [L] for the tournament win.",
            "[L] shoved with top pair, but [W] had the straight to take the trophy.",
            "[W] bullied the bubble and stacked [L] to win it all.",
            "[W] won a massive coin flip against [L] to secure the 1st place finish.",
            "[L] got caught bluffing with air, handing the tournament to [W].",
        ),
        "Poker (Cash Games)" to listOf(
            "[W] cleaned out the table, walking away with a [S]-point margin.",
            "[W] stacked [L] and racked up a [S]-point lead.",
            "Pure profit! [W] dominated the cash game by [S] points.",
            "[L] re-bought twice, but [W] still beat them by [S].",
            "[W] played the odds perfectly, securing a [S]-point win.",
            "A masterclass in value betting! [W] cashes out [S] ahead.",
            "[W] trapped [L] and took down a massive pot to lead by [S] points.",
            "[W] was the shark of the table tonight, up [S] on the field.",
            "[W] ground out a [S]-point edge while [L] bluffed their stack away.",
            "[W] hit the nuts and extracted a [S]-point margin.",
            "[W] hit a flush on the river, draining [L] by [S] points.",
            "[W] check-raised [L] into oblivion, winning by [S] points.",
            "[L] went on tilt, and [W] gladly took a [S]-point lead.",
            "[W] caught runner-runner to crush [L] by [S].",
            "[W] flopped the absolute nuts and beat [L] by [S] points.",
        ),
        "Chess" to listOf(
            "[W] delivered a crushing checkmate to [L].",
            "A brilliant tactical sequence by [W] forced [L] to resign.",
            "[L] blundered their queen, allowing [W] to easily secure the win.",
            "[W] played a flawless endgame to convert the advantage over [L].",
            "[W] found the forced mate in 3, completely outclassing [L].",
            "[W] dominated the center and squeezed the life out of [L]'s position.",
            "$DRAW_TAG An absolute grind! The players agreed to a hard-fought draw.",
            "$DRAW_TAG Neither side could break through. It ends in a peaceful stalemate.",
            "$DRAW_TAG A brilliant defensive stand forced a draw by repetition.",
            "$DRAW_TAG Insufficient material leaves both players sharing the spoils.",
            "[W] forked [L]'s king and rook, forcing a swift resignation.",
            "[L] missed a simple pin, allowing [W] to take control of the board.",
            "[W] sacked the exchange and launched a devastating mating net against [L].",
            "[W] squeezed [L] in a grueling pawn endgame for the win.",
            "$DRAW_TAG Both players shuffled their pieces until a completely dead drawn endgame.",
        ),
        "Monopoly" to listOf(
            "[W] put a hotel on Boardwalk and completely bankrupted [L].",
            "Capitalism is cruel, but [W] is crueler. A massive win over [L].",
            "[W] owned all the railroads and rode them straight to 1st place.",
            "[L] went straight to jail while [W] collected all the rent.",
            "[W] cornered the housing market and starved [L] of cash.",
            "[W] held the orange properties and drained [L]'s bank account.",
            "A ruthless trading strategy gave [W] the ultimate Monopoly.",
            "[W] passed GO, collected \$200, and secured the final victory.",
            "[L] had to mortgage everything, but [W] still took the crown.",
            "[W] controlled the board, leaving [L] with nothing but debt.",
            "[W] refused to trade the final green property, suffocating [L].",
            "[L] landed on Free Parking, but [W] still took all their money.",
            "[W] upgraded to hotels on the first side of the board and destroyed [L].",
            "[W] survived on mortgage loans before turning it around to bankrupt [L].",
            "[L] flipped the board in rage after [W] collected massive rent.",
        ),
        "Scotland Yard" to listOf(
            "[W] outsmarted [L] in a tense chase across London.",
            "The mind games went the distance, but [W] came out on top over [L].",
            "[W] controlled the board and left [L] a step behind all game.",
            "A masterclass in movement — [W] read the map perfectly to beat [L].",
            "[W] won the standoff on the Underground, edging out [L].",
            "Every ticket counted, and [W] played them better than [L].",
            "[W] kept their nerve to the final move and took it from [L].",
            "[L] made one wrong turn, and [W] pounced to seal the win.",
            "A brilliant bluff from [W] settled the hunt against [L].",
            "The chase came down to the wire, but [W] outfoxed [L].",
            "[W] out-planned [L] station by station for a well-earned win.",
            "Nerves of steel — [W] held the pressure and defeated [L].",
            "[W] anticipated every move and shut [L] down cold.",
            "A calculated endgame from [W] left [L] with no answer.",
            "[W] pulled off the perfect play, leaving [L] guessing wrong.",
        ),
        "Table Tennis" to listOf(
            "[W] unleashed unreturnable topspin to secure a [S]-point win!",
            "[L] got caught flat-footed by [W]'s lethal [S]-point clinic.",
            "[W] dominated the rallies and smashed their way to a [S]-point lead.",
            "A masterclass in spin! [W] takes the table by [S] points.",
            "[W] kept [L] running edge-to-edge, closing it out by [S] points.",
            "Unbelievable reflexes from [W] resulted in a massive [S]-point win.",
            "[W] served absolute heat, acing [L] to win by [S] points.",
            "[L] couldn't handle the chop, handing [W] the [S]-point victory.",
            "[W] hit every edge and corner, dominating by [S] points.",
            "A fierce backhand secured [W] the well-deserved [S]-point win.",
            "[W] smashed a forehand down the line, securing a [S]-point victory.",
            "[L] whiffed the return, handing [W] the game by [S] points.",
            "[W] used a nasty pendulum serve to completely trick [L] for a [S]-point win.",
            "[W] rallied from behind to snatch a [S]-point win from [L].",
            "[L] couldn't deal with [W]'s backhand flicks, losing by [S] points.",
        ),
        "Badminton" to listOf(
            "[W] reigned supreme with a crushing [S]-point victory!",
            "[W]'s jump smash was simply too much for [L] to handle.",
            "[W] floated like a butterfly and smashed to a [S]-point win.",
            "Incredible net play from [W] secured a brilliant [S]-point margin.",
            "[W] cleared the shuttlecock perfectly, exhausting [L] for the W.",
            "[L] was left guessing as [W] dominated by [S] points.",
            "A relentless attack gave [W] a dominant [S]-point victory.",
            "[W] controlled the pace of the rally, winning cleanly by [S] points.",
            "[W] disguised the drop shot beautifully to close out the game.",
            "Agility and power! [W] takes the court by [S] points.",
            "[W] dominated the net exchanges, taking the game by [S] points.",
            "[L] misjudged the baseline, allowing [W] to win by [S] points.",
            "[W] hit a deceptive cross-court drop to finish [L] by [S] points.",
            "[W] showed insane stamina, outlasting [L] for a [S]-point win.",
            "[L] got frustrated by [W]'s flawless clears, losing by [S] points.",
        ),
        "Tennis" to listOf(
            "[W] served up an absolute clinic, winning by [S] points!",
            "Game, set, match to [W], who completely outplayed [L].",
            "[W] hit winner after winner down the line to lead by [S] points.",
            "[L] couldn't break [W]'s serve, resulting in a dominant [S]-point win.",
            "[W] controlled the baseline and secured the [S]-point victory.",
            "A flawless volley game gave [W] the edge and the [S]-point win.",
            "[W] hit the kick serve perfectly, acing [L] for the final point.",
            "[W] rallied back to take the match by [S] points.",
            "Unforced errors doomed [L], while [W] stayed steady for the win.",
            "[W] dominated the court, securing a grand slam [S]-point finish.",
            "[W] hit a stunning passing shot to break [L]'s serve, up [S] points.",
            "[L] double-faulted on match point, handing [W] the [S]-point win.",
            "[W] lobbed [L] perfectly to close out the match by [S] points.",
            "[W] completely dominated the tiebreaker, winning by [S] points.",
            "[L]'s forehand broke down under pressure from [W]'s [S]-point clinic.",
        ),
        "Pool" to listOf(
            "[W] ran the table and sank the 8-ball with absolute style.",
            "[W] capitalized on [L]'s scratch to easily secure the win.",
            "A masterclass in English! [W] spun their way to victory.",
            "[W] called the pocket, made the shot, and crushed [L]'s hopes.",
            "[W] played the perfect safety, forcing [L] into a losing error.",
            "Clean break, clean finish. [W] dominated the felt tonight.",
            "[W] banked the final shot beautifully to secure the W over [L].",
            "[L] left the 8-ball hanging, and [W] gladly took the win.",
            "[W] mapped out the table perfectly, clearing it for the victory.",
            "Incredible cue control gave [W] the definitive edge over [L].",
            "[W] scratched on the break but still managed to run the table on [L].",
            "[L] accidentally potted the 8-ball early, giving [W] the easiest win.",
            "[W] executed a flawless jump shot to clear the table and defeat [L].",
            "[W] played a brutal safety battle, eventually forcing [L] to concede.",
            "[W] sliced the final ball into the side pocket, devastating [L].",
        ),
        "Billiards" to listOf(
            "[W] showed absolute mastery over the cue ball for the win.",
            "[W]'s precision cannons left [L] with absolutely no response.",
            "[W] controlled the cushions perfectly to secure a tactical victory.",
            "[L] was snookered, allowing [W] to take complete control of the match.",
            "[W] built a massive break, leaving [L] stranded in their chair.",
            "A brilliant display of safety play gave [W] the ultimate win.",
            "[W] potted out with flawless positional play to defeat [L].",
            "[L] missed the plant, and [W] cleaned up the table for the win.",
            "[W] proved their stroke is pure, dominating [L] on the green baize.",
            "[W] played the angles perfectly to become the billiards champion.",
            "[W] calculated the angles flawlessly, completely freezing out [L].",
            "[L] left the balls wide open, and [W] punished them severely.",
            "[W] executed a beautiful massé shot to secure the victory over [L].",
            "[W] strung together a massive break, leaving [L] with no chance.",
            "[L] completely miscued, allowing [W] to step up and win.",
        ),
        "Codenames" to listOf(
            "[W] perfectly deciphered their Spymaster's clues to snatch the win!",
            "[W] successfully avoided the Assassin to secure a brilliant victory.",
            "[L] guessed the wrong agent, handing [W] the easiest win ever.",
            "A flawless 4-word clue led [W] straight to absolute victory.",
            "[W] linked the most abstract concepts to clear their board first.",
            "[W] got into their Spymaster's head and found all the agents.",
            "[L] made contact with a bystander, costing them the game to [W].",
            "[W] took a massive risk on the final guess, and it paid off beautifully.",
            "The ultimate intelligence agency! [W] outsmarted [L] completely.",
            "[W] cracked the code and swept the board clean for the win.",
            "[W] linked 5 words with one obscure clue and obliterated [L].",
            "[L] accidentally picked the Assassin on turn one, handing [W] the win.",
            "[W] mind-melded with their team for a flawless sweep over [L].",
            "[W] pulled off a desperation 0-clue guess to snatch the victory from [L].",
            "[L]'s Spymaster gave a terrible clue, opening the door for [W]'s win.",
        ),
        "Mafia" to listOf(
            "[W] successfully manipulated the town and survived the night.",
            "The town was saved! [W]'s brilliant deductions routed the Mafia.",
            "[W] lied through their teeth and convinced [L] to vote wrong.",
            "[W] played the perfect Doctor, saving the game for the town.",
            "[L] got framed perfectly by [W], who walks away victorious.",
            "[W] remained under the radar and struck when the town was weak.",
            "A flawless execution of the night phase gave [W] the ultimate win.",
            "[W] led the daytime tribunal perfectly, eliminating [L]'s faction.",
            "[L] trusted the wrong person, handing [W] the total victory.",
            "[W] proved that deception is an art form, taking the crown.",
            "[W] bussed their own Mafia teammate to earn the town's trust and win.",
            "[L] was the Cop but nobody believed them, giving [W] the victory.",
            "[W] claimed a fake role and rode it all the way to a dominant win.",
            "[W] read [L]'s body language instantly and got them eliminated.",
            "[L] stayed too quiet and got voted out, letting [W] secure the win.",
        ),
        "Contact" to listOf(
            "[W] successfully defended their words against all of [L]'s attacks!",
            "[W] established Contact, guessed the word, and stole the win.",
            "[L] couldn't break [W]'s cryptic clues in a stunning victory.",
            "[W] completely mind-read their partner to secure the W.",
            "[W] pulled the most obscure words out of the dictionary to win.",
            "A brilliant interception allowed [W] to take control of the game.",
            "[W] successfully stalled the guessers, securing the ultimate win.",
            "[L] was left totally confused by [W]'s masterful wordplay.",
            "[W] built the perfect chain of clues to dominate the round.",
            "[W] proved their vocabulary is unmatched, crushing [L].",
            "[W] came up with a clue so galaxy-brain that [L] had zero chance.",
            "[L] panicked and yelled a random word, allowing [W] to advance.",
            "[W] and their partner were perfectly in sync, completely destroying [L].",
            "[W] forced [L] to reveal the letter by making contact at the last second.",
            "[L] couldn't think of a single word, while [W] cruised to victory.",
        ),
        "Spyfall" to listOf(
            "[W] blended in perfectly, leaving [L] completely clueless.",
            "[W] proved to be the ultimate Spy, fooling everyone at the location.",
            "[W] asked the perfect question to expose [L] as the Spy!",
            "[L] stumbled on their answer, and [W] successfully called the vote.",
            "[W] guessed the location blindly and pulled off a miracle win.",
            "A masterclass in vague answering gave [W] the victory.",
            "[W] sniffed out the traitor immediately, securing the win over [L].",
            "[W] framed [L] flawlessly, escaping undetected as the Spy.",
            "[W] kept their cool under intense interrogation to secure the W.",
            "[L] tried to fit in, but [W] saw right through the disguise.",
            "[L] asked a question that was way too specific, and [W] instantly caught them.",
            "[W] gave the vaguest answer possible and somehow survived to win.",
            "[W] laughed nervously but still managed to convince [L] they weren't the spy.",
            "[W] accurately pinpointed the location based on [L]'s terrible question.",
            "[L] accused the wrong person, allowing [W] to slip away victorious.",
        ),
        "Two Truths and a Lie" to listOf(
            "[W] spun a massive web of lies and won by [S] points!",
            "[W] is officially the best liar in the room, up [S] points.",
            "[L] got completely bamboozled, losing to [W] by [S] points.",
            "[W] called every bluff perfectly, winning by [S] points.",
            "A flawless poker face earned [W] a dominant [S]-point victory.",
            "[W] told the most unbelievable truths to trick [L] and win by [S].",
            "[W] knows their friends too well, exposing the lies for a [S]-point win.",
            "[W] played the group like a fiddle, opening up a [S]-point lead.",
            "[L] thought they were sneaky, but [W] saw through it to win by [S].",
            "[W] proved you can't trust anyone, winning the game by [S] points.",
            "[W] told a completely absurd truth that [L] thought was a lie, winning by [S].",
            "[W] kept a straight face through the craziest lie, up [S] points.",
            "[L] second-guessed themselves, letting [W] secure the [S]-point win.",
            "[W] completely fabricated a childhood memory to trick [L] by [S] points.",
            "[L] couldn't hide their smirk, allowing [W] to win by [S].",
        ),
        "Wordle" to listOf(
            "[W] solved it in [S] fewer guesses than [L], claiming Wordle supremacy.",
            "[W] pulled off a genius starting word to beat [L] by [S] guesses.",
            "A masterclass in vocabulary! [W] crushed [L] by [S] guesses.",
            "[L] got stuck on a tricky letter, handing [W] a [S]-guess victory.",
            "[W] nailed the Wordle effortlessly, leaving [L] [S] guesses behind.",
            "[W] channeled their inner dictionary to outsmart [L] by [S] guesses.",
            "[L] barely survived with [LS] guesses while [W] cruised to a [WS]-guess win.",
        ),
        "Connections" to listOf(
            "[W] grouped the categories perfectly, beating [L] by [S] mistakes.",
            "[W] spotted the purple category immediately to crush [L] by [S] mistakes.",
            "[L] got completely lost in the red herrings, letting [W] win by [S].",
            "A flawless grid from [W], leaving [L] [S] mistakes behind.",
            "[W] saw the common thread instantly, dominating [L] by [S] mistakes.",
            "[W] didn't fall for the trap words and beat [L] by [S] mistakes.",
            "[L] wasted all their guesses on obvious overlaps while [W] won by [S].",
        ),
    )
}
