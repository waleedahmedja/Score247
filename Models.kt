package com.waleedahmedja.score247.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// NAVIGATION ROUTES — single source of truth
// ─────────────────────────────────────────────────────────────────────────────
object Routes {
    const val HOME          = "home"
    const val SETUP         = "setup"
    const val TOSS          = "toss"
    const val PLAYERS       = "players"
    const val SCORING       = "scoring/{innings}"
    const val INNINGS_BREAK = "innings_break"
    const val SUMMARY       = "summary"
    const val HISTORY       = "history"
    const val MATCH_DETAIL  = "match/{id}"
    const val ABOUT         = "about"
    const val DOC           = "doc/{id}"

    fun scoring(innings: Int) = "scoring/$innings"
    fun matchDetail(id: Long) = "match/$id"
    fun doc(id: String) = "doc/$id"
}

// ─────────────────────────────────────────────────────────────────────────────
// LIMITS: sensible upper bounds on text the user types. They keep screens tidy and stop an
// absurdly long (or odd) name from bloating the saved data or the shared picture.
// ─────────────────────────────────────────────────────────────────────────────
object Limits {
    const val NAME_LENGTH = 30

    /** Removes control characters (tabs, line breaks...) and cuts the name to [NAME_LENGTH]. */
    fun cleanName(raw: String): String = raw.filter { !it.isISOControl() }.trim().take(NAME_LENGTH)
}

// ─────────────────────────────────────────────────────────────────────────────
// MATCH RULES — every toggle maps to a common gully-cricket house rule
// ─────────────────────────────────────────────────────────────────────────────
@Serializable
data class MatchRules(
    val totalOvers:       Int     = 5,
    val playersPerTeam:   Int     = 6,
    /** A wide costs the bowling side one extra run (on top of any runs run). */
    val wideGivesRun:     Boolean = true,
    /** A wide uses up one ball of the over (street rule — off in the real game). */
    val wideIsLegalBall:  Boolean = false,
    /** A no-ball costs the bowling side one extra run. */
    val noBallGivesRun:   Boolean = true,
    /** The last remaining batter keeps batting alone instead of the innings ending. */
    val lastBatsmanAlone: Boolean = true,
    /** The ball after a no-ball is a free hit: only a run-out can dismiss the batter. */
    val freeHitOnNoBall:  Boolean = true,
) {
    /** Wickets the batting side can lose before being all out. */
    val maxWickets: Int get() = if (lastBatsmanAlone) playersPerTeam else playersPerTeam - 1
}

// ─────────────────────────────────────────────────────────────────────────────
// DISMISSAL — how a batter got out
// ─────────────────────────────────────────────────────────────────────────────
@Serializable
enum class DismissalKind(val label: String, val creditsBowler: Boolean) {
    BOWLED("Bowled", true),
    CAUGHT("Caught", true),
    LBW("LBW", true),
    STUMPED("Stumped", true),
    HIT_WICKET("Hit wicket", true),
    /** The only dismissal that does NOT count as a bowler's wicket. */
    RUN_OUT("Run out", false),
}

// ─────────────────────────────────────────────────────────────────────────────
// PLAYER — one struct for batting and bowling stats.
// Immutable: the engine produces new copies, so saved state is never mutated.
// `id` is always the player's index in its team's list (the engine relies on it).
// ─────────────────────────────────────────────────────────────────────────────
@Serializable
data class Player(
    val id:   Int,
    val name: String,

    // Batting
    val runsScored: Int     = 0,
    val ballsFaced: Int     = 0,
    val fours:      Int     = 0,
    val sixes:      Int     = 0,
    val isOut:      Boolean = false,
    val dismissal:  DismissalKind? = null,
    /** Index of the bowler credited with the wicket, or -1 (run-out / not out). */
    val dismissedByBowler: Int = -1,

    // Bowling
    val ballsBowled:   Int = 0,
    val runsConceded:  Int = 0,
    val wicketsTaken:  Int = 0,
    val widesBowled:   Int = 0,
    val noBallsBowled: Int = 0,
) {
    val strikeRate: Float
        get() = if (ballsFaced == 0) 0f else runsScored * 100f / ballsFaced

    val economy: Float
        get() = if (ballsBowled == 0) 0f else runsConceded * 6f / ballsBowled

    /** Overs in cricket notation, e.g. 13 balls -> "2.1". */
    val oversBowled: String
        get() = "${ballsBowled / 6}.${ballsBowled % 6}"
}

// ─────────────────────────────────────────────────────────────────────────────
// BALL EVENT — the innings is an event log; every number on screen is derived by
// replaying it (see ScoringEngine.rebuild). That is what makes undo exact.
// Every subclass has a @SerialName so renaming a class never breaks saved data.
// ─────────────────────────────────────────────────────────────────────────────
@Serializable
sealed class BallEvent {

    /** Legal delivery; [runs] all came off the bat. [batter]/[bowler] are player indices. */
    @Serializable @SerialName("runs")
    data class Runs(val runs: Int, val batter: Int, val bowler: Int) : BallEvent()

    /** [extras] = additional runs run on top of the wide's own penalty run. */
    @Serializable @SerialName("wide")
    data class Wide(val extras: Int, val bowler: Int) : BallEvent()

    /** [extras] = runs the batter scored off the no-ball (the penalty run is separate). */
    @Serializable @SerialName("noball")
    data class NoBall(val extras: Int, val batter: Int, val bowler: Int) : BallEvent()

