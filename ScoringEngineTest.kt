package com.waleedahmedja.score247.engine

import com.waleedahmedja.score247.data.model.BallEvent
import com.waleedahmedja.score247.data.model.DismissalKind
import com.waleedahmedja.score247.data.model.Innings
import com.waleedahmedja.score247.data.model.MatchRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Plain JUnit tests for the pure scoring engine — no emulator needed.
 * Run with: ./gradlew test
 */
class ScoringEngineTest {

    private val rules = MatchRules(totalOvers = 5, playersPerTeam = 6)

    // ── helpers ───────────────────────────────────────────────────────────────
    private fun innings(batters: Int = 6) = ScoringEngine.newInnings(
        "A", "B",
        (0 until batters).map { "Bat$it" },
        (0 until 4).map { "Bowl$it" },
    )

    private fun Innings.play(e: BallEvent, r: MatchRules = rules) = ScoringEngine.applyEvent(this, e, r)
    private fun Innings.runs(n: Int, r: MatchRules = rules) = play(BallEvent.Runs(n, strikerIndex, bowlerIndex), r)
    private fun Innings.dots(n: Int, r: MatchRules = rules): Innings {
        var i = this; repeat(n) { i = i.runs(0, r) }; return i
    }
    private fun Innings.wicket(
        kind: DismissalKind = DismissalKind.BOWLED, runs: Int = 0, out: Int = strikerIndex,
    ) = play(BallEvent.Wicket(out, bowlerIndex, runs, kind))

    // ── strike rotation ───────────────────────────────────────────────────────
    @Test fun odd_runs_swap_strike_even_do_not() {
        val a = innings().runs(1)
        assertEquals(1, a.strikerIndex); assertEquals(0, a.nonStrikerIndex)
        val b = a.runs(2)
        assertEquals(1, b.strikerIndex)
    }

    @Test fun end_of_over_swaps_strike() {
        val i = innings().dots(6)
        assertEquals(1, i.strikerIndex); assertEquals(0, i.nonStrikerIndex)
        assertEquals("1.0", i.oversDisplay)
    }

    @Test fun single_off_last_ball_leaves_same_batter_on_strike() {
        val i = innings().dots(5).runs(1)   // cross, then end-of-over cross back
        assertEquals(0, i.strikerIndex)
    }

    // ── extras ────────────────────────────────────────────────────────────────
    @Test fun wide_adds_run_and_is_not_a_ball_by_default() {
        val i = innings().play(BallEvent.Wide(0, 0))
        assertEquals(1, i.runs); assertEquals(1, i.extras); assertEquals(0, i.balls)
        assertEquals(1, i.bowlers[0].runsConceded); assertEquals(1, i.bowlers[0].widesBowled)
        assertEquals(0, i.bowlers[0].ballsBowled)
        assertEquals(0, i.batters[0].ballsFaced)
    }

    @Test fun wide_as_legal_ball_counts_for_over_and_bowler() {
        val r = rules.copy(wideIsLegalBall = true)
        val i = innings().dots(5, r).play(BallEvent.Wide(0, 0), r)
        assertEquals(6, i.balls); assertEquals(6, i.bowlers[0].ballsBowled)
        assertEquals(1, i.strikerIndex)          // over ended on the wide → strike changes
    }

    @Test fun wide_without_penalty_rule_is_free() {
        val r = rules.copy(wideGivesRun = false)
        assertEquals(0, innings().play(BallEvent.Wide(0, 0), r).runs)
    }

    @Test fun runs_run_on_a_wide_can_change_strike() {
        val i = innings().play(BallEvent.Wide(1, 0))
        assertEquals(2, i.runs); assertEquals(1, i.strikerIndex)
    }

    @Test fun no_ball_does_not_advance_over_and_credits_bat_runs() {
        val i = innings().play(BallEvent.NoBall(4, 0, 0))
        assertEquals(5, i.runs); assertEquals(1, i.extras)   // penalty only is an extra
        assertEquals(0, i.balls)
        assertEquals(4, i.batters[0].runsScored); assertEquals(1, i.batters[0].fours)
        assertEquals(1, i.batters[0].ballsFaced)
        assertEquals(5, i.bowlers[0].runsConceded); assertEquals(1, i.bowlers[0].noBallsBowled)
    }

    @Test fun byes_and_leg_byes_are_not_charged_to_the_bowler() {
        val i = innings().play(BallEvent.Bye(2, 0)).play(BallEvent.LegBye(1, 0))
        assertEquals(3, i.runs); assertEquals(3, i.extras)
        assertEquals(0, i.bowlers[0].runsConceded); assertEquals(2, i.bowlers[0].ballsBowled)
        assertEquals(0, i.batters[0].runsScored + i.batters[1].runsScored)
    }

