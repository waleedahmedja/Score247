package com.waleedahmedja.score247.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waleedahmedja.score247.data.datastore.MatchDataStore
import com.waleedahmedja.score247.data.model.BallEvent
import com.waleedahmedja.score247.data.model.DismissalKind
import com.waleedahmedja.score247.data.model.Limits
import com.waleedahmedja.score247.data.model.MatchPhase
import com.waleedahmedja.score247.data.model.MatchRecord
import com.waleedahmedja.score247.data.model.MatchRules
import com.waleedahmedja.score247.data.model.MatchState
import com.waleedahmedja.score247.data.model.Toss
import com.waleedahmedja.score247.data.model.TossChoice
import com.waleedahmedja.score247.engine.ScoringEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Holds the one live [MatchState] and is the only place that changes it.
 * All cricket rules live in [ScoringEngine]; this class just wires user actions to the
 * engine, enforces the match flow (setup → toss → innings 1 → innings 2) and persists.
 */
class MatchViewModel(app: Application) : AndroidViewModel(app) {

    private val store = MatchDataStore(app)

    // ── State ─────────────────────────────────────────────────────────────────
    private val _state = MutableStateFlow(MatchState())
    val state: StateFlow<MatchState> = _state.asStateFlow()

    /** False until the saved match has been read — navigation waits for this (see MainActivity). */
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /** Finished matches, newest first (kept to the last [MAX_HISTORY]). */
    private val _history = MutableStateFlow<List<MatchRecord>>(emptyList())
    val history: StateFlow<List<MatchRecord>> = _history.asStateFlow()

    /** One-shot messages for the UI (validation errors, "Last ball undone", …). */
    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    init {
        viewModelScope.launch {
            store.flow.firstOrNull()?.let { saved ->
                // A match saved by an older app version has no id yet; give it one so it can be
                // filed in (and removed from) the history correctly.
                _state.value = if (saved.id == 0L) saved.copy(id = System.currentTimeMillis()) else saved
            }
            store.historyFlow.firstOrNull()?.let { saved -> _history.value = saved }
            _ready.value = true

            // Persist every change made after the initial load. collectLatest drops a stale
            // write if a newer state arrives while it is still in flight (DataStore edits
            // are atomic, so cancelling one is safe) — rapid scoring never queues up writes.
            // Two independent collectors: the live match and the history are saved separately.
            launch { _state.drop(1).collectLatest { store.save(it) } }
            launch { _history.drop(1).collectLatest { store.saveHistory(it) } }
        }
    }

    private fun toast(message: String) { _toast.tryEmit(message) }

    // ── Setup ─────────────────────────────────────────────────────────────────

    /**
     * Validates the setup form and starts a brand-new match. Returns true on success.
     * Replaces the whole state so nothing from a previous match (innings, result,
     * Player of the Match) can leak into the new one.
     */
    fun saveSetup(teamA: String, teamB: String, rules: MatchRules): Boolean {
        val a = Limits.cleanName(teamA)
        val b = Limits.cleanName(teamB)
        if (a.isEmpty() || b.isEmpty())  { toast("Enter both team names"); return false }
        if (a.equals(b, ignoreCase = true)) { toast("Team names must differ"); return false }

        _state.value = MatchState(
            id = System.currentTimeMillis(),   // unique id, used to file the match in the history
            teamA = a, teamB = b, rules = rules, phase = MatchPhase.TOSS,
        )
        return true
    }

    // ── Toss ──────────────────────────────────────────────────────────────────
    fun performToss(winner: String, choice: TossChoice) {
        _state.value = _state.value.copy(
            toss  = Toss(winner = winner, choice = choice, done = true),
            phase = MatchPhase.SETUP_INNINGS_1,
        )
    }

    /** Which team bats in innings [n] (derived from the toss). */
    fun battingTeamFor(n: Int): String = _state.value.battingTeamFor(n)

    // ── Squads (entered once, at the start of innings 1) ──────────────────────

    /** Blank names become "Player N"; surrounding spaces are trimmed. */
    private fun cleanNames(names: List<String>) =
        names.mapIndexed { i, n -> Limits.cleanName(n).ifBlank { "Player ${i + 1}" } }

    fun startFirstInnings(battingNames: List<String>, bowlingNames: List<String>) {
        val s = _state.value
        val batting = s.battingTeamFor(1)
        val bowling = if (batting == s.teamA) s.teamB else s.teamA
        _state.value = s.copy(
            innings1 = ScoringEngine.newInnings(batting, bowling, cleanNames(battingNames), cleanNames(bowlingNames)),
            phase    = MatchPhase.INNINGS_1,
        )
    }

