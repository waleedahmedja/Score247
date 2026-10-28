package com.waleedahmedja.score247.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waleedahmedja.score247.data.model.Limits
import com.waleedahmedja.score247.data.model.MatchRecord
import com.waleedahmedja.score247.engine.CareerStats
import com.waleedahmedja.score247.ui.components.S247Button
import com.waleedahmedja.score247.ui.components.S247Card
import com.waleedahmedja.score247.ui.components.S247Screen
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink200
import com.waleedahmedja.score247.ui.theme.Ink300
import com.waleedahmedja.score247.ui.theme.Ink50
import com.waleedahmedja.score247.ui.theme.Pitch
import com.waleedahmedja.score247.ui.theme.ShapeInput
import com.waleedahmedja.score247.viewmodel.MatchViewModel

/**
 * Both squads are entered ONCE, before the first innings. In the second innings the same
 * two squads simply swap roles (see MatchViewModel.proceedToInnings2), so there is nothing
 * to retype and every player's stats line up across both innings.
 */
@Composable
fun PlayersScreen(vm: MatchViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val count = state.rules.playersPerTeam
    val battingTeam = state.battingTeamFor(1)
    val bowlingTeam = if (battingTeam == state.teamA) state.teamB else state.teamA

    // Friends usually play again as the same teams, so reuse the names from their last match.
    val history by vm.history.collectAsStateWithLifecycle()
    val savedBat = remember(battingTeam, count) { rosterFor(battingTeam, history, count) }
    val savedBow = remember(bowlingTeam, count) { rosterFor(bowlingTeam, history, count) }

    // rememberSaveable + nameListSaver: typed names survive rotation and the app being killed.
    val batNames = rememberSaveable(count, saver = nameListSaver) { savedBat.toMutableStateList() }
    val bowNames = rememberSaveable(count, saver = nameListSaver) { savedBow.toMutableStateList() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val prefilled = savedBat.any { it.isNotBlank() } || savedBow.any { it.isNotBlank() }

    S247Screen("Squads", onBack = onBack) {
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = tab, containerColor = Ink50, contentColor = Pitch) {
                Tab(selected = tab == 0, onClick = { tab = 0 },
                    text = { Text("$battingTeam (${batNames.count { it.isNotBlank() }}/$count)", style = MaterialTheme.typography.labelLarge) })
                Tab(selected = tab == 1, onClick = { tab = 1 },
                    text = { Text("$bowlingTeam (${bowNames.count { it.isNotBlank() }}/$count)", style = MaterialTheme.typography.labelLarge) })
            }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(16.dp))
                if (tab == 0) {
                    SquadCard(battingTeam, "Batting first — listed in batting order. Players 1 and 2 open.", batNames)
                } else {
                    SquadCard(bowlingTeam, "Fielding first. Any of them can bowl.", bowNames)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (prefilled) "Names filled in from your last match. Edit anything that changed."
                    else "Leave a name blank and it becomes “Player 1”, “Player 2”…",
                    style = MaterialTheme.typography.bodySmall, color = Ink300,
                )
                Spacer(Modifier.height(20.dp))
                S247Button(
                    text    = "Start 1st Innings",
                    onClick = { vm.startFirstInnings(batNames.toList(), bowNames.toList()); onDone() },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Lets a list of typed names be saved and restored (a plain state list can't be). */
private val nameListSaver = listSaver<SnapshotStateList<String>, String>(
    save    = { it.toList() },
    restore = { it.toMutableStateList() },
)

/**
 * The squad this team fielded in its most recent saved match, padded with blanks (or cut)
 * to [count] names. Default "Player N" names are left blank so they don't get pre-filled.
 */
private fun rosterFor(team: String, history: List<MatchRecord>, count: Int): List<String> {
    for (record in history) {
        val innings = listOfNotNull(record.state.innings1, record.state.innings2)
        val names = innings.firstNotNullOfOrNull { inn ->
            when (team) {
                inn.battingTeam -> inn.batters
                inn.bowlingTeam -> inn.bowlers
                else            -> null
            }
        }?.map { it.name } ?: continue
        return List(count) { i -> names.getOrNull(i)?.takeIf { CareerStats.isRealName(it) } ?: "" }
    }
    return List(count) { "" }
}

@Composable
private fun SquadCard(team: String, subtitle: String, names: MutableList<String>) {
    S247Card {
        Text(team, style = MaterialTheme.typography.headlineMedium, color = Ink)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ink300)
        Spacer(Modifier.height(14.dp))
        names.indices.forEach { i ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = Ink300, modifier = Modifier.width(24.dp))
                OutlinedTextField(
                    value           = names[i],
                    onValueChange   = { names[i] = it.take(Limits.NAME_LENGTH) },
                    placeholder     = { Text("Player ${i + 1}") },
                    modifier        = Modifier.weight(1f),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction      = if (i == names.lastIndex) ImeAction.Done else ImeAction.Next,
                    ),
                    shape  = ShapeInput,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Pitch, unfocusedBorderColor = Ink200),
                )
            }
        }
    }
}