    @Test fun free_hit_follows_no_ball_and_survives_a_wide() {
        var i = innings()
        assertFalse(ScoringEngine.isFreeHit(i, rules))
        i = i.play(BallEvent.NoBall(0, 0, 0)); assertTrue(ScoringEngine.isFreeHit(i, rules))
        i = i.play(BallEvent.Wide(0, 0));      assertTrue(ScoringEngine.isFreeHit(i, rules))
        i = i.runs(0);                         assertFalse(ScoringEngine.isFreeHit(i, rules))
        assertFalse(ScoringEngine.isFreeHit(innings().play(BallEvent.NoBall(0, 0, 0)), rules.copy(freeHitOnNoBall = false)))
    }

    // ── wickets ───────────────────────────────────────────────────────────────
    @Test fun wicket_blocks_scoring_until_next_batter_chosen() {
        val w = innings().wicket()
        assertTrue(w.awaitingNextBatter)
        assertEquals(1, w.wickets); assertTrue(w.batters[0].isOut)
        assertEquals(1, w.bowlers[0].wicketsTaken)
        assertEquals(listOf(2, 3, 4, 5), ScoringEngine.nextBatterCandidates(w))

        val n = ScoringEngine.selectNextBatter(w, 3)
        assertFalse(n.awaitingNextBatter)
        assertEquals(3, n.strikerIndex); assertEquals(1, n.nonStrikerIndex)
    }

    @Test fun invalid_next_batter_choices_are_ignored() {
        val w = innings().wicket()
        assertEquals(w, ScoringEngine.selectNextBatter(w, 0))   // the dismissed player
        assertEquals(w, ScoringEngine.selectNextBatter(w, 1))   // already at the crease
        assertEquals(w, ScoringEngine.selectNextBatter(w, 99))
    }

    /** Regression: the original app lost the surviving batter in exactly this situation. */
    @Test fun wicket_on_last_ball_of_over_keeps_the_surviving_batter() {
        val w = innings().dots(5).wicket()
        assertTrue(w.awaitingNextBatter)
        val n = ScoringEngine.selectNextBatter(w, 2)
        assertEquals(setOf(1, 2), setOf(n.strikerIndex, n.nonStrikerIndex))
        assertEquals("survivor faces the new over", 1, n.strikerIndex)
    }

    @Test fun run_out_of_non_striker_is_not_a_bowler_wicket() {
        val w = innings().wicket(DismissalKind.RUN_OUT, runs = 1, out = 1)
        assertEquals(0, w.bowlers[0].wicketsTaken)
        assertTrue(w.batters[1].isOut); assertFalse(w.batters[0].isOut)
        assertEquals(1, w.batters[0].runsScored)             // striker keeps the run
        assertEquals(1, w.runs)
        // One run completed → batters had crossed, so the old striker is now at the non-striker end.
        val n = ScoringEngine.selectNextBatter(w, 2)
        assertEquals(1, n.runs)
        assertEquals(setOf(0, 2), setOf(n.strikerIndex, n.nonStrikerIndex))
    }

    @Test fun dismissal_records_how_and_who() {
        val w = innings().wicket(DismissalKind.CAUGHT)
        assertEquals(DismissalKind.CAUGHT, w.batters[0].dismissal)
        assertEquals(0, w.batters[0].dismissedByBowler)
        val r = innings().wicket(DismissalKind.RUN_OUT)
        assertEquals(-1, r.batters[0].dismissedByBowler)
    }

    // ── last batsman / all out ────────────────────────────────────────────────
    private fun fallOfWickets(i0: Innings, count: Int, r: MatchRules): Innings {
        var i = i0
        repeat(count) {
            i = i.wicket()
            if (i.awaitingNextBatter) i = ScoringEngine.selectNextBatter(i, ScoringEngine.nextBatterCandidates(i).first())
        }
        return i
    }

    @Test fun last_batsman_alone_keeps_batting_then_is_all_out() {
        val r = rules.copy(lastBatsmanAlone = true)                    // 6 players
        var i = fallOfWickets(innings(), 4, r)                         // 2 left at the crease
        assertFalse(ScoringEngine.checkCompletion(i, r, null).complete)
        i = i.wicket()                                                 // 5th wicket, nobody waiting
        assertFalse(i.awaitingNextBatter)
        assertEquals(i.strikerIndex, i.nonStrikerIndex)                // lone batter at both ends
        assertFalse(i.batters[i.strikerIndex].isOut)
        assertFalse(ScoringEngine.checkCompletion(i, r, null).complete)

        val lone = i.runs(1)                                           // no strike change, no crash
        assertEquals(lone.strikerIndex, lone.nonStrikerIndex)

        val end = ScoringEngine.checkCompletion(i.wicket(), r, null)   // 6th wicket
        assertTrue(end.complete); assertEquals("All out", end.completeReason)
        assertEquals(6, end.wickets)
    }

