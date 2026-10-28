package com.waleedahmedja.score247.util

import com.waleedahmedja.score247.data.model.Innings
import com.waleedahmedja.score247.data.model.MatchState

// ─────────────────────────────────────────────────────────────────────────────
// ShareSummary.kt: the few facts a shared scorecard picture needs.
//
// The picture-drawing code (ScorecardImage.kt) only knows how to paint text and shapes. This
// file decides WHAT to print: the result, each side's score, the best batter and bowler of
// each innings, and the Player of the Match. It is plain Kotlin with no Android in it, so it
// is unit-tested like the scoring engine.
// ─────────────────────────────────────────────────────────────────────────────

/** Where the app lives. Shown in the share caption and encoded in the picture's QR code. */
object AppLinks {
    const val REPO_URL = "https://github.com/waleedahmedja/Score247"
    const val NAME = "Score247"
}

/** One side's line on the picture, e.g. team "Tigers", score "112/4", overs "5.0". */
data class InningsLine(
    val team: String,
    val score: String,
    val overs: String,
    val runRate: String,
    /** e.g. "Sam 45 (20)" or null if nobody batted. */
    val topBatter: String?,
    /** e.g. "Ali 3/12 (2.0 ov)" or null if nobody bowled. */
    val topBowler: String?,
)

data class ShareSummary(
    val title: String,                    // "Tigers vs Lions"
    val result: String,                   // "Tigers won by 12 runs"
    val innings: List<InningsLine>,       // one or two entries, in batting order
    val potmName: String?,                // Player of the Match, if one was picked
    val potmStats: String?,               // e.g. "34 (18) • 2/12"
)

object ShareSummaryBuilder {

    fun from(state: MatchState): ShareSummary {
        val potm = potmLine(state)
        return ShareSummary(
            title = "${state.teamA} vs ${state.teamB}",
            result = state.result.ifBlank { "Match in progress" },
            innings = listOfNotNull(state.innings1, state.innings2).map(::lineFor),
            potmName = potm?.first,
            potmStats = potm?.second?.takeIf { it.isNotEmpty() },
        )
    }

    private fun lineFor(inn: Innings) = InningsLine(
        team = inn.battingTeam,
        score = "${inn.runs}/${inn.wickets}",
        overs = inn.oversDisplay,
        runRate = inn.runRate.oneDecimal(),
        topBatter = topBatter(inn),
        topBowler = topBowler(inn),
    )

    /** Most runs; on a tie, the one who needed fewer balls. */
    fun topBatter(inn: Innings): String? =
        inn.batters.withIndex()
            .filter { (i, _) -> inn.hasBatted(i) }
            .maxWithOrNull(compareBy({ it.value.runsScored }, { -it.value.ballsFaced }))
            ?.value
            ?.let { "${it.name} ${it.runsScored}${if (it.isOut) "" else "*"} (${it.ballsFaced})" }

    /** Most wickets; on a tie, the one who conceded fewer runs. */
    fun topBowler(inn: Innings): String? =
        inn.bowlers.filter { it.ballsBowled > 0 }
            .maxWithOrNull(compareBy({ it.wicketsTaken }, { -it.runsConceded }))
            ?.let { "${it.name} ${it.wicketsTaken}/${it.runsConceded} (${it.oversBowled} ov)" }

    /** The text sent along with the picture (WhatsApp shows it as the caption). */
    fun caption(state: MatchState): String = buildString {
        append("🏏 ${state.teamA} vs ${state.teamB}")
        if (state.result.isNotBlank()) append("\n${state.result}")
        append("\n\nScored with ${AppLinks.NAME} — free and open source:\n${AppLinks.REPO_URL}")
    }
}