    /**
     * [batter] is the index of the player who is OUT (striker or, for a run-out,
     * possibly the non-striker). [runsOnBall] are runs completed before the dismissal.
     * [nextBatter] is filled in once the scorer picks the replacement (-1 = not yet);
     * recording it here is what lets undo replay a wicket exactly.
     */
    @Serializable @SerialName("wicket")
    data class Wicket(
        val batter: Int,
        val bowler: Int,
        val runsOnBall: Int = 0,
        val kind: DismissalKind = DismissalKind.BOWLED,
        val nextBatter: Int = -1,
    ) : BallEvent()

    @Serializable @SerialName("bye")
    data class Bye(val runs: Int, val bowler: Int) : BallEvent()

    @Serializable @SerialName("legbye")
    data class LegBye(val runs: Int, val bowler: Int) : BallEvent()
}

// ─────────────────────────────────────────────────────────────────────────────
// INNINGS — immutable snapshot; the engine returns a new one for every event
// ─────────────────────────────────────────────────────────────────────────────
@Serializable
data class Innings(
    val battingTeam: String,
    val bowlingTeam: String,

    // Separate lists — batting and bowling side are different people
    val batters: List<Player> = emptyList(),
    val bowlers: List<Player> = emptyList(),

    val runs:    Int = 0,
    val wickets: Int = 0,
    /** Legal balls bowled so far (an over = 6). */
    val balls:   Int = 0,
    val extras:  Int = 0,

    /** Indices into [batters]. Equal when the last batter is batting alone. */
    val strikerIndex:    Int = 0,
    val nonStrikerIndex: Int = 0,
    val bowlerIndex:     Int = 0,

    /** True after a wicket while the scorer still has to pick the incoming batter. */
    val awaitingNextBatter: Boolean = false,

    val events:         List<BallEvent> = emptyList(),
    val complete:       Boolean = false,
    val completeReason: String  = "",
) {
    val currentOver:  Int    get() = balls / 6
    val currentBall:  Int    get() = balls % 6
    val oversDisplay: String get() = "$currentOver.$currentBall"

    val runRate: Float
        get() = if (balls == 0) 0f else runs * 6f / balls

    /** True once the player has been at the crease (faced a ball, got out, or is there now). */
    fun hasBatted(index: Int): Boolean {
        val p = batters.getOrNull(index) ?: return false
        return p.isOut || p.ballsFaced > 0 || index == strikerIndex || index == nonStrikerIndex
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOSS
// ─────────────────────────────────────────────────────────────────────────────
@Serializable
enum class TossChoice { BAT, BOWL }

@Serializable
data class Toss(
    val winner: String     = "",
    val choice: TossChoice = TossChoice.BAT,
    val done:   Boolean    = false,
)

// ─────────────────────────────────────────────────────────────────────────────
// MATCH PHASE — drives navigation and process-death resume
// ─────────────────────────────────────────────────────────────────────────────
@Serializable
enum class MatchPhase {
    SETUP,
    TOSS,
    SETUP_INNINGS_1,
    INNINGS_1,
    INNINGS_BREAK,
    /** Legacy: the second-innings name screen no longer exists (squads carry over). */
    SETUP_INNINGS_2,
    INNINGS_2,
    COMPLETED,
}

// ─────────────────────────────────────────────────────────────────────────────
// MATCH STATE — the single object persisted to DataStore
// ─────────────────────────────────────────────────────────────────────────────
@Serializable
data class MatchState(
    /** Unique id of this match (its start time in ms). Used to save it to the history once. */
    val id: Long = 0L,
    val teamA: String     = "Team A",
    val teamB: String     = "Team B",
    val rules: MatchRules = MatchRules(),
    val toss:  Toss       = Toss(),

    val innings1: Innings? = null,
    val innings2: Innings? = null,

    val phase:  MatchPhase = MatchPhase.SETUP,
    val result: String     = "",

    val potmPlayerId: Int    = -1,
    val potmTeam:     String = "",
) {
    fun inningsFor(n: Int): Innings? = if (n == 1) innings1 else innings2

    fun withInnings(n: Int, innings: Innings): MatchState =
        if (n == 1) copy(innings1 = innings) else copy(innings2 = innings)

    /** Runs the chasing side needs, or null in the first innings. */
    fun targetFor(n: Int): Int? = if (n == 2) innings1?.let { it.runs + 1 } else null

    /** Which team bats in innings [n], derived from the toss (and innings 1 once it exists). */
    fun battingTeamFor(n: Int): String {
        val winner = toss.winner
        val loser  = if (winner == teamA) teamB else teamA
        return if (n == 1) {
            if (toss.choice == TossChoice.BAT) winner else loser
        } else {
            val firstBat = innings1?.battingTeam ?: if (toss.choice == TossChoice.BAT) winner else loser
            if (firstBat == teamA) teamB else teamA
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MATCH RECORD — a finished match kept in the history list.
// It stores the complete final MatchState, so the full scorecard can be shown again
// (and career stats calculated) long after the match was played.
// ─────────────────────────────────────────────────────────────────────────────
@Serializable
data class MatchRecord(
    val id: Long,
    /** When the last ball was scored (ms since 1970) — shown as the date in the history list. */
    val completedAt: Long,
    val state: MatchState,
)
