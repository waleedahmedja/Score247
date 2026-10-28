package com.waleedahmedja.score247.engine

import com.waleedahmedja.score247.data.model.Innings
import com.waleedahmedja.score247.data.model.MatchRecord

/**
 * One player's totals across every saved match.
 * (Plain numbers only — the averages below are worked out on demand from them.)
 */
data class PlayerCareer(
    val name: String,
    val matches: Int,

    // Batting
    val battingInnings: Int,        // innings in which the player actually batted
    val notOuts: Int,
    val runs: Int,
    val ballsFaced: Int,
    val fours: Int,
    val sixes: Int,
    val highScore: Int,
    val highScoreNotOut: Boolean,

    // Bowling
    val wickets: Int,
    val ballsBowled: Int,
    val runsConceded: Int,
    val bestWickets: Int,           // best single-innings figures: wickets…
    val bestRuns: Int,              // …for this many runs
) {
    /** Runs per dismissal. Null until the player has been out at least once. */
    val battingAverage: Float?
        get() = (battingInnings - notOuts).let { outs -> if (outs > 0) runs.toFloat() / outs else null }

    val strikeRate: Float get() = if (ballsFaced == 0) 0f else runs * 100f / ballsFaced

    val economy: Float get() = if (ballsBowled == 0) 0f else runsConceded * 6f / ballsBowled

    val oversBowled: String get() = "${ballsBowled / 6}.${ballsBowled % 6}"

    /** "3/12" — or null if the player never bowled. */
    val bestBowling: String? get() = if (ballsBowled == 0) null else "$bestWickets/$bestRuns"
}

/**
 * Turns the list of finished matches into per-player career numbers.
 * Pure Kotlin (no Android), so it is unit-tested like the scoring engine.
 *
 * Players are matched BY NAME (ignoring capitals and extra spaces), because the app has
 * no player accounts. Placeholder names such as "Player 3" are skipped — they would
 * otherwise merge completely different people into one fake player.
 */
object CareerStats {

    private val placeholderName = Regex("""Player \d+""", RegexOption.IGNORE_CASE)

    fun isRealName(name: String): Boolean = name.isNotBlank() && !placeholderName.matches(name.trim())

    /** Same person if the names match ignoring case and surrounding spaces. */
    private fun keyOf(name: String) = name.trim().lowercase()

    /** Mutable scratch-pad used only while adding things up. */
    private class Tally(var name: String) {
        var matches = 0
        var battingInnings = 0; var notOuts = 0; var runs = 0; var balls = 0
        var fours = 0; var sixes = 0; var highScore = -1; var highNotOut = false
        var wickets = 0; var ballsBowled = 0; var conceded = 0
        var bestWickets = -1; var bestRuns = 0

        fun toCareer() = PlayerCareer(
            name = name, matches = matches,
            battingInnings = battingInnings, notOuts = notOuts, runs = runs, ballsFaced = balls,
            fours = fours, sixes = sixes,
            highScore = maxOf(highScore, 0), highScoreNotOut = highNotOut,
            wickets = wickets, ballsBowled = ballsBowled, runsConceded = conceded,
            bestWickets = maxOf(bestWickets, 0), bestRuns = bestRuns,
        )
    }

    /**
     * @param records finished matches, newest first (so a player's most recent spelling
     *                of their name is the one shown).
     * @return every real-named player who appears in at least one match, unsorted.
     */
    fun compute(records: List<MatchRecord>): List<PlayerCareer> {
        val tallies = LinkedHashMap<String, Tally>()
        fun tally(name: String) = tallies.getOrPut(keyOf(name)) { Tally(name.trim()) }

        for (record in records) {
            val innings = listOfNotNull(record.state.innings1, record.state.innings2)

            // 1) "Matches played": everyone named in either squad, counted once per match.
            innings.flatMap { it.batters + it.bowlers }
                .map { it.name }.filter(::isRealName)
                .distinctBy(::keyOf)
                .forEach { tally(it).matches++ }

            // 2) Batting and bowling lines from each innings.
            for (inn in innings) {
                addBatting(inn, ::tally)
                addBowling(inn, ::tally)
            }
        }
        return tallies.values.map { it.toCareer() }
    }

    private fun addBatting(inn: Innings, tally: (String) -> Tally) {
        inn.batters.forEachIndexed { i, p ->
            if (!isRealName(p.name) || !inn.hasBatted(i)) return@forEachIndexed
            val t = tally(p.name)
            t.battingInnings++
            if (!p.isOut) t.notOuts++
            t.runs += p.runsScored; t.balls += p.ballsFaced
            t.fours += p.fours; t.sixes += p.sixes
            // Highest score; on a tie a "not out" score is the better one.
            if (p.runsScored > t.highScore || (p.runsScored == t.highScore && !p.isOut)) {
                t.highScore = p.runsScored
                t.highNotOut = !p.isOut
            }
        }
    }

    private fun addBowling(inn: Innings, tally: (String) -> Tally) {
        for (p in inn.bowlers) {
            if (!isRealName(p.name) || (p.ballsBowled == 0 && p.runsConceded == 0)) continue
            val t = tally(p.name)
            t.wickets += p.wicketsTaken
            t.ballsBowled += p.ballsBowled
            t.conceded += p.runsConceded
            // Best bowling = most wickets; on a tie, fewest runs.
            if (p.wicketsTaken > t.bestWickets || (p.wicketsTaken == t.bestWickets && p.runsConceded < t.bestRuns)) {
                t.bestWickets = p.wicketsTaken
                t.bestRuns = p.runsConceded
            }
        }
    }

    // ── Ready-made sort orders for the leaderboard ────────────────────────────

    /** Most runs first; strike rate breaks ties. */
    fun battingLeaders(all: List<PlayerCareer>): List<PlayerCareer> =
        all.filter { it.battingInnings > 0 }
            .sortedWith(compareByDescending<PlayerCareer> { it.runs }.thenByDescending { it.strikeRate })

    /** Most wickets first; lower economy breaks ties. */
    fun bowlingLeaders(all: List<PlayerCareer>): List<PlayerCareer> =
        all.filter { it.ballsBowled > 0 }
            .sortedWith(compareByDescending<PlayerCareer> { it.wickets }.thenBy { it.economy })
}
