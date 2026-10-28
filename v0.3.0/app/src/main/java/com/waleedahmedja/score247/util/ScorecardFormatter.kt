package com.waleedahmedja.score247.util

import com.waleedahmedja.score247.data.model.Innings
import com.waleedahmedja.score247.data.model.MatchState
import com.waleedahmedja.score247.data.model.Player
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Number formatting. Locale.US keeps digits Latin and the decimal a dot, so scores look
// the same on every device (and match the rest of the UI, which is not localised yet).
// ─────────────────────────────────────────────────────────────────────────────
fun Float.oneDecimal(): String = String.format(Locale.US, "%.1f", this)
fun Float.noDecimal():  String = String.format(Locale.US, "%.0f", this)

/** "17.3", "—" when nothing has been bowled/faced yet. */
fun Float.oneDecimalOrDash(hasData: Boolean): String = if (hasData) oneDecimal() else "—"

/**
 * Required run rate, or null when it can't be computed (no balls left / nothing needed).
 */
fun requiredRate(runsNeeded: Int, ballsLeft: Int): Float? =
    if (ballsLeft > 0 && runsNeeded > 0) runsNeeded * 6f / ballsLeft else null

// ─────────────────────────────────────────────────────────────────────────────
// Scorecard text — used by the Share button (WhatsApp-friendly plain text).
// Pure Kotlin so it can be unit-tested.
// ─────────────────────────────────────────────────────────────────────────────

/** "not out", "bowled b Ali", "run out" … */
fun Player.dismissalText(bowlers: List<Player>): String {
    if (!isOut) return "not out"
    val kind = dismissal ?: return "out"
    val bowler = bowlers.getOrNull(dismissedByBowler)?.name
    return if (kind.creditsBowler && bowler != null) "${kind.label.lowercase()} b $bowler"
    else kind.label.lowercase()
}

/** Everything the Player-of-the-Match card needs, e.g. "34 (18) • 2/12". */
fun potmLine(state: MatchState): Pair<String, String>? {
    val i1 = state.innings1 ?: return null
    val i2 = state.innings2 ?: return null
    if (state.potmPlayerId < 0) return null

    val batInnings  = listOf(i1, i2).firstOrNull { it.battingTeam == state.potmTeam } ?: return null
    val bowlInnings = listOf(i1, i2).firstOrNull { it.bowlingTeam == state.potmTeam } ?: return null
    val bat  = batInnings.batters.getOrNull(state.potmPlayerId)  ?: return null
    val bowl = bowlInnings.bowlers.getOrNull(state.potmPlayerId)

    val parts = buildList {
        if (batInnings.hasBatted(bat.id)) add("${bat.runsScored} (${bat.ballsFaced})")
        if (bowl != null && bowl.ballsBowled > 0) add("${bowl.wicketsTaken}/${bowl.runsConceded}")
    }
    return bat.name to parts.joinToString(" • ")
}

fun shareScorecard(state: MatchState): String {
    val i1 = state.innings1
    val i2 = state.innings2
    return buildString {
        appendLine("🏏 ${state.teamA} vs ${state.teamB}")
        if (state.result.isNotBlank()) appendLine(state.result)
        appendLine()
        i1?.let { appendInnings(it) }
        i2?.let { appendLine(); appendInnings(it) }
        potmLine(state)?.let { (name, stats) ->
            appendLine(); appendLine("⭐ Player of the Match: $name${if (stats.isNotEmpty()) " ($stats)" else ""}")
        }
        appendLine()
        append("Scored with Score247")
    }
}

private fun StringBuilder.appendInnings(innings: Innings) {
    appendLine("${innings.battingTeam}: ${innings.runs}/${innings.wickets} (${innings.oversDisplay} ov)")
    innings.batters.forEachIndexed { i, p ->
        if (innings.hasBatted(i)) {
            val boundaries = buildList {
                if (p.fours > 0) add("${p.fours}x4")
                if (p.sixes > 0) add("${p.sixes}x6")
            }.joinToString(" ")
            appendLine("• ${p.name} ${p.runsScored}${if (p.isOut) "" else "*"} (${p.ballsFaced})" +
                    if (boundaries.isNotEmpty()) "  $boundaries" else "")
        }
    }
    val bowled = innings.bowlers.filter { it.ballsBowled > 0 }
    if (bowled.isNotEmpty()) {
        appendLine("Bowling (${innings.bowlingTeam}):")
        bowled.forEach { appendLine("• ${it.name} ${it.oversBowled}-${it.runsConceded}-${it.wicketsTaken}") }
    }
}
