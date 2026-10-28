// ScoringEngine.kt: the brain of the app. ALL the cricket rules live here.
//
// Nothing in this file draws anything or touches the phone (no screens, no storage), so it can
// be tested on a laptop in milliseconds. The screens and the ViewModel just ask this file
// "what is the score after this ball?" and show the answer.
//
// Three ideas to understand first:
//  1. EVENT LOG. An innings is stored as a list of things that happened ("4 runs", "wide",
//     "wicket"...). The totals on screen (runs, wickets, overs, every batter's score) are NOT
//     stored separately; they are worked out by replaying that list from the start.
//  2. IMMUTABLE. Functions here never change an object. They build and return a NEW copy of the
//     innings with the change applied. The old copy is untouched, which makes bugs much rarer.
//  3. UNDO FOR FREE. Because the totals come from replaying the list, "undo" is simply
//     "replay everything except the last item". That is exact, with no special undo code.
package com.waleedahmedja.score247.engine

import com.waleedahmedja.score247.data.model.BallEvent
import com.waleedahmedja.score247.data.model.Innings
import com.waleedahmedja.score247.data.model.MatchRules
import com.waleedahmedja.score247.data.model.Player

/**
 * The app's suggestion for Player of the Match.
 * A player is identified by their team plus their number (id) within that team.
 */
data class PotmPick(val team: String, val playerId: Int)

/**
 * The balls of one over, used for the row of dots under "This over" on the scoring screen.
 * [overIndex] counts from 0, so the first over has overIndex 0. [complete] is true when the
 * sixth legal ball has just been bowled.
 */
data class OverView(val overIndex: Int, val events: List<BallEvent>, val complete: Boolean)

/**
 * Pure Kotlin scoring engine: no Android, no screens, no storage.
 * ("object" means there is exactly one of it, so you call ScoringEngine.applyEvent(...)
 * directly instead of creating one first.)
 *
 * [applyEvent] adds ONE event to an immutable [Innings]; [rebuild] replays a whole list of
 * events from scratch. See the notes at the top of this file for why that makes undo exact.
 * The tests that prove it are named undo_* in ScoringEngineTest.
 */
object ScoringEngine {

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTRUCTION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Starts an innings with nothing scored. The first two names in the batting list open
     * (positions 0 and 1); if the side only has one player, that player opens alone.
     * "id" is simply each player's position in the list; other code relies on that.
     */
    fun newInnings(
        battingTeam: String,
        bowlingTeam: String,
        batterNames: List<String>,
        bowlerNames: List<String>,
    ): Innings = Innings(
        battingTeam     = battingTeam,
        bowlingTeam     = bowlingTeam,
        batters         = batterNames.mapIndexed { i, n -> Player(id = i, name = n) },
        bowlers         = bowlerNames.mapIndexed { i, n -> Player(id = i, name = n) },
        strikerIndex    = 0,
        // The "striker" faces the next ball; the "non-striker" waits at the other end.
        nonStrikerIndex = if (batterNames.size > 1) 1 else 0,
    )

