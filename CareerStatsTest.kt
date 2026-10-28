package com.waleedahmedja.score247.engine

import com.waleedahmedja.score247.data.model.BallEvent
import com.waleedahmedja.score247.data.model.DismissalKind
import com.waleedahmedja.score247.data.model.Innings
import com.waleedahmedja.score247.data.model.MatchRecord
import com.waleedahmedja.score247.data.model.MatchRules
import com.waleedahmedja.score247.data.model.MatchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for turning saved matches into career numbers. */
class CareerStatsTest {

    private val rules = MatchRules(totalOvers = 5, playersPerTeam = 3)

    private fun play(i: Innings, e: BallEvent) = ScoringEngine.applyEvent(i, e, rules)

    /**
     * Ali (A's opener) scores 4 + 6 off Zed, then is bowled by Zed. Bilal comes in, faces a dot.
     * Innings 2 is just one dot ball (Zed now bats, Ali bowls).
     */
    private fun sampleRecord(id: Long = 1L, aliName: String = "Ali"): MatchRecord {
        var i1 = ScoringEngine.newInnings("A", "B", listOf(aliName, "Bilal", "Player 3"), listOf("Zed", "Yusuf", "Player 3"))
        i1 = play(i1, BallEvent.Runs(4, 0, 0))
        i1 = play(i1, BallEvent.Runs(6, 0, 0))
        i1 = play(i1, BallEvent.Wicket(0, 0, 0, DismissalKind.BOWLED))
        i1 = ScoringEngine.selectNextBatter(i1, 2)
        var i2 = ScoringEngine.newInnings("B", "A", listOf("Zed", "Yusuf", "Player 3"), listOf(aliName, "Bilal", "Player 3"))
        i2 = play(i2, BallEvent.Runs(0, 0, 0))
        return MatchRecord(id, 1000L * id, MatchState(id = id, teamA = "A", teamB = "B", rules = rules, innings1 = i1, innings2 = i2))
    }

    private fun career(records: List<MatchRecord>, name: String) =
        CareerStats.compute(records).firstOrNull { it.name.equals(name, ignoreCase = true) }

    @Test fun batting_totals_are_added_up() {
        val ali = career(listOf(sampleRecord()), "Ali")!!
        assertEquals(10, ali.runs)
        assertEquals(3, ali.ballsFaced)
        assertEquals(1, ali.fours); assertEquals(1, ali.sixes)
        assertEquals(10, ali.highScore); assertFalse(ali.highScoreNotOut)
        assertEquals(1, ali.battingInnings)
        assertEquals(0, ali.notOuts)
    }

    @Test fun average_needs_a_dismissal_and_not_outs_are_excluded() {
        val ali = career(listOf(sampleRecord()), "Ali")!!
        assertEquals(10f, ali.battingAverage!!, 0.001f)       // 10 runs, out once
        val zed = career(listOf(sampleRecord()), "Zed")!!
        assertNull(zed.battingAverage)                          // batted, never out
        assertEquals(1, zed.notOuts)
    }

    @Test fun bowling_totals_and_best_figures() {
        val zed = career(listOf(sampleRecord()), "Zed")!!
        assertEquals(1, zed.wickets)
        assertEquals(3, zed.ballsBowled)
        assertEquals(10, zed.runsConceded)
        assertEquals("1/10", zed.bestBowling)
        assertEquals("0.3", zed.oversBowled)
    }

    @Test fun same_player_across_matches_is_merged_ignoring_case_and_spaces() {
        val all = CareerStats.compute(listOf(sampleRecord(2, "  ali "), sampleRecord(1, "Ali")))
        val ali = all.filter { it.name.equals("ali", ignoreCase = true) }
        assertEquals(1, ali.size)
        assertEquals(2, ali[0].matches)
        assertEquals(20, ali[0].runs)
        assertEquals("ali", ali[0].name)      // newest record first → its spelling wins
    }

    @Test fun placeholder_names_are_skipped() {
        val names = CareerStats.compute(listOf(sampleRecord())).map { it.name }
        assertFalse(names.any { it.startsWith("Player") })
        assertTrue(CareerStats.isRealName("Ali"))
        assertFalse(CareerStats.isRealName("player 12"))
        assertFalse(CareerStats.isRealName("   "))
    }

    @Test fun leaderboards_are_sorted_and_filtered() {
        val all = CareerStats.compute(listOf(sampleRecord()))
        val bat = CareerStats.battingLeaders(all)
        assertEquals("Ali", bat.first().name)
        assertTrue(bat.zipWithNext().all { (a, b) -> a.runs >= b.runs })
        val bowl = CareerStats.bowlingLeaders(all)
        assertEquals("Zed", bowl.first().name)
        assertTrue(bowl.all { it.ballsBowled > 0 })
    }

    @Test fun no_matches_means_no_players() {
        assertTrue(CareerStats.compute(emptyList()).isEmpty())
        assertNotNull(PlayerCareer("x", 0, 0, 0, 0, 0, 0, 0, 0, false, 0, 0, 0, 0, 0))
    }
}
