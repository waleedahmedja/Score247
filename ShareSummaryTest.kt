package com.waleedahmedja.score247.util

import com.waleedahmedja.score247.data.model.BallEvent
import com.waleedahmedja.score247.data.model.MatchRules
import com.waleedahmedja.score247.data.model.MatchState
import com.waleedahmedja.score247.engine.ScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareSummaryTest {

    private val rules = MatchRules(totalOvers = 5, playersPerTeam = 3)

    /** Three balls (4, 2, 4 runs) all faced by opener Ana, who stays on strike (no odd runs). Zed bowls. */
    private fun sampleInnings() = listOf(
        BallEvent.Runs(4, 0, 0),
        BallEvent.Runs(2, 0, 0),
        BallEvent.Runs(4, 1, 0),
    ).fold(ScoringEngine.newInnings("Tigers", "Lions", listOf("Ana", "Ben", "Cy"), listOf("Zed", "Yan", "Xi"))) { acc, e ->
        ScoringEngine.applyEvent(acc, e, rules)
    }

    @Test fun topBatter_isHighestScorerAndMarksNotOut() {
        // Ana: 4 + 2 + 4 = 10 off 3 balls and not out; Ben has faced nothing.
        assertEquals("Ana 10* (3)", ShareSummaryBuilder.topBatter(sampleInnings()))
    }

    @Test fun topBowler_isMostWicketsThenFewestRuns() {
        val inn = sampleInnings()
        // Nobody took a wicket; Zed bowled all three balls for 10 runs and is the only bowler.
        assertEquals("Zed 0/10 (0.3 ov)", ShareSummaryBuilder.topBowler(inn))
    }

    @Test fun topBatter_isNullBeforeAnyBallIsBowled() {
        val fresh = ScoringEngine.newInnings("A", "B", listOf("P", "Q"), listOf("R", "S"))
        // The two openers are at the crease, so they count as having batted.
        assertEquals("P 0* (0)", ShareSummaryBuilder.topBatter(fresh))
    }

    @Test fun topBowler_isNullWhenNobodyBowled() {
        val fresh = ScoringEngine.newInnings("A", "B", listOf("P", "Q"), listOf("R", "S"))
        assertNull(ShareSummaryBuilder.topBowler(fresh))
    }

    @Test fun from_buildsTitleResultAndInningsLines() {
        val state = MatchState(teamA = "Tigers", teamB = "Lions", innings1 = sampleInnings(), result = "Tigers won by 3 runs")
        val s = ShareSummaryBuilder.from(state)
        assertEquals("Tigers vs Lions", s.title)
        assertEquals("Tigers won by 3 runs", s.result)
        assertEquals(1, s.innings.size)
        assertEquals("10/0", s.innings[0].score)
        assertEquals("0.3", s.innings[0].overs)
    }

    @Test fun caption_containsResultAndRepoLink() {
        val state = MatchState(teamA = "Tigers", teamB = "Lions", result = "Tigers won by 3 runs")
        val c = ShareSummaryBuilder.caption(state)
        assertTrue(c.contains("Tigers won by 3 runs"))
        assertTrue(c.contains(AppLinks.REPO_URL))
    }
}