    // ─────────────────────────────────────────────────────────────────────────
    // RULE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Does this delivery use up one of the over's six balls?
     * Runs, wickets, byes and leg byes always do. A no-ball never does (it must be bowled again).
     * A wide only does if the house rule "wide is a legal ball" is switched on.
     * ("when" below is Kotlin's version of a many-way if/else.)
     */
    fun isLegalDelivery(event: BallEvent, rules: MatchRules): Boolean = when (event) {
        is BallEvent.Runs, is BallEvent.Wicket,
        is BallEvent.Bye,  is BallEvent.LegBye -> true
        is BallEvent.Wide                      -> rules.wideIsLegalBall
        is BallEvent.NoBall                    -> false
    }

    /**
     * A "free hit" follows a no-ball: the next batter cannot be given out (except run-out).
     * To find out whether we are on a free hit we look at the most recent event that is not a
     * wide: if it was a no-ball, the free hit is still on. So a wide bowled during a free hit
     * does not use the free hit up, and it ends after the next proper ball.
     */
    fun isFreeHit(innings: Innings, rules: MatchRules): Boolean =
        rules.freeHitOnNoBall && innings.events.lastOrNull { it !is BallEvent.Wide } is BallEvent.NoBall

    /**
     * After a wicket: which batters could walk in next? Anyone who is not out and is not already
     * at the crease. Returns their positions in the batting list (their "indices").
     */
    fun nextBatterCandidates(innings: Innings): List<Int> =
        innings.batters.indices.filter { i ->
            !innings.batters[i].isOut && i != innings.strikerIndex && i != innings.nonStrikerIndex
        }

    // ─────────────────────────────────────────────────────────────────────────
    // APPLY ONE EVENT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adds ONE event (one ball) to the innings and returns the NEW innings. This is the most
     * important function in the app. It never changes [innings] itself.
     *
     * Which bowler gets the blame or credit comes from the event, not from "whoever is bowling
     * now". That way, if the bowler changes mid-innings and the scorer then presses Undo, every
     * ball is still credited to the right bowler when the innings is replayed.
     */
    fun applyEvent(innings: Innings, event: BallEvent, rules: MatchRules): Innings {
        // Work on local copies of the numbers, change them as the ball requires, then build the
        // final new Innings at the bottom. ("var" can be reassigned; "val" cannot.)
        var batters    = innings.batters
        var bowlers    = innings.bowlers
        var runs       = innings.runs
        var wickets    = innings.wickets
        var balls      = innings.balls
        var extras     = innings.extras
        var striker    = innings.strikerIndex
        var nonStriker = innings.nonStrikerIndex
        var awaitingNextBatter = false

        // "legal" = does this ball use up one of the six balls in the over?
        val bowler = event.bowlerIndex
        val legal  = isLegalDelivery(event, rules)

        // When the batters cross (odd number of runs, or end of over) the striker and non-striker
        // trade places.
        fun swapEnds() { val t = striker; striker = nonStriker; nonStriker = t }

        // One branch per kind of event. Kotlin checks we have covered every kind (BallEvent is a
        // "sealed" class), so adding a new event type without handling it here is a compile error.
        when (event) {

            // A normal legal ball. All the runs came off the bat.
            is BallEvent.Runs -> {
                balls++
                batters = batters.updatedAt(striker) { it.facedBall(event.runs) }
                bowlers = bowlers.updatedAt(bowler)  { it.copy(ballsBowled = it.ballsBowled + 1, runsConceded = it.runsConceded + event.runs) }
                runs += event.runs
                if (event.runs % 2 != 0) swapEnds()           // odd runs → batters cross
            }

            // A wide: not the batter's fault, so it goes to extras and to the bowler's runs conceded.
            is BallEvent.Wide -> {
                val penalty = if (rules.wideGivesRun) 1 else 0
                val total   = penalty + event.extras
                if (legal) balls++                            // street rule: wide eats a ball
                bowlers = bowlers.updatedAt(bowler) {
                    it.copy(
                        ballsBowled  = it.ballsBowled + if (legal) 1 else 0,
                        runsConceded = it.runsConceded + total,
                        widesBowled  = it.widesBowled + 1,
                    )
                }
                extras += total; runs += total                // wides are all extras
                if (event.extras % 2 != 0) swapEnds()         // runs actually run on a wide
            }

            // A no-ball: the batter DID face it, so any runs scored count to the batter. The penalty
            // run (if the rule is on) is an extra. It does not use up a ball of the over.
            is BallEvent.NoBall -> {
                val penalty = if (rules.noBallGivesRun) 1 else 0
                val total   = penalty + event.extras
                // The ball is not legal (over doesn't advance) but the batter did face it.
                batters = batters.updatedAt(striker) { it.facedBall(event.extras) }
                bowlers = bowlers.updatedAt(bowler)  {
                    it.copy(runsConceded = it.runsConceded + total, noBallsBowled = it.noBallsBowled + 1)
                }
                extras += penalty; runs += total              // only the penalty is an "extra"
                if (event.extras % 2 != 0) swapEnds()
            }

            // A dismissal. The batter who is out may be the striker or (for a run-out) the non-striker.
            is BallEvent.Wicket -> {
                balls++
                val out     = event.batter
                val credits = event.kind.creditsBowler

                // The striker always faces the ball and keeps any runs completed
                // (matters for a run-out where the NON-striker is the one dismissed).
                batters = batters.updatedAt(striker) { it.facedBall(event.runsOnBall) }
                batters = batters.updatedAt(out) {
                    it.copy(isOut = true, dismissal = event.kind, dismissedByBowler = if (credits) bowler else -1)
                }
                bowlers = bowlers.updatedAt(bowler) {
                    it.copy(
                        ballsBowled  = it.ballsBowled + 1,
                        runsConceded = it.runsConceded + event.runsOnBall,
                        wicketsTaken = it.wicketsTaken + if (credits) 1 else 0,
                    )
                }
                runs += event.runsOnBall; wickets++

                // Batters who completed an odd number of runs had crossed before the dismissal.
                if (event.runsOnBall % 2 != 0) swapEnds()

                // "waiting" = batters who have not batted yet and could come in as the replacement.
                val waiting = batters.indices.filter { i ->
                    !batters[i].isOut && i != striker && i != nonStriker
                }
                when {
                    // Case 1: someone is left to come in.
                    waiting.isNotEmpty() -> {
                        // Replay path: the replacement was already recorded on the event.
                        if (event.nextBatter in waiting) {
                            if (striker == out) striker = event.nextBatter else nonStriker = event.nextBatter
                        } else {
                            awaitingNextBatter = true         // live path: scorer must choose
                        }
                    }
                    // Case 2: nobody is left, but the house rule says the last batter carries on alone.
                    rules.lastBatsmanAlone -> {
                        // Nobody left to come in: the survivor carries on alone, at both "ends".
                        val survivor = if (striker == out) nonStriker else striker
                        if (survivor != out && !batters[survivor].isOut) {
                            striker = survivor; nonStriker = survivor
                        }
                    }
                    // else: all out — checkCompletion ends the innings.
                }
            }

            // A bye: the ball missed everyone and the batters ran. Extras for the batting side; the
            // bowler is not charged runs (but the ball still counts in his over).
            is BallEvent.Bye -> {
                balls++
                bowlers = bowlers.updatedAt(bowler) { it.copy(ballsBowled = it.ballsBowled + 1) } // byes aren't the bowler's runs
                extras += event.runs; runs += event.runs
                if (event.runs % 2 != 0) swapEnds()
            }

            // A leg bye: the ball hit the batter's body (not the bat) and they ran. Treated like a bye.
            is BallEvent.LegBye -> {
                balls++
                bowlers = bowlers.updatedAt(bowler) { it.copy(ballsBowled = it.ballsBowled + 1) }
                extras += event.runs; runs += event.runs
                if (event.runs % 2 != 0) swapEnds()
            }
        }

        // End of over → the non-striker faces the next over. (No-op for a lone batter.)
        // (Overs are 6 legal balls. When the 6th is bowled, the other batter faces the next over.)
        if (legal && balls % 6 == 0) swapEnds()

        // Build the new Innings: same as the old one except for the fields listed here.
        // "copy" is how immutable objects get "changed".
        return innings.copy(
            batters            = batters,
            bowlers            = bowlers,
            runs               = runs,
            wickets            = wickets,
            balls              = balls,
            extras             = extras,
            strikerIndex       = striker,
            nonStrikerIndex    = nonStriker,
            awaitingNextBatter = awaitingNextBatter,
            events             = innings.events + event,
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NEXT BATTER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The scorer has chosen who walks in after a wicket. We put that player into the empty spot
     * and ALSO write the choice onto the wicket event itself. Storing it there means that when the
     * innings is replayed (for undo), the same replacement is chosen automatically.
     * The empty spot is found by looking for the dismissed player; it can be either end.
     * An invalid choice is ignored: the innings is returned unchanged.
     */
    fun selectNextBatter(innings: Innings, batterIndex: Int): Innings {
        if (!innings.awaitingNextBatter || batterIndex !in nextBatterCandidates(innings)) return innings

        val strikerIsOut = innings.batters[innings.strikerIndex].isOut
        val lastWicket   = innings.events.indexOfLast { it is BallEvent.Wicket }
        val events = innings.events.mapIndexed { i, e ->
            if (i == lastWicket && e is BallEvent.Wicket) e.copy(nextBatter = batterIndex) else e
        }
        return innings.copy(
            strikerIndex       = if (strikerIsOut) batterIndex else innings.strikerIndex,
            nonStrikerIndex    = if (strikerIsOut) innings.nonStrikerIndex else batterIndex,
            awaitingNextBatter = false,
            events             = events,
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPLETION & RESULT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Decides whether the innings is over, and why. It ends when:
     *  - the chasing side reaches the target ("target" is null in the first innings),
     *  - all the overs have been bowled, or
     *  - the batting side is all out (the house rule "last batsman alone" lets one batter
     *    continue, so "all out" then means nobody is left at all).
     */
    fun checkCompletion(innings: Innings, rules: MatchRules, target: Int?): Innings {
        val stillIn   = innings.batters.count { !it.isOut }
        val minNeeded = if (rules.lastBatsmanAlone) 1 else 2
        val reason = when {
            target != null && innings.runs >= target -> "Target chased!"
            innings.balls >= rules.totalOvers * 6    -> "Overs complete"
            stillIn < minNeeded                      -> "All out"
            else                                     -> null
        }
        return if (reason != null)
            innings.copy(complete = true, completeReason = reason, awaitingNextBatter = false)
        else innings
    }

    /**
     * The result line shown at the end: "Team won by N runs", "won by N wickets", or a tie.
     * The side batting second wins by wickets in hand, the side batting first by runs.
     */
    fun calcResult(i1: Innings, i2: Innings, rules: MatchRules): String {
        // With "last batsman alone" the final batter counts as a wicket in hand.
        val wicketsInHand = rules.maxWickets - i2.wickets
        return when {
            i2.runs > i1.runs -> "${i2.battingTeam} won by $wicketsInHand wicket${if (wicketsInHand == 1) "" else "s"}"
            i1.runs > i2.runs -> {
                val diff = i1.runs - i2.runs
                "${i1.battingTeam} won by $diff run${if (diff == 1) "" else "s"}"
            }
            else -> "Match tied!"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UNDO — replay the log without its last event
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Rebuilds an innings by starting from nothing and replaying [events] one by one.
     * (This is the "fold": start with a fresh innings, feed in event 1, then event 2...)
     * The current choice of bowler is kept because it is not part of the event list.
     */
    fun rebuild(base: Innings, events: List<BallEvent>, rules: MatchRules): Innings {
        val fresh = newInnings(
            battingTeam = base.battingTeam,
            bowlingTeam = base.bowlingTeam,
            batterNames = base.batters.map { it.name },
            bowlerNames = base.bowlers.map { it.name },
        ).copy(bowlerIndex = base.bowlerIndex)
        return events.fold(fresh) { acc, ev -> applyEvent(acc, ev, rules) }
    }

    /**
     * UNDO. Returns the innings as it was before its last ball, or null if nothing has been
     * bowled yet. It just replays every event except the last one.
     */
    fun withoutLastBall(innings: Innings, rules: MatchRules): Innings? =
        if (innings.events.isEmpty()) null
        else rebuild(innings, innings.events.dropLast(1), rules)

    // ─────────────────────────────────────────────────────────────────────────
    // OVER STRIP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Works out which balls to show in the "This over" strip: the over being bowled now, or,
     * straight after the sixth legal ball, the over that has just finished (so the scorer can
     * check it before the next over starts). Wides and no-balls belong to the over that was
     * current when they were bowled.
     */
    fun currentOver(innings: Innings, rules: MatchRules): OverView {
        val lastEvent = innings.events.lastOrNull()
        val justFinished = innings.balls > 0 && innings.balls % 6 == 0 &&
                lastEvent != null && isLegalDelivery(lastEvent, rules)
        val target = if (justFinished) innings.balls / 6 - 1 else innings.balls / 6

        var legalSoFar = 0
        val inOver = mutableListOf<BallEvent>()
        for (e in innings.events) {
            if (legalSoFar / 6 == target) inOver += e
            if (isLegalDelivery(e, rules)) legalSoFar++
        }
        return OverView(overIndex = target, events = inOver, complete = justFinished)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PLAYER OF THE MATCH
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Suggests a Player of the Match by giving every player points and picking the highest.
     *  - Batting: 1 point per run, plus a small bonus for a fast strike rate (only counted after
     *    at least 3 balls faced, so one lucky hit does not win it).
     *  - Bowling: 25 points per wicket, plus a bonus for a low economy rate (only after at least
     *    one full over).
     * Only players from the winning side are eligible (either side if the match was tied).
     * Returns null if nobody scored any points.
     */
    fun suggestPotm(i1: Innings, i2: Innings): PotmPick? {
        val winner = when {
            i2.runs > i1.runs -> i2.battingTeam
            i1.runs > i2.runs -> i1.battingTeam
            else              -> null // tie: anyone is eligible
        }
        // A scoreboard of points. The key is (team name, player number); the value is their points.
        val scores = HashMap<Pair<String, Int>, Float>()
        fun add(team: String, id: Int, pts: Float) {
            if (winner == null || team == winner) {
                val key = team to id
                scores[key] = (scores[key] ?: 0f) + pts
            }
        }
        fun batting(inn: Innings) = inn.batters.forEach { p ->
            // Strike-rate bonus only counts once there is a meaningful sample.
            add(inn.battingTeam, p.id, p.runsScored + if (p.ballsFaced >= 3) p.strikeRate / 10f else 0f)
        }
        fun bowling(inn: Innings) = inn.bowlers.forEach { p ->
            val econBonus = if (p.ballsBowled >= 6) maxOf(0f, 12f - p.economy) else 0f
            add(inn.bowlingTeam, p.id, p.wicketsTaken * 25f + econBonus)
        }
        batting(i1); batting(i2); bowling(i1); bowling(i2)

        val best = scores.maxByOrNull { it.value } ?: return null
        if (best.value <= 0f) return null
        return PotmPick(team = best.key.first, playerId = best.key.second)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    // Every kind of event stores which bowler bowled it, under the same name "bowler". This
    // small helper lets the code above ask any event "who bowled you?" without checking its type.
    private val BallEvent.bowlerIndex: Int
        get() = when (this) {
            is BallEvent.Runs    -> bowler
            is BallEvent.Wide    -> bowler
            is BallEvent.NoBall  -> bowler
            is BallEvent.Wicket  -> bowler
            is BallEvent.Bye     -> bowler
            is BallEvent.LegBye  -> bowler
        }

    /**
     * Returns a copy of the list in which only the player at [index] has been changed by
     * [transform]; everyone else is left exactly as they were.
     */
    private inline fun List<Player>.updatedAt(index: Int, transform: (Player) -> Player): List<Player> =
        mapIndexed { i, p -> if (i == index) transform(p) else p }

    /** A batter faces one ball and scores [batRuns] off the bat (counting fours and sixes). */
    private fun Player.facedBall(batRuns: Int): Player = copy(
        runsScored = runsScored + batRuns,
        ballsFaced = ballsFaced + 1,
        fours      = fours + if (batRuns == 4) 1 else 0,
        sixes      = sixes + if (batRuns == 6) 1 else 0,
    )
}
