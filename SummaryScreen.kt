package com.waleedahmedja.score247.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waleedahmedja.score247.data.model.Innings
import com.waleedahmedja.score247.data.model.MatchState
import com.waleedahmedja.score247.ui.components.S247ButtonSecondary
import com.waleedahmedja.score247.ui.components.ShareScorecardActions
import com.waleedahmedja.score247.ui.components.S247Card
import com.waleedahmedja.score247.ui.components.S247Divider
import com.waleedahmedja.score247.ui.components.S247Screen
import com.waleedahmedja.score247.ui.components.SectionLabel
import com.waleedahmedja.score247.ui.theme.Amber
import com.waleedahmedja.score247.ui.theme.AmberTint
import com.waleedahmedja.score247.ui.theme.HeaderBg
import com.waleedahmedja.score247.ui.theme.HeaderMuted
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink300
import com.waleedahmedja.score247.ui.theme.Ink500
import com.waleedahmedja.score247.ui.theme.ShapeCard
import com.waleedahmedja.score247.util.dismissalText
import com.waleedahmedja.score247.util.noDecimal
import com.waleedahmedja.score247.util.oneDecimal
import com.waleedahmedja.score247.util.potmLine
import com.waleedahmedja.score247.viewmodel.MatchViewModel

/** Final result, Player of the Match, full scorecards, and a share button. */
@Composable
fun SummaryScreen(vm: MatchViewModel, onNewMatch: () -> Unit, onUndo: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    // The picture is dated with the moment the summary is opened.
    val dateMillis = remember { System.currentTimeMillis() }
    if (state.innings1 == null || state.innings2 == null) return

    // Pick the Player of the Match once (the ViewModel ignores repeat calls).
    LaunchedEffect(Unit) { vm.autoSuggestPotm() }
    S247Screen("Scorecard") {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            ScorecardBody(state)

            Spacer(Modifier.height(4.dp))
            ShareScorecardActions(state, dateMillis)
            S247ButtonSecondary(text = "Start New Match", onClick = { vm.newMatch(); onNewMatch() })
            TextButton(onClick = { vm.undoLastBall(2); onUndo() }, modifier = Modifier.fillMaxWidth()) {
                Text("Scoring mistake? Undo last ball", color = Ink300, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * The read-only scorecard: result banner, both innings totals, Player of the Match and the
 * four tables. Shared by the live Summary screen and the saved-match detail screen.
 * Must be called inside a Column (it emits several cards one after another).
 */
@Composable
fun ColumnScope.ScorecardBody(state: MatchState) {
    val i1 = state.innings1 ?: return
    val i2 = state.innings2 ?: return
    val potm = potmLine(state)

    // Result banner — dark "scoreboard" panel
    Box(
        Modifier.fillMaxWidth().clip(ShapeCard).background(HeaderBg).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("RESULT", style = MaterialTheme.typography.labelSmall, color = HeaderMuted)
            Spacer(Modifier.height(6.dp))
            Text(state.result, style = MaterialTheme.typography.headlineLarge, color = Color.White, textAlign = TextAlign.Center)
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InningsTile(i1, Modifier.weight(1f))
        InningsTile(i2, Modifier.weight(1f))
    }

    if (potm != null) {
        Surface(shape = ShapeCard, color = AmberTint, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                SectionLabel("Player of the Match")
                Spacer(Modifier.height(4.dp))
                Text(potm.first, style = MaterialTheme.typography.headlineMedium, color = Ink)
                if (potm.second.isNotEmpty()) {
                    Text(potm.second, style = MaterialTheme.typography.bodyMedium, color = Amber, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    BattingCard(i1)
    BowlingCard(i1)
    BattingCard(i2)
    BowlingCard(i2)
}

@Composable
private fun InningsTile(innings: Innings, modifier: Modifier) {
    S247Card(modifier = modifier) {
        SectionLabel(innings.battingTeam)
        Text("${innings.runs}/${innings.wickets}", style = MaterialTheme.typography.displaySmall, color = Ink)
        Text("${innings.oversDisplay} ov", style = MaterialTheme.typography.bodySmall, color = Ink500)
    }
}

// ── Scorecard tables ──────────────────────────────────────────────────────────

private val WideCol:   Dp = 34.dp
private val SmallCol:  Dp = 28.dp
private val RateCol:   Dp = 46.dp

@Composable
private fun Cell(text: String, width: Dp, bold: Boolean = false, color: Color = Ink500) {
    Text(
        text, modifier = Modifier.width(width), textAlign = TextAlign.End,
        style = MaterialTheme.typography.labelMedium, color = color,
        fontWeight = if (bold) FontWeight.Bold else null,
    )
}

@Composable
private fun BattingCard(innings: Innings) {
    S247Card {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${innings.battingTeam} batting", style = MaterialTheme.typography.titleLarge, color = Ink)
            Text("${innings.runs}/${innings.wickets}", style = MaterialTheme.typography.titleLarge, color = Ink)
        }
        S247Divider(Modifier.padding(vertical = 8.dp))

        Row(Modifier.fillMaxWidth()) {
            Text("Batter", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Ink300)
            Cell("R", WideCol, color = Ink300); Cell("B", WideCol, color = Ink300)
            Cell("4s", SmallCol, color = Ink300); Cell("6s", SmallCol, color = Ink300)
            Cell("SR", RateCol, color = Ink300)
        }

        val didNotBat = mutableListOf<String>()
        innings.batters.forEachIndexed { i, p ->
            if (!innings.hasBatted(i)) { didNotBat += p.name; return@forEachIndexed }
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium, color = Ink,
                        fontWeight = if (p.isOut) FontWeight.Medium else FontWeight.Bold, maxLines = 1)
                    Text(p.dismissalText(innings.bowlers), style = MaterialTheme.typography.bodySmall, color = Ink300, maxLines = 1)
                }
                Cell("${p.runsScored}", WideCol, bold = true, color = Ink)
                Cell("${p.ballsFaced}", WideCol)
                Cell("${p.fours}", SmallCol); Cell("${p.sixes}", SmallCol)
                Cell(if (p.ballsFaced == 0) "—" else p.strikeRate.noDecimal(), RateCol)
            }
        }

        S247Divider(Modifier.padding(vertical = 8.dp))
        Text("Extras: ${innings.extras}", style = MaterialTheme.typography.bodySmall, color = Ink500)
        if (didNotBat.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Did not bat: ${didNotBat.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = Ink300)
        }
    }
}

@Composable
private fun BowlingCard(innings: Innings) {
    S247Card {
        Text("${innings.bowlingTeam} bowling", style = MaterialTheme.typography.titleLarge, color = Ink)
        S247Divider(Modifier.padding(vertical = 8.dp))

        val bowled = innings.bowlers.filter { it.ballsBowled > 0 || it.widesBowled > 0 || it.noBallsBowled > 0 }
        if (bowled.isEmpty()) {
            Text("No bowling data", style = MaterialTheme.typography.bodySmall, color = Ink300)
            return@S247Card
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Bowler", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Ink300)
            Cell("O", WideCol, color = Ink300); Cell("R", WideCol, color = Ink300)
            Cell("W", SmallCol, color = Ink300); Cell("Econ", RateCol, color = Ink300)
        }
        bowled.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, Modifier.weight(1f).padding(end = 8.dp), style = MaterialTheme.typography.titleMedium, color = Ink, maxLines = 1)
                Cell(p.oversBowled, WideCol)
                Cell("${p.runsConceded}", WideCol)
                Cell("${p.wicketsTaken}", SmallCol, bold = true, color = Ink)
                Cell(if (p.ballsBowled == 0) "—" else p.economy.oneDecimal(), RateCol)
            }
        }
    }
}
