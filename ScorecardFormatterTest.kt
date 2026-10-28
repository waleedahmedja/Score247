package com.waleedahmedja.score247.util

import com.waleedahmedja.score247.data.model.BallEvent
import com.waleedahmedja.score247.data.model.DismissalKind
import com.waleedahmedja.score247.data.model.MatchPhase
import com.waleedahmedja.score247.data.model.MatchRules
import com.waleedahmedja.score247.data.model.MatchState
import com.waleedahmedja.score247.engine.ScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScorecardFormatterTest {

    private val rules = MatchRules(totalOvers = 2, playersPerTeam = 3)
    private fun names(prefix: String) = List(3) { "$prefix$it" }

    @Test fun dismissal_text_reads_like_a_scorecard() {
        var i = ScoringEngine.newInnings("A", "B", names("Bat"), names("Bowl"))
        i = ScoringEngine.applyEvent(i, BallEvent.Wicket(0, 1, 0, DismissalKind.CAUGHT), rules)
        assertEquals("caught b Bowl1", i.batters[0].dismissalText(i.bowlers))
        assertEquals("not out", i.batters[1].dismissalText(i.bowlers))
        i = ScoringEngine.selectNextBatter(i, 2)
        i = ScoringEngine.applyEvent(i, BallEvent.Wicket(1, 1, 0, DismissalKind.RUN_OUT), rules)
        assertEquals("run out", i.batters[1].dismissalText(i.bowlers))
    }

    @Test fun share_text_contains_result_scores_and_potm() {
        var i1 = ScoringEngine.newInnings("A", "B", names("A"), names("B"))
        i1 = ScoringEngine.applyEvent(i1, BallEvent.Runs(6, 0, 0), rules)
        i1 = ScoringEngine.applyEvent(i1, BallEvent.Runs(4, 0, 0), rules)
        var i2 = ScoringEngine.newInnings("B", "A", names("B"), names("A"))
        i2 = ScoringEngine.applyEvent(i2, BallEvent.Runs(2, 0, 0), rules)

        val pick = ScoringEngine.suggestPotm(i1, i2)!!
        val state = MatchState(
            teamA = "A", teamB = "B", innings1 = i1, innings2 = i2, phase = MatchPhase.COMPLETED,
            result = ScoringEngine.calcResult(i1, i2, rules),
            potmPlayerId = pick.playerId, potmTeam = pick.team,
        )
        val text = shareScorecard(state)
        assertTrue(text, "A won by 8 runs" in text)
        assertTrue(text, "A: 10/0 (0.2 ov)" in text)
        assertTrue(text, "A0 10* (2)  1x4 1x6" in text)
        assertTrue(text, "Player of the Match: A0" in text)
        assertEquals("A0", potmLine(state)!!.first)
    }

    @Test fun number_formatting_is_locale_independent() {
        assertEquals("7.5", 7.5f.oneDecimal())
        assertEquals("133", 133.33f.noDecimal())
        assertEquals(null, requiredRate(0, 10))
        assertEquals(12f, requiredRate(12, 6)!!, 0.001f)
    }
}