    @Test fun without_last_batsman_rule_innings_ends_one_wicket_earlier() {
        val r = rules.copy(lastBatsmanAlone = false)
        val i = fallOfWickets(innings(), 5, r)
        val end = ScoringEngine.checkCompletion(i, r, null)
        assertTrue(end.complete); assertEquals("All out", end.completeReason)
        assertEquals(5, end.wickets)
    }

    // ── completion & result ───────────────────────────────────────────────────
    @Test fun innings_ends_when_overs_are_up_or_target_is_chased() {
        val over = ScoringEngine.checkCompletion(innings().dots(30), rules, null)
        assertTrue(over.complete); assertEquals("Overs complete", over.completeReason)

        val chase = ScoringEngine.checkCompletion(innings().runs(6), rules, target = 6)
        assertTrue(chase.complete); assertEquals("Target chased!", chase.completeReason)

        assertFalse(ScoringEngine.checkCompletion(innings().runs(4), rules, target = 6).complete)
    }

    /** Regression: the original said "won by 0 wickets" when the last man finished the chase. */
    @Test fun result_counts_the_last_batsman_as_a_wicket_in_hand() {
        val r  = rules.copy(playersPerTeam = 3, lastBatsmanAlone = true)
        val i1 = innings().copy(runs = 10)
        val i2 = innings().copy(battingTeam = "B", bowlingTeam = "A", runs = 11, wickets = 2)
        assertEquals("B won by 1 wicket", ScoringEngine.calcResult(i1, i2, r))
        assertEquals("B won by 3 wickets", ScoringEngine.calcResult(i1, i2.copy(wickets = 0), r))
        // Without the last-man rule 3 players can lose only 2 wickets, so 1 lost leaves 1 in hand.
        assertEquals("B won by 1 wicket",
            ScoringEngine.calcResult(i1, i2.copy(wickets = 1), r.copy(lastBatsmanAlone = false)))
        assertEquals("A won by 1 run", ScoringEngine.calcResult(i1.copy(runs = 12), i2, r))
        assertEquals("Match tied!", ScoringEngine.calcResult(i1.copy(runs = 11), i2, r))
    }

    // ── undo ──────────────────────────────────────────────────────────────────
    /** Regression: undo used to leave both batters on player 0 and give every ball to bowler 0. */
    @Test fun undo_restores_exact_previous_state_with_bowler_change_and_wicket() {
        var i = innings().runs(1).runs(4)
        i = i.copy(bowlerIndex = 2)
        i = i.runs(2).dots(3)                       // over complete, strike swapped
        i = i.wicket()
        i = ScoringEngine.selectNextBatter(i, 2)
        i = i.copy(bowlerIndex = 3)
        val beforeLast = i
        val afterLast  = i.runs(6)

        val undone = ScoringEngine.withoutLastBall(afterLast, rules)
        assertNotNull(undone)
        assertEquals(beforeLast, undone)
        // Every earlier ball is still credited to the bowler who actually bowled it.
        assertEquals(2, undone!!.bowlers[0].ballsBowled)
        assertEquals(5, undone.bowlers[2].ballsBowled)
        assertEquals(0, undone.bowlers[3].ballsBowled)
    }

    @Test fun undoing_a_wicket_brings_the_batter_back() {
        val w = innings().runs(3).wicket()
        val u = ScoringEngine.withoutLastBall(w, rules)!!
        assertFalse(u.awaitingNextBatter); assertEquals(0, u.wickets)
        assertFalse(u.batters[1].isOut)
        assertEquals(setOf(0, 1), setOf(u.strikerIndex, u.nonStrikerIndex))
    }

    @Test fun nothing_to_undo_on_a_fresh_innings() {
        assertEquals(null, ScoringEngine.withoutLastBall(innings(), rules))
    }

    // ── over strip ────────────────────────────────────────────────────────────
    @Test fun current_over_only_contains_this_overs_deliveries() {
        var i = innings().dots(6)                                   // over 0 done
        i = i.play(BallEvent.Wide(0, 0)).runs(1).runs(4)            // wide + 2 balls of over 1
        val v = ScoringEngine.currentOver(i, rules)
        assertEquals(1, v.overIndex); assertEquals(3, v.events.size); assertFalse(v.complete)
    }

