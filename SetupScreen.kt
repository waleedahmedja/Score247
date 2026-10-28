package com.waleedahmedja.score247.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.waleedahmedja.score247.data.model.Limits
import com.waleedahmedja.score247.data.model.MatchRules
import com.waleedahmedja.score247.ui.components.RuleRow
import com.waleedahmedja.score247.ui.components.S247Button
import com.waleedahmedja.score247.ui.components.S247Card
import com.waleedahmedja.score247.ui.components.S247Divider
import com.waleedahmedja.score247.ui.components.S247Screen
import com.waleedahmedja.score247.ui.components.Stepper
import com.waleedahmedja.score247.ui.components.ToastHost
import com.waleedahmedja.score247.ui.theme.Ink
import com.waleedahmedja.score247.ui.theme.Ink200
import com.waleedahmedja.score247.ui.theme.Ink300
import com.waleedahmedja.score247.ui.theme.Pitch
import com.waleedahmedja.score247.ui.theme.ShapeInput
import com.waleedahmedja.score247.viewmodel.MatchViewModel

/** Step 1 of a match: team names, format and the house rules. */
@Composable
fun SetupScreen(vm: MatchViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    // rememberSaveable → the form survives process death while the keyboard is open.
    // Start from the last finished match's names and rules (so a regular game needs no retyping);
    // a brand-new install falls back to the default rules.
    val last  = vm.history.value.firstOrNull()?.state
    val start = last?.rules ?: MatchRules()
    var teamA     by rememberSaveable { mutableStateOf(last?.teamA ?: "") }
    var teamB     by rememberSaveable { mutableStateOf(last?.teamB ?: "") }
    var overs     by rememberSaveable { mutableStateOf(start.totalOvers) }
    var players   by rememberSaveable { mutableStateOf(start.playersPerTeam) }
    var wideRun   by rememberSaveable { mutableStateOf(start.wideGivesRun) }
    var wideLegal by rememberSaveable { mutableStateOf(start.wideIsLegalBall) }
    var noBallRun by rememberSaveable { mutableStateOf(start.noBallGivesRun) }
    var freeHit   by rememberSaveable { mutableStateOf(start.freeHitOnNoBall) }
    var lastMan   by rememberSaveable { mutableStateOf(start.lastBatsmanAlone) }
    var triedToSubmit by rememberSaveable { mutableStateOf(false) }
    val teamAError = if (triedToSubmit && teamA.isBlank()) "Enter a team name" else null
    val teamBError = when {
        triedToSubmit && teamB.isBlank()                          -> "Enter a team name"
        triedToSubmit && teamA.trim().equals(teamB.trim(), true) -> "Team names must differ"
        else                                                      -> null
    }

    fun submit() {
        triedToSubmit = true
        val rules = MatchRules(
            totalOvers = overs, playersPerTeam = players,
            wideGivesRun = wideRun, wideIsLegalBall = wideLegal,
            noBallGivesRun = noBallRun, freeHitOnNoBall = freeHit,
            lastBatsmanAlone = lastMan,
        )
        // The ViewModel validates (names present and different) and shows a toast if not.
        if (vm.saveSetup(teamA, teamB, rules)) onDone()
    }

    S247Screen("Match Setup", onBack = onBack) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(16.dp))

                S247Card {
                    Text("Teams", style = MaterialTheme.typography.headlineMedium, color = Ink)
                    Spacer(Modifier.height(16.dp))
                    NameField("Team A", teamA, ImeAction.Next, error = teamAError) { teamA = it }
                    Spacer(Modifier.height(12.dp))
                    NameField("Team B", teamB, ImeAction.Done, error = teamBError, onDone = ::submit) { teamB = it }
                }

                Spacer(Modifier.height(12.dp))

                S247Card {
                    Text("Format", style = MaterialTheme.typography.headlineMedium, color = Ink)
                    Spacer(Modifier.height(14.dp))
                    Stepper("Overs", overs, 1, 50) { overs = it }
                    S247Divider(Modifier.padding(vertical = 12.dp))
                    Stepper("Players per side", players, 2, 11) { players = it }
                }

                Spacer(Modifier.height(12.dp))

                S247Card {
                    Text("Rules", style = MaterialTheme.typography.headlineMedium, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    Text("Match these to your local gully rules.", style = MaterialTheme.typography.bodySmall, color = Ink300)
                    Spacer(Modifier.height(8.dp))
                    S247Divider()
                    RuleRow("Wide adds a run", "The bowling side concedes 1 extra per wide", wideRun) { wideRun = it }
                    S247Divider()
                    RuleRow("Wide is a legal ball", "A wide uses up one ball of the over", wideLegal) { wideLegal = it }
                    S247Divider()
                    RuleRow("No-ball adds a run", "The bowling side concedes 1 extra per no-ball", noBallRun) { noBallRun = it }
                    S247Divider()
                    RuleRow("Free hit after a no-ball", "Only a run-out can dismiss the batter", freeHit) { freeHit = it }
                    S247Divider()
                    RuleRow("Last batsman bats alone", "Innings continues without a partner", lastMan) { lastMan = it }
                }

                Spacer(Modifier.height(20.dp))
                S247Button(text = "Continue to Toss", onClick = ::submit)
                Spacer(Modifier.height(24.dp))
            }

            ToastHost(
                messages = vm.toast,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
            )
        }
    }
}

@Composable
private fun NameField(
    label: String,
    value: String,
    ime: ImeAction,
    error: String? = null,
    onDone: () -> Unit = {},
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = { onChange(it.take(Limits.NAME_LENGTH)) },
        label           = { Text(label) },
        modifier        = Modifier.fillMaxWidth(),
        singleLine      = true,
        isError         = error != null,
        supportingText  = if (error != null) { { Text(error) } } else null,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ime),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape           = ShapeInput,
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Pitch,
            unfocusedBorderColor = Ink200,
            focusedLabelColor    = Pitch,
        ),
    )
}
