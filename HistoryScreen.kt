package com.waleedahmedja.score247.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waleedahmedja.score247.data.model.MatchRecord
import com.waleedahmedja.score247.engine.CareerStats
import com.waleedahmedja.score247.engine.PlayerCareer
import com.waleedahmedja.score247.ui.components.EmptyState
import com.waleedahmedja.score247.ui.components.OptionChip
import com.waleedahmedja.score247.ui.components.S247Card
import com.waleedahmedja.score247.ui.components.S247Screen
import com.waleedahmedja.score247.ui.theme.Amber
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink300
import com.waleedahmedja.score247.ui.theme.Ink500
import com.waleedahmedja.score247.ui.theme.Pitch
import com.waleedahmedja.score247.util.formatMatchDate
import com.waleedahmedja.score247.util.noDecimal
import com.waleedahmedja.score247.util.oneDecimal
import com.waleedahmedja.score247.viewmodel.MatchViewModel

/** Which of the two lists is showing. */
private enum class HistoryTab(val label: String) { MATCHES("Matches"), PLAYERS("Players") }

/** Which leaderboard the Players tab shows. */
private enum class Board(val label: String) { BATTING("Batting"), BOWLING("Bowling") }

/**
 * Everything that has been played: the list of finished matches, and a leaderboard
 * of every named player's career numbers.
 */
@Composable
fun HistoryScreen(vm: MatchViewModel, onBack: () -> Unit, onOpenMatch: (Long) -> Unit) {
    val history by vm.history.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(HistoryTab.MATCHES) }

    S247Screen("History & stats", onBack = onBack) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistoryTab.entries.forEach { t ->
                    OptionChip(t.label, selected = tab == t, onClick = { tab = t }, modifier = Modifier.weight(1f))
                }
            }
            when (tab) {
                HistoryTab.MATCHES -> MatchesList(history, onOpenMatch)
                HistoryTab.PLAYERS -> PlayersBoard(history)
            }
        }
    }
}

// ── Matches tab ───────────────────────────────────────────────────────────────

@Composable
private fun MatchesList(history: List<MatchRecord>, onOpen: (Long) -> Unit) {
    if (history.isEmpty()) {
        EmptyState("🏏", "No matches yet", "Finished matches are saved here automatically.")
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(history, key = { it.id }) { record -> MatchRow(record, onClick = { onOpen(record.id) }) }
    }
}

@Composable
private fun MatchRow(record: MatchRecord, onClick: () -> Unit) {
    val s = record.state
    val i1 = s.innings1
    val i2 = s.innings2
    S247Card(Modifier.clickable(onClick = onClick)) {
        Text(formatMatchDate(record.completedAt), style = MaterialTheme.typography.labelSmall, color = Ink300)
        Spacer(Modifier.height(6.dp))
        Text("${s.teamA} vs ${s.teamB}", style = MaterialTheme.typography.titleLarge, color = Ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (i1 != null && i2 != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "${i1.battingTeam} ${i1.runs}/${i1.wickets}  ·  ${i2.battingTeam} ${i2.runs}/${i2.wickets}",
                style = MaterialTheme.typography.bodyMedium, color = Ink500,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(s.result, style = MaterialTheme.typography.bodyMedium, color = Pitch, fontWeight = FontWeight.SemiBold)
    }
}

// ── Players tab ───────────────────────────────────────────────────────────────

@Composable
private fun PlayersBoard(history: List<MatchRecord>) {
    var board by rememberSaveable { mutableStateOf(Board.BATTING) }

    // Re-worked only when the history changes, not on every redraw.
    val careers = remember(history) { CareerStats.compute(history) }
    val rows = remember(careers, board) {
        if (board == Board.BATTING) CareerStats.battingLeaders(careers) else CareerStats.bowlingLeaders(careers)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Board.entries.forEach { b ->
                OptionChip(b.label, selected = board == b, onClick = { board = b }, modifier = Modifier.weight(1f))
            }
        }
        if (rows.isEmpty()) {
            EmptyState(
                "📊", "No stats yet",
                "Stats appear once matches are finished. Players with the default names (\"Player 1\"…) are skipped, so enter real names.",
            )
            return
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(rows.withIndex().toList(), key = { it.value.name.lowercase() }) { (i, p) ->
                PlayerRow(rank = i + 1, p = p, board = board)
            }
        }
    }
}

@Composable
private fun PlayerRow(rank: Int, p: PlayerCareer, board: Board) {
    S247Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$rank", style = MaterialTheme.typography.headlineMedium, color = if (rank == 1) Amber else Ink300,
                modifier = Modifier.width(32.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleLarge, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${p.matches} ${if (p.matches == 1) "match" else "matches"}", style = MaterialTheme.typography.bodySmall, color = Ink300)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (board == Board.BATTING) {
                    Text("${p.runs} runs", style = MaterialTheme.typography.titleLarge, color = Ink)
                    val hs = "HS ${p.highScore}${if (p.highScoreNotOut) "*" else ""}"
                    val avg = p.battingAverage?.oneDecimal() ?: "—"
                    Text("$hs · Avg $avg · SR ${p.strikeRate.noDecimal()}", style = MaterialTheme.typography.bodySmall, color = Ink500)
                } else {
                    Text("${p.wickets} wkts", style = MaterialTheme.typography.titleLarge, color = Ink)
                    Text("Best ${p.bestBowling ?: "—"} · Econ ${p.economy.oneDecimal()} · ${p.oversBowled} ov",
                        style = MaterialTheme.typography.bodySmall, color = Ink500)
                }
            }
        }
    }
}
