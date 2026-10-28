package com.waleedahmedja.score247.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waleedahmedja.score247.ui.components.S247Button
import com.waleedahmedja.score247.ui.components.S247Card
import com.waleedahmedja.score247.ui.components.S247Screen
import com.waleedahmedja.score247.ui.components.SectionLabel
import com.waleedahmedja.score247.ui.theme.Amber
import com.waleedahmedja.score247.ui.theme.AmberTint
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink300
import com.waleedahmedja.score247.ui.theme.Ink500
import com.waleedahmedja.score247.ui.theme.ShapeCard
import com.waleedahmedja.score247.viewmodel.MatchViewModel

/** Shown between the innings: the first score, the target, and the way back if a ball was mis-scored. */
@Composable
fun InningsBreakScreen(vm: MatchViewModel, onContinue: () -> Unit, onUndo: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val i1 = state.innings1 ?: return
    val target  = i1.runs + 1
    val chasing = state.battingTeamFor(2)
    var proceeded by remember { mutableStateOf(false) }   // guards against a double tap

    S247Screen("Innings Break") {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("End of innings 1", style = MaterialTheme.typography.headlineLarge, color = Ink)
            Text(i1.completeReason, style = MaterialTheme.typography.bodySmall, color = Ink300)
            Spacer(Modifier.height(24.dp))

            S247Card {
                SectionLabel(i1.battingTeam)
                Text("${i1.runs}/${i1.wickets}", style = MaterialTheme.typography.displayMedium, color = Ink)
                Text("${i1.oversDisplay} overs  •  extras ${i1.extras}", style = MaterialTheme.typography.bodySmall, color = Ink500)
            }
            Spacer(Modifier.height(16.dp))

            Surface(shape = ShapeCard, color = AmberTint, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SectionLabel("Target")
                    Text("$target", style = MaterialTheme.typography.displayMedium, color = Amber)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$chasing need $target in ${state.rules.totalOvers} overs",
                        style = MaterialTheme.typography.bodyMedium, color = Ink500, textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            S247Button(
                text    = "Start $chasing's chase",
                enabled = !proceeded,
                onClick = { if (!proceeded) { proceeded = true; vm.proceedToInnings2(); onContinue() } },
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.undoLastBall(1); onUndo() }, enabled = !proceeded) {
                Text("Scoring mistake? Undo last ball", color = Ink300, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