    @Test fun finished_over_stays_visible_until_the_next_ball() {
        val v = ScoringEngine.currentOver(innings().dots(6), rules)
        assertEquals(0, v.overIndex); assertEquals(6, v.events.size); assertTrue(v.complete)
        val next = ScoringEngine.currentOver(innings().dots(6).play(BallEvent.Wide(0, 0)), rules)
        assertEquals(1, next.overIndex); assertEquals(1, next.events.size)
    }

    // ── player of the match ───────────────────────────────────────────────────
    @Test fun potm_comes_from_the_winning_side() {
        val i1 = innings().copy(runs = 50).let {
            it.copy(batters = it.batters.mapIndexed { n, p -> if (n == 0) p.copy(runsScored = 50, ballsFaced = 20) else p })
        }
        // A scored 50 but B chased 51 → B wins; A's batting hero must NOT be picked.
        val i2 = ScoringEngine.newInnings("B", "A", List(6) { "B$it" }, List(6) { "A$it" }).let {
            it.copy(runs = 51, batters = it.batters.mapIndexed { n, p -> if (n == 2) p.copy(runsScored = 10, ballsFaced = 8) else p })
        }
        val pick = ScoringEngine.suggestPotm(i1, i2)
        assertEquals("B", pick!!.team); assertEquals(2, pick.playerId)
    }

    // ── property tests ────────────────────────────────────────────────────────
    private fun randomEvent(i: Innings, rnd: Random): BallEvent {
        val s = i.strikerIndex; val b = i.bowlerIndex
        return when (rnd.nextInt(10)) {
            0    -> BallEvent.Wide(rnd.nextInt(3), b)
            1    -> BallEvent.NoBall(rnd.nextInt(7), s, b)
            2    -> BallEvent.Bye(1 + rnd.nextInt(4), b)
            3    -> BallEvent.LegBye(1 + rnd.nextInt(4), b)
            4    -> {
                val kind = DismissalKind.values().random(rnd)
                if (kind == DismissalKind.RUN_OUT)
                    BallEvent.Wicket(if (rnd.nextBoolean()) s else i.nonStrikerIndex, b, rnd.nextInt(3), kind)
                else BallEvent.Wicket(s, b, 0, kind)
            }
            else -> BallEvent.Runs(rnd.nextInt(7), s, b)
        }
    }

    /**
     * Plays hundreds of random innings under random house rules. After EVERY ball:
     *  - team runs == sum of batter runs + extras
     *  - wickets == number of batters out; balls == sum of bowler legal balls
     *  - the pair at the crease is valid
     *  - replaying the log reproduces the exact state (so undo can never drift)
     */
    @Test fun random_innings_keep_invariants_and_replay_exactly() {
        val rnd = Random(247)
        repeat(400) { game ->
            val r = MatchRules(
                totalOvers = 6, playersPerTeam = 6,
                wideGivesRun = rnd.nextBoolean(), wideIsLegalBall = rnd.nextBoolean(),
                noBallGivesRun = rnd.nextBoolean(), lastBatsmanAlone = rnd.nextBoolean(),
            )
            var inn = innings()
            val snapshots = mutableListOf<Innings>()
            var guard = 0
            while (!inn.complete && guard++ < 300) {
                if (rnd.nextInt(6) == 0) inn = inn.copy(bowlerIndex = rnd.nextInt(4))
                inn = ScoringEngine.checkCompletion(ScoringEngine.applyEvent(inn, randomEvent(inn, rnd), r), r, null)
                if (inn.awaitingNextBatter) {
                    inn = ScoringEngine.selectNextBatter(inn, ScoringEngine.nextBatterCandidates(inn).random(rnd))
                }
                snapshots += inn

                val tag = "game $game ball ${snapshots.size}"
                assertEquals(tag, inn.batters.sumOf { it.runsScored } + inn.extras, inn.runs)
                assertEquals(tag, inn.batters.count { it.isOut }, inn.wickets)
                assertEquals(tag, inn.bowlers.sumOf { it.ballsBowled }, inn.balls)
                if (!inn.complete) {
                    assertFalse("$tag striker out", inn.batters[inn.strikerIndex].isOut)
                    assertFalse("$tag non-striker out", inn.batters[inn.nonStrikerIndex].isOut)
                    if (inn.strikerIndex == inn.nonStrikerIndex)
                        assertTrue("$tag lone batter only with the rule", r.lastBatsmanAlone)
                }
            }
            // Replay every prefix and compare with what was live.
            snapshots.forEachIndexed { k, live ->
                val replay = ScoringEngine.rebuild(live, inn.events.take(k + 1), r)
                val a = live.copy(complete = false, completeReason = "", bowlerIndex = 0, awaitingNextBatter = false)
                val b = replay.copy(bowlerIndex = 0, awaitingNextBatter = false)
                assertEquals("game $game replay of ball ${k + 1}", a, b)
            }
        }
    }
}