    /** Second innings: the same two squads swap roles — nobody has to retype names. */
    fun proceedToInnings2() {
        val s  = _state.value
        val i1 = s.innings1 ?: return
        if (s.phase != MatchPhase.INNINGS_BREAK && s.phase != MatchPhase.SETUP_INNINGS_2) return

        _state.value = s.copy(
            innings2 = ScoringEngine.newInnings(
                battingTeam = i1.bowlingTeam,
                bowlingTeam = i1.battingTeam,
                batterNames = i1.bowlers.map { it.name },
                bowlerNames = i1.batters.map { it.name },
            ),
            phase = MatchPhase.INNINGS_2,
        )
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    fun scoreBall(inningsNum: Int, event: BallEvent) {
        val s       = _state.value
        val innings = s.inningsFor(inningsNum) ?: return
        if (innings.complete || innings.awaitingNextBatter) return

        // Free hit: the batter can only be run out.
        if (event is BallEvent.Wicket && event.kind != DismissalKind.RUN_OUT &&
            ScoringEngine.isFreeHit(innings, s.rules)
        ) {
            toast("Free hit — only a run-out counts")
            return
        }

        val applied  = ScoringEngine.applyEvent(innings, event, s.rules)
        val finished = ScoringEngine.checkCompletion(applied, s.rules, s.targetFor(inningsNum))

        var next = s.withInnings(inningsNum, finished)
        if (finished.complete) {
            next = if (inningsNum == 1) {
                next.copy(phase = MatchPhase.INNINGS_BREAK)
            } else {
                val first = s.innings1 ?: return
                next.copy(phase = MatchPhase.COMPLETED, result = ScoringEngine.calcResult(first, finished, s.rules))
            }
        }
        if (next.phase == MatchPhase.COMPLETED) next = withPotm(next)
        _state.value = next
        if (next.phase == MatchPhase.COMPLETED) addToHistory(next)
    }

    /** Scorer picked who walks in after a wicket. Invalid picks are ignored by the engine. */
    fun selectNextBatter(inningsNum: Int, batterIndex: Int) {
        val s = _state.value
        val innings = s.inningsFor(inningsNum) ?: return
        _state.value = s.withInnings(inningsNum, ScoringEngine.selectNextBatter(innings, batterIndex))
    }

    fun setBowler(inningsNum: Int, index: Int) {
        val s = _state.value
        val innings = s.inningsFor(inningsNum) ?: return
        if (index !in innings.bowlers.indices) return
        _state.value = s.withInnings(inningsNum, innings.copy(bowlerIndex = index))
    }

    /**
     * Undo = replay the event log without its last event, so every statistic is
     * recomputed exactly. Works even for the ball that ended an innings or the match
     * (the phase, result and Player of the Match are rolled back too).
     */
    fun undoLastBall(inningsNum: Int) {
        val s = _state.value
        val innings = s.inningsFor(inningsNum) ?: return
        val previous = ScoringEngine.withoutLastBall(innings, s.rules)
        if (previous == null) { toast("Nothing to undo"); return }

        // If this undo re-opens a finished match, it no longer belongs in the history.
        if (s.phase == MatchPhase.COMPLETED) removeFromHistory(s.id)

        _state.value = s.withInnings(inningsNum, previous).copy(
            phase        = if (inningsNum == 1) MatchPhase.INNINGS_1 else MatchPhase.INNINGS_2,
            result       = "",
            potmPlayerId = -1,
            potmTeam     = "",
        )
        toast("Last ball undone")
    }

    // ── Player of the Match ───────────────────────────────────────────────────

    /** Returns [s] with the Player of the Match filled in (unchanged if already set). */
    private fun withPotm(s: MatchState): MatchState {
        val i1 = s.innings1 ?: return s
        val i2 = s.innings2 ?: return s
        if (s.potmPlayerId != -1) return s
        val pick = ScoringEngine.suggestPotm(i1, i2) ?: return s
        return s.copy(potmPlayerId = pick.playerId, potmTeam = pick.team)
    }

    /** Kept for the summary screen; the pick is normally made the moment the match ends. */
    fun autoSuggestPotm() {
        val s = _state.value
        val updated = withPotm(s)
        if (updated != s) {
            _state.value = updated
            addToHistory(updated)   // refresh the saved copy so it includes the pick
        }
    }

    // ── History ───────────────────────────────────────────────────────────────

    /** Saves (or refreshes) a finished match at the top of the history. */
    private fun addToHistory(finished: MatchState) {
        // Matches started before the history existed have id 0 — give them a real one.
        val id = if (finished.id != 0L) finished.id else System.currentTimeMillis()
        val old = _history.value.firstOrNull { it.id == id }
        val record = MatchRecord(
            id = id,
            completedAt = old?.completedAt ?: System.currentTimeMillis(),
            state = finished.copy(id = id),
        )
        _history.value = (listOf(record) + _history.value.filterNot { it.id == id })
            .sortedByDescending { it.completedAt }
            .take(MAX_HISTORY)
        if (finished.id != id) _state.value = _state.value.copy(id = id)
    }

    private fun removeFromHistory(id: Long) {
        _history.value = _history.value.filterNot { it.id == id }
    }

    /** Called from the match-detail screen's delete button. */
    fun deleteMatch(id: Long) = removeFromHistory(id)

    // ── New match ─────────────────────────────────────────────────────────────

    /**
     * Resets synchronously. (The old version cleared storage first and reset the state
     * afterwards, so the next screen could briefly see the old match and jump back to it.)
     * The new default state is persisted by the collector in [init].
     */
    fun newMatch() {
        _state.value = MatchState()
    }

    private companion object {
        /** Older matches are dropped so the saved JSON stays small. */
        const val MAX_HISTORY = 50
    }
}
